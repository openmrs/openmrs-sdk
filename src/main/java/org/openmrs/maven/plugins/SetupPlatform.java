package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.utility.SDKValues;

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
        if (null == serverId) try {
            // prompt this param
            serverId = prompter.prompt("Please specify server id");
        } catch (PrompterException e) {
            e.printStackTrace();
        }
        //getLog().info(System.getProperty("user.home"));
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
    }
}