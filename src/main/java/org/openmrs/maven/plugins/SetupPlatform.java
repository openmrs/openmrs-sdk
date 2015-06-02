package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

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
     * @parameter expression="${interactiveMode}" default-value="false"
     */
    private String interactiveMode;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        executeMojo(
                plugin(groupId("org.openmrs.maven.archetypes"), artifactId("maven-archetype-openmrs-project"), version("1.0.2")),
                goal("generate"),
                configuration(
                        element(name("interactiveMode"), interactiveMode),
                        element(name("package"), "org.openmrs"),
                        element(name("archetypeGroupId"),"org.openmrs.maven.archetypes"),
                        element(name("archetypeArtifactId"), "maven-archetype-openmrs-project"),
                        element(name("archetypeVersion"), "1.0.0-SNAPSHOT")
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}