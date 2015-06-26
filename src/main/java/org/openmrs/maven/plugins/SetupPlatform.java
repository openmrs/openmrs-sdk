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
     * @param requireDbParams - require db params if not selected
     * @throws MojoExecutionException
     */
    public String setup(Server server, Boolean requireDbParams) throws MojoExecutionException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File openMRSPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        try {
            server.setServerId(helper.promptForNewServerIfMissing(openMRSPath.getPath(), server.getServerId()));
        } catch (PrompterException e) {
            getLog().error(e.getMessage());
        }
        File serverPath = new File(openMRSPath, server.getServerId());
        if (serverPath.exists()) throw new MojoExecutionException("Server with same id already created");
        File modules = new File (serverPath, "modules");
        modules.mkdirs();
        // get modules version
        String openMRSVersion = server.getVersion().startsWith("1.") ? "1.x": server.getVersion();
        List<Artifact> artifacts = SDKConstants.ARTIFACTS.get(openMRSVersion);
        Element[] artifactItems = new Element[artifacts.size()];
        for (Artifact artifact: artifacts) {
            int index = artifacts.indexOf(artifact);
            // some hack for webapp and h2
            if (index < 2) {
                // web app version for 1.x modules should be equal OpenMRS version
                if ((index == 0) && (server.getVersion().startsWith("1."))) artifact.setVersion(server.getVersion());
                // put to server root
                artifactItems[index] =
                        artifact.setOutputDirectory(serverPath.getAbsolutePath()).toElement();
            }
            else {
                // put to "modules"
                artifactItems[index] =
                        artifact.setOutputDirectory(modules.getAbsolutePath()).toElement();
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
        getLog().info("Server created successfully, path: " + serverPath.getPath());
        if ((server.getDbDriver() != null) ||
                (server.getDbUser() != null) ||
                (server.getDbPassword() != null) ||
                (server.getDbUri() != null) ||
                requireDbParams) {
            File propertiesFile = new File(serverPath.getPath(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
            PropertyManager properties = new PropertyManager(propertiesFile.getPath(), getLog());
            properties.setDefaults();
            try {
                server.setDbDriver(helper.promptForValueIfMissingWithDefault(dbDriver, "dbDriver", "mysql"));
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

                server.setDbUri(helper.promptForValueIfMissingWithDefault(dbUri, "dbUri", defaultUri));
                String defaultUser = "root";
                server.setDbUser(helper.promptForValueIfMissingWithDefault(dbUser, "dbUser", defaultUser));
                server.setDbPassword(helper.promptForValueIfMissing(dbPassword, "dbPassword"));
                //properties.setParam("dbDriver", server.getDbDriver());
                properties.setParam(SDKConstants.PROPERTY_DB_USER, server.getDbUser());
                properties.setParam(SDKConstants.PROPERTY_DB_PASS, server.getDbPassword());
                properties.setParam(SDKConstants.PROPERTY_DB_URI, server.getDbUri());
                properties.apply();
            } catch (PrompterException e) {
                getLog().error(e.getMessage());
            }
        }
        return serverPath.getPath();
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
        setup(server, false);
        getLog().info("Server configured successfully");
    }
}