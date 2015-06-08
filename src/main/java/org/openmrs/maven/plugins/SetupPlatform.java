package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKValues;

import java.io.File;
import java.io.IOException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal setup-platform
 * @requiresProject false
 *
 */
public class SetupPlatform extends AbstractMojo {

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

    public void execute() throws MojoExecutionException {
        // check if user not set serverId parameter
        if (serverId == null) try {
            // prompt this param
            serverId = prompter.prompt("Please specify server id");
        } catch (PrompterException e) {
            e.printStackTrace();
        }
        // path to omrs home
        String omrsHome = System.getProperty("user.home") + File.separator + SDKValues.OPENMRS_SERVER_PATH;
        // path to server with serverId
        File serverPath = new File(omrsHome + File.separator + serverId);
        // check existence
        if (serverPath.exists()) {
            // show massage and exit
            getLog().error("Server with same id already created");
            return;
        }
        executeMojo(
                plugin(
                        groupId(SDKValues.ARCH_GROUP_ID),
                        artifactId(SDKValues.ARCH_ARTIFACT_ID),
                        version(SDKValues.ARCH_VERSION)
                ),
                goal("generate"),
                configuration(
                        element(name("archetypeCatalog"), SDKValues.ARCH_CATALOG),
                        element(name("interactiveMode"), interactiveMode),
                        //element(name("package"), SDKValues.PROJECT_PACKAGE),
                        element(name("archetypeGroupId"), SDKValues.ARCH_PROJECT_GROUP_ID),
                        element(name("archetypeArtifactId"), SDKValues.ARCH_PROJECT_ARTIFACT_ID),
                        element(name("archetypeVersion"), SDKValues.ARCH_PROJECT_VERSION)
                        //element(name("groupId"), SDKValues.PROJECT_GROUP_ID),
                        //element(name("artifactId"), serverId)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
        // move server after creating
        File currentProject = new File(System.getProperty("user.dir") + File.separator + serverId);
        try {
            // check if paths are not equal
            if (!currentProject.equals(serverPath)) FileUtils.moveDirectory(currentProject, serverPath);
            getLog().info("Server created successfully, path: " + serverPath.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // check if any db parameter is set
        if ((dbDriver != null) || (dbUser != null) || (dbPassword != null) || (dbUri != null)) {
            // configure properties
            String propertiesPath = serverPath.getPath() + File.separator + SDKValues.OPENMRS_SERVER_PROPERTIES;
            PropertyManager properties = new PropertyManager(propertiesPath);
            try {
                // ask for option which not set
                if (dbDriver == null) dbDriver = prompter.prompt("Please specify dbDriver option");
                if (dbUser == null) dbUser = prompter.prompt("Please specify dbUser option");
                if (dbPassword == null) dbPassword = prompter.prompt("Please specify dbPassword option");
                if (dbUri == null) dbUri = prompter.prompt("Please specify dbUri option");
                // set properties and write to file
                // set dbDriver property
                if (dbDriver.equals("postgresql")) properties.setParam("dbDriver", "org.postgresql.Driver");
                // if "mysql" or something else - set mysql driver (default)
                else properties.setParam("dbDriver", "com.mysql.jdbc.Driver");
                // set other params
                properties.setParam("dbDriver", dbDriver);
                properties.setParam("dbUser", dbUser);
                properties.setParam("dbPassword", dbPassword);
                properties.setParam("dbUri", dbUri);
                properties.apply();
            } catch (PrompterException e) {
                e.printStackTrace();
            }
        }
    }
}