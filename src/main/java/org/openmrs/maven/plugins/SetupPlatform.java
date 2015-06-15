package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

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
     * @parameter expression="${interactiveMode}" default-value="true"
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
     * @parameter expression="${version} default-value="1.11.2"
     */
    private String version;

    /**
     * DB Driver type
     *
     * @parameter expression="${dbDriver}
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter expression="${dbUri}
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter expression="${dbUser}
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter expression="${dbPassword}
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
     * @param serverId
     * @param version
     * @param dbDriver
     * @param dbUri
     * @param dbUser
     * @param dbPassword
     * @param interactiveMode
     * @throws MojoExecutionException
     */
    public Server setup(String serverId,
                      String version,
                      String dbDriver,
                      String dbUri,
                      String dbUser,
                      String dbPassword,
                      String interactiveMode) throws MojoExecutionException {
        File omrsPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        if (serverId == null) try {
            String defaultId = "server";
            int indx = 0;
            while (new File(omrsPath, defaultId).exists()) {
                indx++;
                defaultId = "server" + String.valueOf(indx);
            }
            serverId = prompter.prompt("Define value for property 'serverId': (default: '" + defaultId + "')");
            if (serverId.equals("")) serverId = defaultId;
        } catch (PrompterException e) {
            e.printStackTrace();
        }
        File serverPath = new File(omrsPath, serverId);
        if (serverPath.exists()) throw new MojoExecutionException("Server with same id already created");
        executeMojo(
                plugin(
                        groupId(SDKConstants.ARCH_GROUP_ID),
                        artifactId(SDKConstants.ARCH_ARTIFACT_ID),
                        version(SDKConstants.ARCH_VERSION)
                ),
                goal("generate"),
                configuration(
                        element(name("archetypeCatalog"), SDKConstants.ARCH_CATALOG),
                        element(name("interactiveMode"), interactiveMode),
                        //element(name("package"), SDKConstants.PROJECT_PACKAGE),
                        element(name("archetypeGroupId"), SDKConstants.ARCH_PROJECT_GROUP_ID),
                        element(name("archetypeArtifactId"), SDKConstants.ARCH_PROJECT_ARTIFACT_ID),
                        element(name("archetypeVersion"), SDKConstants.ARCH_PROJECT_VERSION),
                        element(name("basedir"), omrsPath.getPath())
                        //element(name("groupId"), SDKConstants.PROJECT_GROUP_ID),
                        //element(name("artifactId"), serverId),
                        //element(name("version"), version)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
        getLog().info("Server created successfully, path: " + serverPath.getPath());
        if ((dbDriver != null) || (dbUser != null) || (dbPassword != null) || (dbUri != null)) {
            File propertiesFile = new File(serverPath.getPath(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
            PropertyManager properties = new PropertyManager(propertiesFile.getPath());
            try {
                String defaultDriver = "mysql";
                if (dbDriver == null) dbDriver = prompter.prompt("Define value for property 'dbDriver': (default: 'mysql')");
                if ((dbDriver == null) || (dbDriver.equals(""))) dbDriver = defaultDriver;
                String defaultUri = SDKConstants.URI_MYSQL;
                if ((dbDriver.equals("postgresql")) || (dbDriver.equals(SDKConstants.DRIVER_POSTGRESQL))) {
                    properties.setParam("dbDriver", SDKConstants.DRIVER_POSTGRESQL);
                    defaultUri = SDKConstants.URI_POSTGRESQL;
                }
                else if ((dbDriver.equals("h2")) || (dbDriver.equals(SDKConstants.DRIVER_H2))) {
                    properties.setParam("dbDriver", SDKConstants.DRIVER_H2);
                    defaultUri = SDKConstants.URI_H2;
                }
                else if (dbDriver.equals("mysql")) properties.setParam("dbDriver", SDKConstants.DRIVER_MYSQL);
                else properties.setParam("dbDriver", dbDriver);

                if (dbUri == null) dbUri = prompter.prompt("Define value for property 'dbUri': (default: '" + defaultUri + "')");
                if ((dbUri == null) || (dbUri.equals(""))) dbUri = defaultUri;

                String defaultUser = "root";
                if (dbUser == null) dbUser = prompter.prompt("Define value for property 'dbUser': (default: '" + defaultUser + "')");
                if ((dbUser == null) || (dbUser.equals(""))) dbUser = defaultUser;
                if (dbPassword == null) dbPassword = prompter.prompt("Define value for property 'dbPassword'");

                properties.setParam("dbDriver", dbDriver);
                properties.setParam("dbUser", dbUser);
                properties.setParam("dbPassword", dbPassword);
                properties.setParam("dbUri", dbUri);
                properties.apply();
            } catch (PrompterException e) {
                e.printStackTrace();
            }
        }
        return new Server(serverId, serverPath.getPath(), dbDriver, dbUri, dbUser, dbPassword);
    }

    public void execute() throws MojoExecutionException {
        setup(serverId, version, dbDriver, dbUri, dbUser, dbPassword, interactiveMode);
    }
}