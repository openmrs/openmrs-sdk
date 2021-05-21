package org.openmrs.maven.plugins.utility;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * Handles installing modules on server
 */
public class ModuleInstaller {

    private static final String GOAL_UNPACK = "unpack";

    MavenProject mavenProject;

    MavenSession mavenSession;

    BuildPluginManager pluginManager;

    VersionsHelper versionsHelper;

    public ModuleInstaller(MavenProject mavenProject,
                           MavenSession mavenSession,
                           BuildPluginManager pluginManager,
                           VersionsHelper versionsHelper) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.versionsHelper = versionsHelper;
    }

    public void installCoreModules(Server server, DistroProperties properties, DistroHelper distroHelper) throws MojoExecutionException {
        List<Artifact> coreModules;
        // install other modules
//        if (properties != null) {
            coreModules = properties.getWarArtifacts(distroHelper, server.getServerDirectory());
            if (coreModules == null) {
                throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getVersion()));
            }
            installModules(coreModules, server.getServerDirectory().getPath());
            File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
            modules.mkdirs();
            List<Artifact> artifacts = properties.getModuleArtifacts(distroHelper, server.getServerDirectory());
            // install modules for each version
            installModules(artifacts, modules.getPath());
//        } else {
//            coreModules = SDKConstants.getCoreModules(server.getPlatformVersion(), isCreatePlatform);
//            if (coreModules == null) {
//                throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getPlatformVersion()));
//            }
//            installModules(coreModules, server.getServerDirectory().getPath());
//        }
    }

    public void installModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(artifacts, outputDir, goal);
    }

    public void installModule(Artifact artifact, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModule(artifact, outputDir, goal);
    }

    /**
     * Extract selected Artifact list
     * @param artifacts
     * @param outputDir
     * @throws MojoExecutionException
     */
    public void extractModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        prepareModules(artifacts, outputDir, GOAL_UNPACK);
    }

    /**
     * Handle list of modules
     * @param artifacts
     * @param outputDir
     * @param goal
     * @throws MojoExecutionException
     */
    private void prepareModules(List<Artifact> artifacts, String outputDir, String goal) throws MojoExecutionException {
        MojoExecutor.Element[] artifactItems = new MojoExecutor.Element[artifacts.size()];
        for (Artifact artifact: artifacts) {
            int index = artifacts.indexOf(artifact);
            artifactItems[index] = artifact.toElement(outputDir);
        }
        List<MojoExecutor.Element> configuration = new ArrayList<MojoExecutor.Element>();
        configuration.add(element("artifactItems", artifactItems));
        if (goal.equals(GOAL_UNPACK)) {
            configuration.add(element("overWriteSnapshots", "true"));
            configuration.add(element("overWriteReleases", "true"));
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
    private void prepareModule(Artifact artifact, String outputDir, String goal) throws MojoExecutionException {
        MojoExecutor.Element artifactElement = artifact.toElement(outputDir);
        List<MojoExecutor.Element> configuration = new ArrayList<MojoExecutor.Element>();
        configuration.add(element("artifactItems", artifactElement));
        if (goal.equals(GOAL_UNPACK)) {
            configuration.add(element("overWriteSnapshots", "true"));
            configuration.add(element("overWriteReleases", "true"));
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
