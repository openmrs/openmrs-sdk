package org.openmrs.maven.plugins.utility;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.ArrayList;
import java.util.Collections;
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

    private final MavenSession mavenSession;

    private final MavenProject mavenProject;

    private final BuildPluginManager pluginManager;

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
        runFrontendMavenPlugin("npm", Collections.singletonList(element("arguments", arguments)));
    }

    public void runNpx(String arguments) throws MojoExecutionException {
        runFrontendMavenPlugin("npx", Collections.singletonList(element("arguments", arguments)));
    }

    private void runFrontendMavenPlugin(String goal, List<MojoExecutor.Element> configuration) throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId(SDKConstants.FRONTEND_PLUGIN_GROUP_ID),
                        artifactId(SDKConstants.FRONTEND_PLUGIN_ARTIFACT_ID),
                        version(SDKConstants.FRONTEND_PLUGIN_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new MojoExecutor.Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

}
