package org.openmrs.maven.plugins.utility;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    final MavenProject mavenProject;

    final MavenSession mavenSession;

    final BuildPluginManager pluginManager;

    final VersionsHelper versionsHelper;

    public ModuleInstaller(MavenProject mavenProject,
                           MavenSession mavenSession,
                           BuildPluginManager pluginManager,
                           VersionsHelper versionsHelper) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.versionsHelper = versionsHelper;
    }

    public void installDefaultModules(Server server) throws MojoExecutionException {
        boolean isPlatform = server.getVersion() == null;  // this might be always true, in which case `getCoreModules` can be simplified
        List<Artifact> coreModules =Optional.ofNullable(SDKConstants.getCoreModules(server.getPlatformVersion(), isPlatform))
                .orElseThrow(() -> new MojoExecutionException(String.format("Invalid version: '%s'", server.getPlatformVersion())));
        installModules(coreModules, server.getServerDirectory().getPath());
    }

    public void installModulesForDistro(Server server, DistroProperties properties, DistroHelper distroHelper) throws MojoExecutionException {
        List<Artifact> coreModules = Optional.ofNullable(properties.getWarArtifacts(distroHelper, server.getServerDirectory()))
                .orElseThrow(() -> new MojoExecutionException(String.format("Invalid version: '%s'", server.getVersion())));
        installModules(coreModules, server.getServerDirectory().getPath());
        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        List<Artifact> artifacts = properties.getModuleArtifacts(distroHelper, server.getServerDirectory());
        // install modules for each version
        installModules(artifacts, modules.getPath());
    }

    public void installModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(artifacts.toArray(new Artifact[0]), outputDir, goal);
    }

    public void installModules(Artifact[] artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(artifacts, outputDir, goal);
    }

    public void installModule(Artifact artifact, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(new Artifact[] { artifact }, outputDir, goal);
    }

    /**
     * Handle list of modules
     * @param artifacts
     * @param outputDir
     * @param goal
     * @throws MojoExecutionException
     */
    private void prepareModules(Artifact[] artifacts, String outputDir, String goal) throws MojoExecutionException {
        MojoExecutor.Element[] artifactItems = new MojoExecutor.Element[artifacts.length];
        for (int index = 0; index < artifacts.length; index++) {
            artifactItems[index] = artifacts[index].toElement(outputDir);
        }

        List<MojoExecutor.Element> configuration = new ArrayList<>();
        configuration.add(element("artifactItems", artifactItems));
        if (goal.equals(GOAL_UNPACK)) {
            configuration.add(element("overWriteSnapshots", "true"));
            configuration.add(element("overWriteReleases", "true"));
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
                        artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
                        version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
