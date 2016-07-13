package org.openmrs.maven.plugins;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    private static final String GOAL_UNPACK = "unpack";
    private static final String DISTRIBUTION = "Distribution";
    private static final String PLATFORM = "Platform";
    public static final String SETTING_UP_A_NEW_SERVER = "Setting up a new server...";
    public static final String SETUP_SERVERS_PROMPT = "You can setup the following servers";
    private static final String CLASSPATH_SCRIPT_PREFIX = "classpath://";

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
        File openMRS = Server.getServersPathFile();
        wizard.promptForNewServerIfMissing(server);

        File serverPath = new File(openMRS, server.getServerId());
        if (Server.hasServerConfig(serverPath)) {
            Server props = Server.loadServer(serverPath);
            if (props.getServerId() != null) {
                throw new MojoExecutionException("Server with the same id already exists");
            }
        }
        server.setServerDirectory(serverPath);

        if (distroProperties == null) {
            if(isCreatePlatform){
                Artifact webapp = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_WEB);
                wizard.promptForPlatformVersionIfMissing(server, versionsHelper.getVersionAdvice(webapp, 6));
                moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties);
            } else {
                wizard.promptForRefAppVersionIfMissing(server, versionsHelper);
                distroProperties = extractDistroToServer(server, isCreatePlatform, serverPath);
                distroProperties.saveTo(server.getServerDirectory());
            }
        } else {
            moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties);
            distroProperties.saveTo(server.getServerDirectory());
        }
        
        if(dbDriver == null){
            if(isCreatePlatform){
                wizard.promptForH2Db(server);
            } else if(distroProperties!= null && distroProperties.isH2Supported()){
                wizard.promptForH2Db(server);
            }else {
                wizard.promptForMySQLDb(server);
            }
        } else if(dbDriver.equals("h2")){
            wizard.promptForH2Db(server);
        }else if(dbDriver.equals("mysql")){
            wizard.promptForMySQLDb(server);
        }

        if(server.getDbDriver() != null){
            if(server.getDbName() == null){
                server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
            }
            if (server.isMySqlDb()){
                boolean mysqlDbCreated = connectMySqlDatabase(server);
                if(mysqlDbCreated && !"null".equals(dbSql)){
                    if(dbSql != null){
                        importMysqlDb(server, dbSql);
                    } else if(distroProperties != null && distroProperties.getSqlScriptPath() != null){
                        importMysqlDb(server, distroProperties.getSqlScriptPath());
                    }
                    else if(isCreatePlatform) {
                        importMysqlDb(server, CLASSPATH_SCRIPT_PREFIX+ "openmrs-platform.sql");
                    }
                } else {
                    throw new IllegalStateException("Failed to connect to the specified database " + server.getDbUri());
                }
            } else {
                moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
                wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
            }
        }

        wizard.promptForJavaHomeIfMissing(server);

        server.setUnspecifiedToDefault();
        configureVersion(server, isCreatePlatform);
        server.save();

        return serverPath.getPath();
    }

    private DistroProperties extractDistroToServer(Server server, boolean isCreatePlatform, File serverPath) throws MojoExecutionException, MojoFailureException {
        DistroProperties distroProperties;
        if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
            distroProperties = new DistroProperties(server.getVersion());
        } else {
            distroProperties = distroHelper.downloadDistroProperties(serverPath, server);
        }
        moduleInstaller.installCoreModules(server, isCreatePlatform, distroProperties);
        return distroProperties;
    }

    private void configureVersion(Server server, boolean isCreatePlatform){
        if (isCreatePlatform){
            server.setPlatformVersion(server.getVersion());
            server.setParam(Server.PROPERTY_VERSION, "");
        } else {
            // set web app version for OpenMRS 2.2 and higher
            if (new Version(server.getVersion()).higher(new Version(Version.PRIOR))) {
                for (File f: server.getServerDirectory().listFiles()) {
                    if (f.getName().endsWith("." + Artifact.TYPE_WAR)) {
                        server.setPlatformVersion(Version.parseVersionFromFile(f.getName()));
                        break;
                    }
                }
            }
            else {
                server.setPlatformVersion(SDKConstants.WEBAPP_VERSIONS.get(server.getVersion()));
            }
        }
    }

    private boolean connectMySqlDatabase(Server server) throws MojoExecutionException {
	    String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/"));
        DBConnector connector = null;
        try {
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
        wizard.showMessage("Importing a database from " + sqlScriptPath + "...");
        String uri = server.getDbUri().replace("@DBNAME@", server.getDbName());

        File extractedSqlFile = null;
        InputStream sqlStream;
        if(sqlScriptPath.startsWith(CLASSPATH_SCRIPT_PREFIX)){
            String sqlScript = sqlScriptPath.replace(CLASSPATH_SCRIPT_PREFIX, "");
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
                .build();
        wizard.promptForNewServerIfMissing(server);

        boolean createPlatform = false;
        DistroProperties distroProperties = null;
        String version = null;
        if(platform == null && distro == null){
            List<String> options = new ArrayList<>();
            distroProperties = DistroHelper.getDistroPropertiesFromDir();

            if(distroProperties!=null){
                options.add(distroProperties.getName() + " " + distroProperties.getServerVersion() + " from current directory");
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
                    server.setPlatformVersion(distroProperties.getPlatformVersion());
                    server.setVersion(distroProperties.getServerVersion());
            }
        } else if (platform != null) {
            server.setVersion(platform);
            createPlatform = true;
        } else {
            distroProperties = distroHelper.retrieveDistroProperties(distro);
            server.setVersion(distroProperties.getServerVersion());
            server.setPlatformVersion(distroProperties.getPlatformVersion());
            createPlatform = false;
        }

        // setup non-platform server
        String serverPath = setup(server, createPlatform, true, distroProperties);
        getLog().info("Server configured successfully, path: " + serverPath);
    }
}
