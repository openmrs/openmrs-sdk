package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.utility.SDKConstants;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal create-platform-module
 * @requiresProject false
 *
 */
public class CreatePlatformModule extends AbstractMojo{

    /**
     * The project currently being build.
     *
     * @parameter property="project"
     * @required
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter property="session"
     * @required
     */
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId(SDKConstants.WIZARD_GROUP_ID),
                        artifactId(SDKConstants.WIZARD_ARTIFACT_ID),
                        version(SDKConstants.WIZARD_VERSION)
                ),
                goal("generate"),
                configuration(
                        element(name("archetypeCatalog"), SDKConstants.ARCH_CATALOG)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
