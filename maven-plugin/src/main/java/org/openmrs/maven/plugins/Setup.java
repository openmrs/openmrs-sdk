package org.openmrs.maven.plugins;

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

    /**
     * Server id (folder name)
     *
     * @parameter property="serverId"
     */
    private String serverId;

    /**
     * DB Driver type
     *
     * @parameter property="dbDriver"
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter property="dbUri"
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter property="dbUser"
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter property="dbPassword"
     */
    private String dbPassword;

    /**
     * Path to installation.properties
     *
     * @parameter property="file"
     */
    private String file;

    /**
     * Option to include demo data
     *
     * @parameter property="addDemoData" default-value="false"
     */
    private boolean addDemoData;

    /**
     * @parameter property="distro"
     */
    private String distro;

    /**
     * @parameter property="platform"
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
                wizard.promptForDistroVersionIfMissing(server);
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
        } if(dbDriver.equals("h2")){
            wizard.promptForH2Db(server);
        }else if(dbDriver.equals("mysql")){
            wizard.promptForMySQLDb(server);
        }

        if(server.getDbDriver() != null){
            if(server.getDbName() == null){
                server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
            }
            if (server.getDbDriver().equals(SDKConstants.DRIVER_MYSQL)){
                if(!connectMySqlDatabase(server)){
                    wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
                }
            } else {
                moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
                wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
            }
        }

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
            getLog().info("Database configured successfully");
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
                .setInteractiveMode(interactiveMode)
                .build();
        wizard.promptForNewServerIfMissing(server);

        boolean createPlatform = false;
        DistroProperties distroProperties = null;
        String version = null;
        if(platform == null && distro == null){
            List<String> options = new ArrayList<>();
            distroProperties = DistroHelper.getDistroPropertiesFromDir();

            if(distroProperties!=null){
                options.add(distroProperties.getName() + distroProperties.getServerVersion() + " distribution from current directory");
            }
            options.add(DISTRIBUTION);
            options.add(PLATFORM);

            String choice = wizard.promptForMissingValueWithOptions(SETUP_SERVERS_PROMPT, null, null, options, null, null);

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
                    version = distroProperties.getServerVersion();
            }
        } else if (platform != null) {
            version = platform;
            createPlatform = true;
        } else {
            distroProperties = distroHelper.retrieveDistroProperties(distro);
            version = distroProperties.getServerVersion();
            createPlatform = false;
        }
        server.setVersion(version);

        // setup non-platform server
        String serverPath = setup(server, createPlatform, true, distroProperties);
        getLog().info("Server configured successfully, path: " + serverPath);
    }
}
