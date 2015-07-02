package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal setup-platform
 * @requiresProject false
 *
 */
public class SetupPlatform extends AbstractMojo {

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
     * @parameter expression="${version}" default-value="1.11.2"
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
     * @param isPlatform - flag for platform setup
     * @throws MojoExecutionException
     */
    public String setup(Server server, Boolean isPlatform) throws MojoExecutionException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File openMRSPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        try {
            server.setServerId(helper.promptForNewServerIfMissing(openMRSPath.getPath(), server.getServerId()));
        } catch (PrompterException e) {
            getLog().error(e.getMessage());
        }
        File serverPath = new File(openMRSPath, server.getServerId());
        File propertyPath = new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        if (propertyPath.exists()) throw new MojoExecutionException("Server with same id already created");
        // install platform
        installModules(SDKConstants.PLATFORM, serverPath.getPath(), server.getVersion(), isPlatform);
        // install modules
        if (!isPlatform) {
            File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
            modules.mkdirs();
            List<Artifact> artifacts = SDKConstants.ARTIFACTS.get(server.getVersion());
            // install modules for each version
            installModules(artifacts, modules.getPath(), server.getVersion(), isPlatform);
        }
        getLog().info("Server created successfully, path: " + serverPath.getPath());
        File propertiesFile = new File(serverPath.getPath(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        PropertyManager properties = new PropertyManager(propertiesFile.getPath(), getLog());
        properties.setDefaults();
        // configure db properties
        if ((server.getDbDriver() != null) ||
                (server.getDbUser() != null) ||
                (server.getDbPassword() != null) ||
                (server.getDbUri() != null) ||
                !isPlatform) {
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
        properties.setParam(SDKConstants.PROPERTY_VERSION, server.getVersion());
        properties.setParam(SDKConstants.PROPERTY_PLATFORM, String.valueOf(isPlatform));
        properties.apply();
        return serverPath.getPath();
    }

    /**
     * Install modules from Artifact list
     * @param artifacts
     * @param outputDir
     * @param version
     * @param isPlatform
     */
    private void installModules(List<Artifact> artifacts, String outputDir, String version, boolean isPlatform) throws MojoExecutionException{
        Element[] artifactItems = new Element[artifacts.size()];
        for (Artifact artifact: artifacts) {
            int index = artifacts.indexOf(artifact);
            if (artifact.isWar()) {
                if (isPlatform) {
                    artifactItems[index] = artifact.toElement(outputDir, version);
                }
                else {
                    String webAppVersion = SDKConstants.WEBAPP_VERSIONS.get(version);
                    artifactItems[index] = artifact.toElement(outputDir, webAppVersion);
                }
            }
            else {
                artifactItems[index] = artifact.toElement(outputDir);
            }
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"), artifactItems)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    public void execute() throws MojoExecutionException {
        Server server = new Server.ServerBuilder()
                .setServerId(serverId)
                .setVersion(version)
                .setDbDriver(dbDriver)
                .setDbUser(dbUser)
                .setDbUri(dbUri)
                .setDbPassword(dbPassword)
                .setInteractiveMode(interactiveMode)
                .build();
        // setup platform server
        String path = setup(server, true);
        getLog().info("Server configured successfully, path: " + path);
    }
}