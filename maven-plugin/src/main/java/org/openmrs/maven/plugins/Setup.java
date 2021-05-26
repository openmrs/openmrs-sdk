package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
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
import org.openmrs.maven.plugins.utility.ServerHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final int DEFAULT_PORT = 8080;

    /**
     * Server id (folder name)
     *
     * @parameter  property="serverId"
     */
    private String serverId;

    /**
     * DB Driver type
     *
     * @parameter  property="dbDriver"
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter  property="dbUri"
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter  property="dbUser"
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter  property="dbPassword"
     */
    private String dbPassword;

    /**
     * DB dump script to import
     *
     * @parameter  property="dbSql"
     */
    private String dbSql;

    /**
     * Docker host address
     *
     * @parameter  property="dockerHost"
     */
    private String dockerHost;

    /**
     * DB reset if exists
     * @parameter  property="dbReset"
     */
    private Boolean dbReset;

    /**
     * Path to JDK Version
     *
     * @parameter  property="javaHome"
     */
    private String javaHome;

    /**
     * Path to installation.properties
     *
     * @parameter  property="file"
     */
    private String file;

    /**
     * Option to include demo data
     *
     * @parameter  property="addDemoData" default-value="false"
     */
    private boolean addDemoData;

    /**
     * @parameter  property="distro"
     * OpenMRS Distribution to setup in a format 'groupId:artifactId:version'. You can skip groupId, if it is 'org.openmrs.distro'.
     */
    private String distro;

    /**
     * @parameter  property="platform"
     * OpenMRS Platform version to setup e.g. '1.11.5'.
     */
    private String platform;

    /**
     * @parameter  property="debug"
     */
    private String debug;

    /**
     * @parameter  property="run"
     */
    private boolean run;

    private ServerHelper serverHelper;

    public Setup() {
        super();
    }

    public Setup(AbstractTask other){
        super(other);
    }

    /**
     * Gets the distro properties. The distro properties can come from a file specified by path
     * or in the current directory or from a maven artifact.
     *
     * If no distro properties file can be found, `null` is returned. In this case, the distro
     * file will be created after the database is initialized. Setup should proceed to install
     * modules based on the OpenMRS WAR file for the given platform version.
     *
     * As of this writing, this function can return null only in platform mode.
     *
     * @param server An initialized Server instance
     * @return distro properties instantiated by DistroHelper
     * @throws MojoExecutionException
     */
    private DistroProperties resolveDistroProperties(Server server) throws MojoExecutionException {
        boolean platformMode;
        DistroProperties distroProperties = null;
        if (platform == null && distro == null){
            List<String> options = new ArrayList<>();
            distroProperties = DistroHelper.getDistroPropertiesFromDir();
            if (distroProperties != null) {
                options.add(distroProperties.getName() + " " + distroProperties.getVersion() + " from current directory");
            }
            options.add(DISTRIBUTION);
            options.add(PLATFORM);
            String choice = wizard.promptForMissingValueWithOptions(SETUP_SERVERS_PROMPT, null, null, options);

            switch(choice) {
                case PLATFORM:
                    platformMode = true;
                    break;
                case DISTRIBUTION:
                    wizard.promptForRefAppVersionIfMissing(server, versionsHelper);
                    if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
                        distroProperties = new DistroProperties(server.getVersion());
                    } else {
                        distroProperties = distroHelper.downloadDistroProperties(server.getServerDirectory(), server);
                    }
                    platformMode = false;
                    break;
                default:  // distro properties from current directory
                    server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerTmpDirectory()));
                    server.setVersion(distroProperties.getVersion());
                    platformMode = false;
            }
        } else if (platform != null) {
            server.setPlatformVersion(platform);
            platformMode = true;
        } else {  // getting distro properties from file
            distroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
            if (distroProperties == null) {
                throw new IllegalArgumentException("Distro "+distro+"could not be retrieved");
            }
            server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerTmpDirectory()));
            server.setVersion(distroProperties.getVersion());
            platformMode = false;
        }

        if (platformMode) {
            Artifact platformArtifact = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_DISTRO);
            String version = wizard.promptForPlatformVersionIfMissing(server.getPlatformVersion(), versionsHelper.getVersionAdvice(platformArtifact, 6));
            platformArtifact = DistroHelper.parseDistroArtifact(Artifact.GROUP_DISTRO + ":" + SDKConstants.PLATFORM_ARTIFACT_ID + ":" + version, versionsHelper);
            server.setPlatformVersion(platformArtifact.getVersion());
            try {
                distroProperties = distroHelper.downloadDistroProperties(server.getServerDirectory(), platformArtifact);
                // distroProperties could still be null at this point
            } catch (MojoExecutionException e) {
                distroProperties = null;
            }
        }

        return distroProperties;
    }

    /**
     * Sets up a server based on an initialized Server instance and the provided
     * distroProperties. Installs modules and other artifacts. Sets up the database.
     * Writes openmrs-server.properties.
     *
     * @param server An initialized server instance
     * @param distroProperties Allowed to be null, only if this is a platform install
     * @throws MojoExecutionException
     */
    public void setup(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        if (distroProperties != null) {
            // Add all the distro properties to the server properties.
            // It might be worth looking at redundancy between `distroHelper.savePropertiesToServer`,
            // `setServerVersionsFromDistroProperties`, and `server.setValuesFromDistroPropertiesModules`.
            distroHelper.savePropertiesToServer(distroProperties, server);
            setServerVersionsFromDistroProperties(server, distroProperties);
            moduleInstaller.installModulesForDistro(server, distroProperties, distroHelper);
            installOWAs(server, distroProperties);
        } else {
            moduleInstaller.installDefaultModules(server);
        }

        serverHelper = new ServerHelper(wizard);

        setServerPort(server);
        setDebugPort(server);

        setupDatabase(server, distroProperties);

        // If there's no distro at this point, we create a minimal one here,
        // *after* having initialized server.isH2Supported in `setupDatabase` above.
        if (distroProperties == null) {
            distroProperties = distroHelper.createDistroForPlatform(server);
        }
        distroProperties.saveTo(server.getServerDirectory());

        setJdk(server);

        server.setValuesFromDistroPropertiesModules(
                distroProperties.getWarArtifacts(distroHelper, server.getServerDirectory()),
                distroProperties.getModuleArtifacts(distroHelper, server.getServerDirectory()),
                distroProperties
        );
        server.setUnspecifiedToDefault();
        server.save();
    }

    private void setJdk(Server server) {
        String platformVersion = server.getPlatformVersion();
        Version version = new Version(platformVersion);
        if (platformVersion.startsWith("1.")) {
            wizard.showMessage("Note: JDK 1.7 is needed for platform version " + platformVersion + ".");
        }
        else if (version.getMajorVersion() == 2 && version.getMinorVersion() < 4) {
            wizard.showMessage("Note: JDK 1.8 is needed for platform version " + platformVersion + ".");
        }
        else {
            wizard.showMessage("Note: JDK 1.8 or above is needed for platform version " + platformVersion + ".");
        }

        wizard.promptForJavaHomeIfMissing(server);
    }
    
    private void installOWAs(Server server, DistroProperties distroProperties) throws MojoExecutionException {
    	if (distroProperties != null) {
    		File owasDir = new File(server.getServerDirectory(), "owa");
    		owasDir.mkdir();
    		downloadOWAs(server.getServerDirectory(), distroProperties, owasDir);
    	}
    }
    
    private void downloadOWAs(File targetDirectory, DistroProperties distroProperties, File owasDir) throws MojoExecutionException {
        List<Artifact> owas = distroProperties.getOwaArtifacts(distroHelper, targetDirectory);
        if (!owas.isEmpty()) {
            wizard.showMessage("Downloading OWAs...\n");
            for (Artifact owa: owas) {
                wizard.showMessage("Downloading OWA: " + owa);
                owaHelper.downloadOwa(owasDir, owa, moduleInstaller);
            }
        }
    }

    private void wipeDatabase(Server server) throws MojoExecutionException {
        String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1);
        DBConnector connector = null;
        try {
            connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
            connector.dropDatabase();
            if (server.isMySqlDb()) {
            	connectMySqlDatabase(server);
            }
            else if (server.isPostgreSqlDb()) {
            	connectPostgreSqlDatabase(server);
            }
            wizard.showMessage("Database " + server.getDbName() + " has been wiped.");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to drop " + server.getDbName() + " database");
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

    private void setServerPort(Server server) {
        String message = "What port would you like your server to use?";
        String port = wizard.promptForValueIfMissingWithDefault(
                message,
                server.getParam("tomcat.port"),
                "port number",
                String.valueOf(Setup.DEFAULT_PORT));
        if (!StringUtils.isNumeric(port) || !this.serverHelper.isPort(Integer.parseInt(port))) {
            wizard.showMessage("Port must be numeric and less or equal 65535.");
            this.setServerPort(server);
            return;
        }
        server.setPort(port);
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

    private void setServerVersionsFromDistroProperties(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        if(server.getPlatformVersion() == null){
            server.setPlatformVersion(distroProperties.getPlatformVersion(distroHelper, server.getServerDirectory()));
        }
        if(server.getVersion() == null){
            server.setVersion(distroProperties.getVersion());
        }
    }

    private void setupDatabase(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        if (server.getDbDriver() == null) {
            boolean isH2Supported = true;
            if (distroProperties != null) {
                isH2Supported = distroProperties.isH2Supported();
            }
            wizard.promptForDb(server, dockerHelper, isH2Supported, dbDriver, dockerHost);
        }
        if (server.getDbDriver() != null) {
            setupDatabaseForServer(server);
        }
    }

    private void setupDatabaseForServer(Server server) throws MojoExecutionException {
        if(server.getDbName() == null){
            server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
        }
        if (server.isMySqlDb()){
            boolean mysqlDbCreated = connectMySqlDatabase(server);
            if(!mysqlDbCreated){
                throw new IllegalStateException("Failed to connect to the specified database " + server.getDbUri());
            }

            if (hasDbTables(server)) {
                if (dbReset == null) {
                    dbReset = !wizard.promptYesNo("Would you like to setup the server using existing data (if not, all data will be lost)?");
                }

                if (dbReset) {
                    wipeDatabase(server);
                } else {
                    server.setParam("create_tables", "false");
                }
            }

            if (dbReset == null){
                dbReset = true;
            }


            if(!"null".equals(dbSql) && dbReset){
                if(dbSql != null){
                    importMysqlDb(server, dbSql);
                    resetSearchIndex(server);
                } else {
                    if (server.getPlatformVersion() != null) {
                        if (new Version(server.getPlatformVersion()).higher(new Version("1.9.7"))){
                            importMysqlDb(server, Server.CLASSPATH_SCRIPT_PREFIX+ "openmrs-platform.sql");
                            resetSearchIndex(server);
                        }
                    }
                }
            } else if (!dbReset) {
                resetSearchIndex(server);
            }
        } else if (server.isPostgreSqlDb()){
            boolean postgresqlDbCreated = connectPostgreSqlDatabase(server);
            if(!postgresqlDbCreated){
                throw new IllegalStateException("Failed to connect to the specified database " + server.getDbUri());
            }

            if (hasDbTables(server)) {
                if (dbReset == null) {
                    dbReset = !wizard.promptYesNo("Would you like to setup the server using existing data (if not, all data will be lost)?");
                }

                if (dbReset) {
                    wipeDatabase(server);
                } else {
                    server.setParam("create_tables", "false");
                }
            }

            if (dbReset == null){
                dbReset = true;
            }


            if(!"null".equals(dbSql) && dbReset){
                if(dbSql != null){
                    importPostgreSqlDb(server, dbSql);
                    resetSearchIndex(server);
                } else {
                    if (server.getPlatformVersion() != null) {
                        if (new Version(server.getPlatformVersion()).higher(new Version("1.9.7"))){
                            importPostgreSqlDb(server, Server.CLASSPATH_SCRIPT_PREFIX+ "openmrs-platform-postgres.sql");
                            resetSearchIndex(server);
                        }
                    }
                }
            } else if (!dbReset) {
                resetSearchIndex(server);
            }

        } else {
            moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
            wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
        }
    }

    private boolean connectMySqlDatabase(Server server) throws MojoExecutionException {
	    String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1);
        DBConnector connector = null;
        try {
            try {
                //ensure driver is registered
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load MySQL driver");
            }
            connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
            connector.checkAndCreate(server);
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
    
    private boolean connectPostgreSqlDatabase(Server server) throws MojoExecutionException {
	    String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1);
        DBConnector connector = null;
        try {
            try {
                //ensure driver is registered
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load PostgreSQL driver", e);
            }
            connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
            connector.checkAndCreate(server);
            connector.close();
            wizard.showMessage("Connected to the database.");
            return true;
        } catch (Exception e) {
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

    private boolean hasDbTables(Server server) throws MojoExecutionException {
        String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1);
        DBConnector connector = null;
        ResultSet rs = null;
        try {
            connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
            DatabaseMetaData md = connector.getConnection().getMetaData();

            rs = md.getTables(server.getDbName(), null, null,new String[] {"TABLE"});
            boolean hasTables = false;
            if (rs.next()) {
                hasTables = true;
            }

            rs.close();
            connector.close();

            return hasTables;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch table list from \"" + server.getDbName() + "\" database.", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    getLog().error(e.getMessage());
                }
            }
            if (connector != null){
                try {
                    connector.close();
                } catch (SQLException e) {
                    getLog().error(e.getMessage());
                }
            }
        }
    }

    private void resetSearchIndex(Server server) throws MojoExecutionException {
        String uri = server.getDbUri();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1);
        DBConnector connector = null;
        PreparedStatement ps = null;
        try {
            connector = new DBConnector(uri + server.getDbName(), server.getDbUser(), server.getDbPassword(), server.getDbName());
            ps = connector.getConnection().prepareStatement(String.format(SDKConstants.RESET_SEARCH_INDEX_SQL, server.getDbName()));
            ps.execute();

            ps.close();
            connector.close();

            wizard.showMessage("The search index has been reset.");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset search index", e);
        } finally {
            if (ps != null){
                try {
                    ps.close();
                } catch (SQLException e) {
                    getLog().error(e.getMessage());
                }
            }
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
        Reader sqlReader;
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

        sqlReader = new InputStreamReader(sqlStream);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(uri, server.getDbUser(), server.getDbPassword());
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            //we don't want to display ~5000 lines of queries to user if there is no error
            scriptRunner.setLogWriter(new PrintWriter(new NullOutputStream()));
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(sqlReader);
            scriptRunner.closeConnection();
            sqlReader.close();
            wizard.showMessage("Database imported successfully.");
            server.setParam("create_tables", "false");
        } catch (Exception e) {
            getLog().error(e.getMessage());

            throw new MojoExecutionException("Failed to import database", e);
        } finally {
            IOUtils.closeQuietly(sqlReader);

            try {
                if(connection != null){
                    connection.close();
                }
            } catch (SQLException e1) {
                //close quietly
            }

            //this file is extracted from distribution, clean it up
            if(extractedSqlFile != null && extractedSqlFile.exists()){
                extractedSqlFile.delete();
            }
        }
    }
    
    private void importPostgreSqlDb(Server server, String sqlScriptPath) throws MojoExecutionException {
        wizard.showMessage("Importing an initial database from " + sqlScriptPath + "...");
        String uri = server.getDbUri().replace("@DBNAME@", server.getDbName());

        File extractedSqlFile = null;
        InputStream sqlStream;
        Reader sqlReader;
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

        sqlReader = new InputStreamReader(sqlStream);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(uri, server.getDbUser(), server.getDbPassword());
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            //we don't want to display ~5000 lines of queries to user if there is no error
            scriptRunner.setLogWriter(new PrintWriter(new NullOutputStream()));
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(sqlReader);
            scriptRunner.closeConnection();
            sqlReader.close();
            wizard.showMessage("Database imported successfully.");
            server.setParam("create_tables", "false");
        } catch (Exception e) {
            getLog().error(e.getMessage());

            throw new MojoExecutionException("Failed to import database", e);
        } finally {
            IOUtils.closeQuietly(sqlReader);

            try {
                if(connection != null){
                    connection.close();
                }
            } catch (SQLException e1) {
                //close quietly
            }

            //this file is extracted from distribution, clean it up
            if(extractedSqlFile != null && extractedSqlFile.exists()){
                extractedSqlFile.delete();
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

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        wizard.showMessage(SETTING_UP_A_NEW_SERVER);

        Server.ServerBuilder serverBuilder;
        if(file != null){
            File serverPath = new File(file);
            serverBuilder = new Server.ServerBuilder(Server.loadServer(serverPath));
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
        if (serverDir.isDirectory()) {
            throw new MojoExecutionException("Cannot create server: directory with name "+serverDir.getName()+" already exists");
        }
        server.setServerDirectory(serverDir);
        serverDir.mkdir();

        try {
            DistroProperties distroProperties = resolveDistroProperties(server);
            setup(server, distroProperties);
        } catch (Exception e) {
            FileUtils.deleteQuietly(server.getServerDirectory());
            throw new MojoExecutionException("Failed to setup server", e);
        }

        getLog().info("Server configured successfully, path: " + serverDir);

        if (run) {
            new Run(this, server.getServerId()).execute();
        }
    }
}
