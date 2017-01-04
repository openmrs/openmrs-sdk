package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal setup
 * @requiresProject false
 */
public class Setup extends AbstractTask {

    private static final String DISTRIBUTION = "Distribution";
    private static final String PLATFORM = "Platform";
    public static final String SETTING_UP_A_NEW_SERVER = "Setting up a new server...";
    public static final String SETUP_SERVERS_PROMPT = "You can setup the following servers";
    private static final String CLASSPATH_SCRIPT_PREFIX = "classpath://";
    public static final String ENABLE_DEBUGGING_DEFAULT_MESSAGE = "If you want to enable remote debugging by default when running the server, " +
            "\nspecify the %s here (e.g. 1044). Leave blank to disable debugging. \n(Do not do this on a production server)";
    private static final String NO_DEBUGGING_DEFAULT_ANSWER = "no debugging";


    /**
     * Server id (folder name)
     *
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * DB Driver type
     *
     * @parameter expression="${dbDriver}"
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter expression="${dbUri}"
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter expression="${dbUser}"
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter expression="${dbPassword}"
     */
    private String dbPassword;

    /**
     * DB dump script to import
     *
     * @parameter expression="${dbSql}"
     */
    private String dbSql;

    /**
     * Path to JDK Version
     *
     * @parameter expression="${javaHome}"
     */
    private String javaHome;

    /**
     * Path to installation.properties
     *
     * @parameter expression="${file}"
     */
    private String file;

    /**
     * Option to include demo data
     *
     * @parameter expression="${addDemoData}" default-value="false"
     */
    private boolean addDemoData;

    /**
     * @parameter expression="${distro}"
     */
    private String distro;

    /**
     * @parameter expression="${platform}"
     */
    private String platform;

    /**
     * @parameter expression="${debug}"
     */
    private String debug;

    /**
     * @parameter expression="${run}"
     */
    private boolean run;

    public Setup() {
        super();
    }

    public Setup(AbstractTask other){
        super(other);
    }

