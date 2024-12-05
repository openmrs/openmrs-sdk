package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    private static final String GOAL_COPY = "copy";

    private static final String GOAL_UNPACK = "unpack";

    final MavenEnvironment mavenEnvironment;

    public ModuleInstaller(MavenEnvironment mavenEnvironment) {
        this.mavenEnvironment = mavenEnvironment;
    }

    public void installDefaultModules(Server server) throws MojoExecutionException {
        boolean isPlatform = server.getVersion() == null;  // this might be always true, in which case `getCoreModules` can be simplified
        List<Artifact> coreModules = SDKConstants.getCoreModules(server.getPlatformVersion(), isPlatform);
        if (coreModules == null) {
            throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getPlatformVersion()));
        }
        installModules(coreModules, server.getServerDirectory().getPath());
    }

    public void installModulesForDistro(Server server, DistroProperties properties) throws MojoExecutionException {
        List<Artifact> coreModules;
        // install other modules
        coreModules = properties.getWarArtifacts();
        if (coreModules == null) {
            throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getVersion()));
        }
        installModules(coreModules, server.getServerDirectory().getPath());
        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        List<Artifact> artifacts = properties.getModuleArtifacts();
        // install modules for each version
        installModules(artifacts, modules.getPath());
    }

    public void installModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        if (!artifacts.isEmpty()) {
            prepareModules(artifacts.toArray(new Artifact[0]), outputDir, GOAL_COPY);
        }
    }

    public void installModule(Artifact artifact, String outputDir) throws MojoExecutionException {
        prepareModules(new Artifact[] { artifact }, outputDir, GOAL_COPY);
    }

    /**
     * @param artifact the artifact retrieve from Maven
     * @param outputDir the directory into which the artifact should be unpacked
     * @param includes optionally allows limiting the unpacked artifacts to only those specified in the directory that matches this name
     * @throws MojoExecutionException
     */
    public void installAndUnpackModule(Artifact artifact, File outputDir, String includes) throws MojoExecutionException {
        if (!outputDir.exists()) {
            throw new MojoExecutionException("Output directory does not exist: " + outputDir);
        }
        if (StringUtils.isBlank(includes)) {
            installAndUnpackModule(artifact, outputDir.getAbsolutePath());
        }
        else {
            File tempDir = null;
            try {
                tempDir = Files.createTempDirectory(artifact.getArtifactId() + "-" + UUID.randomUUID()).toFile();
                installAndUnpackModule(artifact, tempDir.getAbsolutePath());
                boolean copied = false;
                for (File f : Objects.requireNonNull(tempDir.listFiles())) {
                    if (f.isDirectory() && f.getName().equals(includes)) {
                        FileUtils.copyDirectory(f, outputDir);
                        copied = true;
                        break;
                    }
                }
                if (!copied) {
                    throw new MojoExecutionException("No directory named " + includes + " exists in artifact " + artifact);
                }
            }
            catch (IOException e) {
                throw new MojoExecutionException("Unable to create temporary directory to install " + artifact);
            }
            finally {
                FileUtils.deleteQuietly(tempDir);
            }
        }
    }

    public void installAndUnpackModule(Artifact artifact, String outputDir) throws MojoExecutionException {
        Path markersDirectory;
        try {
            markersDirectory = Files.createTempDirectory("openmrs-sdk-markers");
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating markers directory", e);
        }

        prepareModules(new Artifact[] { artifact }, outputDir, GOAL_UNPACK,
                element("overWriteSnapshots", "true"),
                element("overWriteReleases", "true"),
                element("markersDirectory", markersDirectory.toAbsolutePath().toString()));

        FileUtils.deleteQuietly(markersDirectory.toFile());
    }

    /**
     * Handle list of modules
     * @param artifacts
     * @param outputDir
     * @param goal
     * @throws MojoExecutionException
     */
    private void prepareModules(Artifact[] artifacts, String outputDir, String goal, MojoExecutor.Element... additionalConfiguration) throws MojoExecutionException {
        MojoExecutor.Element[] artifactItems = new MojoExecutor.Element[artifacts.length];
        for (int index = 0; index < artifacts.length; index++) {
            artifactItems[index] = artifacts[index].toElement(outputDir);
        }

        List<MojoExecutor.Element> configuration = new ArrayList<>();
        configuration.add(element("artifactItems", artifactItems));

        configuration.addAll(Arrays.asList(additionalConfiguration));

        executeMojo(
                plugin(
                        groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
                        artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
                        version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
                ),
                goal(goal),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(
                        mavenEnvironment.getMavenProject(),
                        mavenEnvironment.getMavenSession(),
                        mavenEnvironment.getPluginManager()
                )
        );
    }
}
