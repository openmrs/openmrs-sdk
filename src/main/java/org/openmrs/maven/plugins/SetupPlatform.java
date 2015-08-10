package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal setup-platform
 * @requiresProject false
 *
 */
public class SetupPlatform extends AbstractMojo {

    private static final String DEFAULT_VERSION = "2.2";
    private static final String DEFAULT_PLATFORM_VERSION = "1.11.2";

    /**
     * Default constructor
     */
    public SetupPlatform() {};

    /**
     * Constructor to use this class in other mojos
     * @param mavenProject - outer project
     * @param mavenSession - outer session
     * @param prompter - outer prompter
     * @param pluginManager - outer pluginManager
     */
    public SetupPlatform(MavenProject mavenProject,
                         MavenSession mavenSession,
                         Prompter prompter,
                         BuildPluginManager pluginManager) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.prompter = prompter;
        this.pluginManager = pluginManager;
    }

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
     * Platform version
     *
     * @parameter expression="${version}"
     */
    private String version;

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
     * Component for user prompt
     *
     * @component
     */
    private Prompter prompter;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    /**
     * Create and setup server with following parameters
     * @param server - server instance
     * @param isCreatePlatform - flag for platform setup
     * @param isCopyDependencies - flag for executing depencency plugin for OpenMRS 2.3 and higher
     * @throws MojoExecutionException
     */
    public String setup(Server server, boolean isCreatePlatform, boolean isCopyDependencies) throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File openMRSPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        PropertyManager basicProperties = null;
        if (server.getFilePath() != null) {
            File basicFileProperties = new File(server.getFilePath());
            if (!basicFileProperties.exists()) throw new MojoExecutionException("Selected server file is not exist");
            basicProperties = new PropertyManager(basicFileProperties.getPath());
            // fill empty properties with values from server file
            if (server.getServerId() == null) server.setServerId(basicProperties.getParam(SDKConstants.PROPERTY_SERVER_ID));
            if (server.getVersion() == null) server.setVersion(basicProperties.getParam(SDKConstants.PROPERTY_PLATFORM));
            if (server.getDbDriver() == null) server.setDbDriver(basicProperties.getParam(SDKConstants.PROPERTY_DB_DRIVER));
            if (server.getDbUri() == null) server.setDbUri(basicProperties.getParam(SDKConstants.PROPERTY_DB_URI));
            if (server.getDbUser() == null) server.setDbUser(basicProperties.getParam(SDKConstants.PROPERTY_DB_USER));
            if (server.getDbPassword() == null) server.setDbPassword(basicProperties.getParam(SDKConstants.PROPERTY_DB_PASS));
        }
        try {
            server.setServerId(helper.promptForNewServerIfMissing(openMRSPath.getPath(), server.getServerId()));
        } catch (PrompterException e) {
            getLog().error(e.getMessage());
        }
        File serverPath = new File(openMRSPath, server.getServerId());
        File propertyPath = new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        if (propertyPath.exists()) {
            PropertyManager props = new PropertyManager(propertyPath.getPath());
            if (props.getParam(SDKConstants.PROPERTY_SERVER_ID) != null) {
                throw new MojoExecutionException("Server with same id already created");
            }
        }
        try {
            String defaultV = isCreatePlatform ? DEFAULT_PLATFORM_VERSION : DEFAULT_VERSION;
            server.setVersion(helper.promptForValueIfMissingWithDefault(server.getVersion(), "version", defaultV));
        } catch (PrompterException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        if (new Version(server.getVersion()).higher(new Version(Version.PRIOR)) && !isCreatePlatform) {
            if (isCopyDependencies) extractDistroToServer(server.getVersion(), serverPath);
        }
        else {
            // install core modules
            List<Artifact> coreModules = SDKConstants.getCoreModules(server.getVersion(), isCreatePlatform);
            if (coreModules == null) {
                throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getVersion()));
            }
            installModules(coreModules, serverPath.getPath());
            // install other modules
            if (!isCreatePlatform) {
                File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
                modules.mkdirs();
                List<Artifact> artifacts = SDKConstants.ARTIFACTS.get(server.getVersion());
                // install modules for each version
                installModules(artifacts, modules.getPath());
            }
            // install user modules
            if (basicProperties != null) {
                String values = basicProperties.getParam(SDKConstants.PROPERTY_USER_MODULES);
                if (values != null) {
                    ModuleInstall installer = new ModuleInstall();
                    String[] modules = values.split(PropertyManager.COMMA);
                    for (String mod: modules) {
                        String[] params = mod.split(PropertyManager.SLASH);
                        // check
                        if (params.length == 3) {
                            installer.installModule(server.getServerId(), params[1], params[0] + "-omod", params[2]);
                        }
                        else throw new MojoExecutionException("Properties file parse error - cannot read user modules list");
                    }
                }
            }
        }
        File propertiesFile = new File(serverPath.getPath(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        PropertyManager properties = new PropertyManager(propertiesFile.getPath());
        properties.setDefaults();
        properties.setParam(SDKConstants.PROPERTY_SERVER_ID, server.getServerId());
        // configure db properties
        if ((server.getDbDriver() != null) ||
                (server.getDbUser() != null) ||
                (server.getDbPassword() != null) ||
                (server.getDbUri() != null) ||
                !isCreatePlatform) {
            try {
                server.setDbDriver(helper.promptForValueIfMissingWithDefault(server.getDbDriver(), "dbDriver", "mysql"));
                String defaultUri = SDKConstants.URI_MYSQL;
                if ((server.getDbDriver().equals("postgresql")) || (server.getDbDriver().equals(SDKConstants.DRIVER_POSTGRESQL))) {
                    properties.setParam(SDKConstants.PROPERTY_DB_DRIVER, SDKConstants.DRIVER_POSTGRESQL);
                    defaultUri = SDKConstants.URI_POSTGRESQL;
                }
                else if ((server.getDbDriver().equals("h2")) || (server.getDbDriver().equals(SDKConstants.DRIVER_H2))) {
                    properties.setParam(SDKConstants.PROPERTY_DB_DRIVER, SDKConstants.DRIVER_H2);
                    defaultUri = SDKConstants.URI_H2;
                }
                else if (server.getDbDriver().equals("mysql")) {
                    properties.setParam(SDKConstants.PROPERTY_DB_DRIVER, SDKConstants.DRIVER_MYSQL);
                }
                else properties.setParam(SDKConstants.PROPERTY_DB_DRIVER, server.getDbDriver());

                server.setDbUri(helper.promptForValueIfMissingWithDefault(server.getDbUri(), "dbUri", defaultUri));
                String defaultUser = "root";
                server.setDbUser(helper.promptForValueIfMissingWithDefault(server.getDbUser(), "dbUser", defaultUser));
                server.setDbPassword(helper.promptForValueIfMissing(server.getDbPassword(), "dbPassword"));
                properties.setParam(SDKConstants.PROPERTY_DB_USER, server.getDbUser());
                properties.setParam(SDKConstants.PROPERTY_DB_PASS, server.getDbPassword());
                properties.setParam(SDKConstants.PROPERTY_DB_URI, server.getDbUri());
            } catch (PrompterException e) {
                getLog().error(e.getMessage());
            }
        }
        properties.setParam(SDKConstants.PROPERTY_PLATFORM, server.getVersion());
        if (!isCreatePlatform) {
            // set web app version for OpenMRS 2.2 and higher
            if (new Version(server.getVersion()).higher(new Version(Version.PRIOR)) && !isCreatePlatform) {
                for (File f: serverPath.listFiles()) {
                    if (f.getName().endsWith("." + Artifact.TYPE_WAR)) {
                        properties.setParam(SDKConstants.PROPERTY_VERSION, Version.parseVersionFromFile(f.getName()));
                        break;
                    }
                }
            }
            else {
                properties.setParam(SDKConstants.PROPERTY_VERSION, SDKConstants.WEBAPP_VERSIONS.get(server.getVersion()));
            }
        }
        String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, server.getServerId());
        properties.setParam(SDKConstants.PROPERTY_DB_NAME, dbName);
        properties.setParam(SDKConstants.PROPERTY_DEMO_DATA, String.valueOf(server.isIncludeDemoData()));
        properties.apply();
        String dbType = properties.getParam(SDKConstants.PROPERTY_DB_DRIVER);
        if (dbType.equals(SDKConstants.DRIVER_MYSQL)) {
            String uri = properties.getParam(SDKConstants.PROPERTY_DB_URI);
            uri = uri.substring(0, uri.lastIndexOf("/"));
            String user = properties.getParam(SDKConstants.PROPERTY_DB_USER);
            String pass = properties.getParam(SDKConstants.PROPERTY_DB_PASS);
            DBConnector connector = null;
            try {
                connector = new DBConnector(uri, user, pass, dbName);
                connector.checkAndCreate();
                connector.close();
                getLog().info("Database configured successfully");
            } catch (SQLException e) {
                throw new MojoExecutionException(e.getMessage());
            } finally {
                if (connector != null) try {
                    connector.close();
                } catch (SQLException e) {
                    getLog().error(e.getMessage());
                }
            }
        }

        return serverPath.getPath();
    }

    /**
     * Install modules from Artifact list
     * @param artifacts
     * @param outputDir
     */
    private void installModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(artifacts, outputDir, goal);
    }

    /**
     * Extract selected Artifact list
     * @param artifacts
     * @param outputDir
     * @throws MojoExecutionException
     */
    public void extractModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "unpack";
        prepareModules(artifacts, outputDir, goal);
    }

    /**
     * Handle list of modules
     * @param artifacts
     * @param outputDir
     * @param goal
     * @throws MojoExecutionException
     */
    private void prepareModules(List<Artifact> artifacts, String outputDir, String goal) throws MojoExecutionException {
        Element[] artifactItems = new Element[artifacts.size()];
        for (Artifact artifact: artifacts) {
            int index = artifacts.indexOf(artifact);
            artifactItems[index] = artifact.toElement(outputDir);
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal(goal),
                configuration(
                        element(name("artifactItems"), artifactItems)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
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
        extractModules(artifacts, zipFolder.getPath());
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
        Server server = new Server.ServerBuilder()
                .setServerId(serverId)
                .setVersion(version)
                .setDbDriver(dbDriver)
                .setDbUser(dbUser)
                .setDbUri(dbUri)
                .setDbPassword(dbPassword)
                .setFilePath(file)
                .setInteractiveMode(interactiveMode)
                .setDemoData(addDemoData)
                .build();
        // setup platform server
        String path = setup(server, true, true);
        getLog().info("Server configured successfully, path: " + path);
    }
}