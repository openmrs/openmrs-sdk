package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @goal setup
 * @requiresProject false
 *
 */
public class Setup extends AbstractMojo {

    private static final String GOAL_UNPACK = "unpack";

    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     */
    private MavenSession mavenSession;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     * @required
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Interactive mode param
     *
     * @parameter expression="${interactiveMode}" default-value=false
     */
    private String interactiveMode;

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
     * Path to installation.properties
     *
     * @parameter expression="${file}"
     */
    private String file;

    /**
     * Option to include demo data
     *
     * @parameter expression="${addDemoData}" default-value=false
     */
    private boolean addDemoData;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    /**
     * wizard for resolving artifact available versions
     */
    private VersionsHelper versionsHelper;

    /**
     *
     */
    private ModuleInstaller moduleManager;

    /**
     * @parameter expression="${distro}"
     */
    private String distro;

    /**
     * @parameter expression="${platform}"
     */
    private String platform;

    /**
     * @required
     * @component
     */
    Wizard wizard;

    public Setup() {
    }

    public Setup(MavenProject mavenProject,
                 MavenSession mavenSession,
                 BuildPluginManager pluginManager) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
    }

    /**
     * Create and setup server with following parameters
     * @param server - server instance
     * @param isCreatePlatform - flag for platform setup
     * @param isCopyDependencies - flag for executing depencency plugin for OpenMRS 2.3 and higher
     * @throws MojoExecutionException
     */
    public String setup(Server server, boolean isCreatePlatform, boolean isCopyDependencies) throws MojoExecutionException, MojoFailureException {

        versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
        moduleManager = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);

        File openMRSPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        wizard.promptForNewServerIfMissing(server);

        File serverPath = new File(openMRSPath, server.getServerId());
        if (Server.hasServerConfig(serverPath)) {
            Server props = Server.loadServer(serverPath);
            if (props.getServerId() != null) {
                throw new MojoExecutionException("Server with same id already created");
            }
        }
        server.setServerDirectory(serverPath);

        if(isCreatePlatform){
            Artifact webapp = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_WEB);
            wizard.promptForPlatformVersionIfMissing(server, versionsHelper.getVersionAdvice(webapp, 6));
            moduleManager.installCoreModules(server, isCreatePlatform);
            wizard.promptForDbPlatform(server);
        } else {
            wizard.promptForDistroVersionIfMissing(server);
            if (new Version(server.getVersion()).higher(new Version(Version.PRIOR))&&isCopyDependencies) {
                extractDistroToServer(server.getVersion(), serverPath);
            }
            else {
                moduleManager.installCoreModules(server, isCreatePlatform);
            }
            wizard.promptForDbDistro(server);
        }

        if(server.getDbDriver() != null){
            server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
            if (server.getDbDriver().equals(SDKConstants.DRIVER_MYSQL)){
                if(!connectMySqlDatabase(server)){
                    wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
                }
            } else {
                moduleManager.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
                wizard.showMessage("The specified database "+server.getDbName()+" does not exist and it will be created for you.");
            }
        }

        server.setUnspecifiedToDefault();
        configureVersion(server, isCreatePlatform);
        server.save();

        return serverPath.getPath();
    }

    private void configureVersion(Server server, boolean isCreatePlatform){
        server.setPlatformVersion(server.getVersion());
        if (!isCreatePlatform) {
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

    /**
     * Download distro using dependency plugin to server dir
     * @param version
     * @param serverPath
     * @return web app version
     */
    public Version extractDistroToServer(String version, File serverPath) throws MojoExecutionException {
        File zipFolder = new File(serverPath, SDKConstants.TMP);
        List<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.add(SDKConstants.getReferenceModule(version));
        moduleManager.extractModules(artifacts, zipFolder.getPath());
        if (zipFolder.listFiles().length == 0) {
            throw new MojoExecutionException("Error during resolving dependencies");
        }
        File distro = zipFolder.listFiles()[0];
        File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        Version v = null;
        for (File f: distro.listFiles()) {
            int index = f.getName().lastIndexOf(".");
            String type = f.getName().substring(index + 1);
            File target;
            // for war or h2 file
            if ((type.equals(Artifact.TYPE_WAR)) || f.getName().startsWith("h2")) {
                target = new File(serverPath, f.getName());
                if (type.equals(Artifact.TYPE_WAR)) v = new Version(Version.parseVersionFromFile(f.getName()));
            }
            // for modules
            else {
                target = new File(modules, f.getName());
            }
            try {
                FileUtils.copyFile(f, target);
            } catch (IOException e) {
                throw new MojoExecutionException("Error during copying dependencies: " + e.getMessage());
            }
        }
        // clean
        try {
            FileUtils.deleteDirectory(zipFolder);
        } catch (IOException e) {
            throw new MojoExecutionException("Error during cleaning server directory");
        }
        return v;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        boolean createPlatform;
        String version = null;
        if(platform == null && distro == null){
            createPlatform = !wizard.promptYesNo("Would you like to install OpenMRS distribution?");
        } else if(platform != null){
            version = platform;
            createPlatform = true;
        } else {
            version = distro;
            createPlatform = false;
        }
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
                        .setVersion(version)
                        .setDbDriver(dbDriver)
                        .setDbUri(dbUri)
                        .setDbUser(dbUser)
                        .setDbPassword(dbPassword)
                        .setInteractiveMode(interactiveMode)
                        .build();
        // setup non-platform server
        String serverPath = setup(server, createPlatform, true);
        getLog().info("Server configured successfully, path: " + serverPath);
    }
}
