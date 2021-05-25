package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

public class NodeHelper {
    private static final String FRONTEND_MAVEN_GROUP_ID = "com.github.eirslett";
    private static final String FRONTEND_MAVEN_ARTIFACT_ID = "frontend-maven-plugin";
    private static final String FRONTEND_MAVEN_VERSION = "1.12.0";

    private MavenSession mavenSession;

    private MavenProject mavenProject;

    private BuildPluginManager pluginManager;

    public NodeHelper(MavenProject mavenProject,
                      MavenSession mavenSession,
                      BuildPluginManager pluginManager) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
    }

    public void installNodeAndNpm(String nodeVersion, String npmVersion) throws MojoExecutionException {
        List<MojoExecutor.Element> configuration = new ArrayList<>();
        configuration.add(element("nodeVersion", "v" + nodeVersion));
        configuration.add(element("npmVersion", npmVersion));
        runFrontendMavenPlugin("install-node-and-npm", configuration);
    }

    public void runNpm(String arguments) throws MojoExecutionException {
        runFrontendMavenPlugin("npm", Arrays.asList(element("arguments", arguments)));
    }

    public void runNpx(String arguments) throws MojoExecutionException {
        runFrontendMavenPlugin("npx", Arrays.asList(element("arguments", arguments)));
    }

    private void runFrontendMavenPlugin(String goal, List<MojoExecutor.Element> configuration) throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId(FRONTEND_MAVEN_GROUP_ID),
                        artifactId(FRONTEND_MAVEN_ARTIFACT_ID),
                        version(FRONTEND_MAVEN_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new MojoExecutor.Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

}
