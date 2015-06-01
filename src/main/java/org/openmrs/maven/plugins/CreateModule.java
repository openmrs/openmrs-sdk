package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal create-module
 * @requiresProject false
 * @requiresDependencyResolution test
 *
 */
public class CreateModule extends AbstractMojo {
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
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"), artifactId("maven-archetype-plugin"), version("2.3")),
                goal("generate"),
                configuration(
                        element(name("archetypeCatalog"),
                                "http://mavenrepo.openmrs.org/nexus/service/local/repositories/releases/content/archetype-catalog.xml"),
                        element(name("archetypeGroupId"),"org.openmrs.maven.archetypes"),
                        element(name("archetypeArtifactId"), "maven-archetype-openmrs-module-2.x"),
                        element(name("archetypeVersion"), "1.1")
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