    /**
     * Create and setup server with following parameters
     * @param server - server instance
     * @param isCreatePlatform - flag for platform setup
     * @param isCopyDependencies - flag for executing depencency plugin for OpenMRS 2.3 and higher
     * @throws MojoExecutionException
     */
    public String setup(Server server, boolean isCreatePlatform, boolean isCopyDependencies, DistroProperties distroProperties) throws MojoExecutionException, MojoFailureException {
        wizard.promptForNewServerIfMissing(server);
        File serverPath = new File(Server.getServersPathFile(), server.getServerId());
        server.setServerDirectory(serverPath);


        try {
            if (distroProperties == null) {
                if(isCreatePlatform){
                    Artifact platform = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_DISTRO);
                    wizard.promptForPlatformVersionIfMissing(server, versionsHelper.getVersionAdvice(platform, 6));
                    platform.setVersion(server.getPlatformVersion());
                    try {
                        distroProperties = distroHelper.downloadDistroProperties(serverPath, platform);
                        distroProperties.saveTo(server.getServerDirectory());
                        moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties, distroHelper);
                    } catch (MojoExecutionException e) {
                        getLog().info("Fetching openmrs war file in version " + server.getPlatformVersion());
                        moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties, distroHelper);
                    }
                } else {
                    wizard.promptForRefAppVersionIfMissing(server, versionsHelper);
                    distroProperties = extractDistroToServer(server, isCreatePlatform, serverPath);
                    distroProperties.saveTo(server.getServerDirectory());
                }
            } else {
                moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties, distroHelper);
                distroProperties.saveTo(server.getServerDirectory());
            }
            distroHelper.savePropertiesToServer(distroProperties, server);
            setDebugPort(server);

            if(server.getDbDriver() == null) {
                boolean h2supported = true;
                if(distroProperties != null) {
                    h2supported = distroProperties.isH2Supported();
                }
                wizard.promptForDb(server, dockerHelper, h2supported, dbDriver);
            }

            if(server.getDbDriver() != null){
                if(server.getDbName() == null){
                    server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
                }
                if (server.isMySqlDb()){
                    boolean mysqlDbCreated = connectMySqlDatabase(server);
                    if(!mysqlDbCreated){
                        throw new IllegalStateException("Failed to connect to the specified database " + server.getDbUri());
                    }
                    boolean mysqlImportEnabled = !"null".equals(dbSql);
                    if(mysqlImportEnabled){
                        if(dbSql != null){
                            importMysqlDb(server, dbSql);
                        } else {
                            importMysqlDb(server, Server.CLASSPATH_SCRIPT_PREFIX+ "openmrs-platform.sql");
                        }
                    }
                } else {
                    moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
                    wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
                }
            }

            configureVersion(server, distroProperties);
            distroProperties = createDistroForPlatform(distroProperties, server);

            String platformVersion = server.getPlatformVersion();
            if (platformVersion.startsWith("1.")) {
                wizard.showMessage("Note: JDK 1.7 is needed for platform version " + platformVersion + ".");
            }
            else {
                wizard.showMessage("Note: JDK 1.8 is needed for platform version " + platformVersion + ".");
            }

            wizard.promptForJavaHomeIfMissing(server);
            server.setValuesFromDistroPropertiesModules(
                    distroProperties.getWarArtifacts(distroHelper, server.getServerDirectory()),
                    distroProperties.getModuleArtifacts(distroHelper, server.getServerDirectory()),
                    distroProperties
            );
            server.setUnspecifiedToDefault();
            server.save();

            return serverPath.getPath();
        } catch (Exception e) {
            FileUtils.deleteQuietly(serverPath);
            throw new MojoExecutionException("Failed to setup server", e);
        }
    }

    private DistroProperties createDistroForPlatform(DistroProperties distroProperties, Server server) throws MojoExecutionException {
        if (distroProperties == null) {
            distroProperties = new DistroProperties(server.getServerId(), server.getPlatformVersion());
            if (server.getDbDriver().equals(SDKConstants.DRIVER_H2)) {
                distroProperties.setH2Support(true);
            }
            distroProperties.saveTo(server.getServerDirectory());
        }
        return distroProperties;
    }

    private void setDebugPort(Server server) {
        if (StringUtils.isBlank(debug) || wizard.checkYes(debug)) {
            while (!NO_DEBUGGING_DEFAULT_ANSWER.equals(debug) && !StringUtils.isNumeric(debug)) {
                debug = wizard.promptForValueIfMissingWithDefault(
                        ENABLE_DEBUGGING_DEFAULT_MESSAGE,
                        server.getDebugPort(),
                        "port number",
                        NO_DEBUGGING_DEFAULT_ANSWER);
                if(!StringUtils.isNumeric(debug) && !NO_DEBUGGING_DEFAULT_ANSWER.equals(debug)){
                    wizard.showMessage("\nPort number must be numeric.");
                } else if(!NO_DEBUGGING_DEFAULT_ANSWER.equals(debug)){
                    server.setDebugPort(debug);
                }
            }
        }
    }

    private DistroProperties extractDistroToServer(Server server, boolean isCreatePlatform, File serverPath) throws MojoExecutionException, MojoFailureException {
        DistroProperties distroProperties;
        if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
            distroProperties = new DistroProperties(server.getVersion());
        } else {
            distroProperties = distroHelper.downloadDistroProperties(serverPath, server);
        }
        moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties, distroHelper);
        return distroProperties;
    }

    private void configureVersion(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        if (distroProperties != null) {
            if(server.getPlatformVersion() == null){
                server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerDirectory()));
            }
            if(server.getVersion() == null){
                server.setVersion(distroProperties.getVersion());
            }
        }
    }

    private boolean connectMySqlDatabase(Server server) throws MojoExecutionException {
	    String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/"));
        DBConnector connector = null;
        try {
            try {
                //ensure driver is registered
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load MySQL driver");
            }
            connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
            connector.checkAndCreate();
            connector.close();
            wizard.showMessage("Connected to the database.");
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            if (connector != null){
                try {
                    connector.close();
                } catch (SQLException e) {
                    getLog().error(e.getMessage());
                }
            }
        }
    }

    private void importMysqlDb(Server server, String sqlScriptPath) throws MojoExecutionException {
        wizard.showMessage("Importing an initial database from " + sqlScriptPath + "...");
        String uri = server.getDbUri().replace("@DBNAME@", server.getDbName());

        File extractedSqlFile = null;
        InputStream sqlStream;
        if(sqlScriptPath.startsWith(Server.CLASSPATH_SCRIPT_PREFIX)){
            String sqlScript = sqlScriptPath.replace(Server.CLASSPATH_SCRIPT_PREFIX, "");
            sqlStream = (Setup.class.getClassLoader().getResourceAsStream(sqlScript));
            if(sqlStream == null){
                Artifact distroArtifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
                extractedSqlFile =  distroHelper.extractFileFromDistro(server.getServerDirectory(), distroArtifact, sqlScript);
                try {
                    sqlStream = new FileInputStream(extractedSqlFile);
                } catch (FileNotFoundException e) {
                    throw new MojoExecutionException("Error during opening sql dump script file", e);
                }
            }
        } else {
            File scriptFile = new File(sqlScriptPath);
            try {
                sqlStream = new FileInputStream(scriptFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Invalid path to SQL import script", e);
            }
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(uri, server.getDbUser(), server.getDbPassword());
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            //we don't want to display ~5000 lines of queries to user if there is no error
            scriptRunner.setLogWriter(new PrintWriter(new NullOutputStream()));
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(new InputStreamReader(sqlStream));
            scriptRunner.closeConnection();
            wizard.showMessage("Database imported successfully.");
            server.setParam("create_tables", "false");
        } catch (SQLException e) {
            //this file is extracted from distribution, clean it up
            if(extractedSqlFile != null && extractedSqlFile.exists()){
                extractedSqlFile.delete();
            }
            try {
                if(connection!=null){
                    connection.close();
                }
            } catch (SQLException e1) {
                getLog().error(e.getMessage());
            }
        }
    }

    public String determineDbName(String uri, String serverId) throws MojoExecutionException {
        String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverId);
        if (!uri.contains("@DBNAME@")) {
            //determine db name from uri
            int dbNameStart = uri.lastIndexOf("/");
            if (dbNameStart < 0) {
                throw new MojoExecutionException("The uri is in a wrong format: " + uri);
            }
            int dbNameEnd = uri.indexOf("?");
            dbName = dbNameEnd < 0 ? uri.substring(dbNameStart + 1) : uri.substring(dbNameStart + 1, dbNameEnd);

            if (!dbName.matches("^[A-Za-z0-9_\\-]+$")) {
                throw new MojoExecutionException("The db name is in a wrong format (allowed alphanumeric, dash and underscore signs): " + dbName);
            }
        }
        return dbName;
    }

    private boolean checkIfDistroIsPath(String distro){
        File file = new File(distro);
        if(file.exists()){
            return true;
        }else {
            return false;
        }
    }


    public void executeTask() throws MojoExecutionException, MojoFailureException {
        wizard.showMessage(SETTING_UP_A_NEW_SERVER);

        Server.ServerBuilder serverBuilder;
        if(file != null){
            File serverPath = new File(file);
            Server server = Server.loadServer(serverPath);
            serverBuilder = new Server.ServerBuilder(server);
        } else {
            serverBuilder = new Server.ServerBuilder();
        }
        Server server = serverBuilder
                .setServerId(serverId)
                .setDbDriver(dbDriver)
                .setDbUri(dbUri)
                .setDbUser(dbUser)
                .setDbPassword(dbPassword)
                .setInteractiveMode(testMode)
                .setJavaHome(javaHome)
                .setDebugPort(debug)
                .build();
        wizard.promptForNewServerIfMissing(server);

        File serverDir = new File(Server.getServersPath(), server.getServerId());
        if (serverDir.isDirectory() && serverDir.exists()) {
            throw new MojoExecutionException("Cannot create server: directory with name "+serverDir.getName()+" already exists");
        }

        boolean createPlatform = false;
        DistroProperties distroProperties = null;
        if(platform == null && distro == null){
            List<String> options = new ArrayList<>();
            distroProperties = DistroHelper.getDistroPropertiesFromDir();

            if(distroProperties!=null){
                options.add(distroProperties.getName() + " " + distroProperties.getVersion() + " from current directory");
            }
            options.add(DISTRIBUTION);
            options.add(PLATFORM);

            String choice = wizard.promptForMissingValueWithOptions(SETUP_SERVERS_PROMPT, null, null, options);

            switch(choice) {
                case PLATFORM:
                    createPlatform = true;
                    distroProperties = null;
                    break;
                case DISTRIBUTION:
                    createPlatform = false;
                    distroProperties = null;
                    break;
                default:
                    createPlatform = false;
                    server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerTmpDirectory()));
                    server.setVersion(distroProperties.getVersion());
            }
        } else if (platform != null) {
            server.setPlatformVersion(platform);
            createPlatform = true;
        } else {
            distroProperties = distroHelper.retrieveDistroProperties(distro, versionsHelper);
            if(distroProperties == null){
                throw new IllegalArgumentException("Distro "+distro+"could not be retrieved");
            }
            server.setVersion(distroProperties.getVersion());
            server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerTmpDirectory()));
            createPlatform = false;
        }

        // setup non-platform server
        String serverPath = setup(server, createPlatform, true, distroProperties);
        getLog().info("Server configured successfully, path: " + serverPath);

        if(run){
            new Run(this, server.getServerId()).execute();
        }
    }
}
