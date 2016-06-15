package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.ModuleInstall;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

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

    public void installCoreModules(Server server, boolean isCreatePlatform, DistroProperties properties) throws MojoExecutionException, MojoFailureException {
        List<Artifact> coreModules;
        // install other modules
        if (!isCreatePlatform && properties != null) {
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
        } else {
            coreModules = SDKConstants.getCoreModules(server.getVersion(), isCreatePlatform);
            if (coreModules == null) {
                throw new MojoExecutionException(String.format("Invalid version: '%s'", server.getVersion()));
            }
            installModules(coreModules, server.getServerDirectory().getPath());
        }
        // install user modules
        if (server != null) {
            String values = server.getParam(Server.PROPERTY_USER_MODULES);
            if (values != null) {
                ModuleInstall installer = new ModuleInstall(mavenProject, mavenSession, pluginManager);
                String[] modules = values.split(Server.COMMA);
                for (String mod: modules) {
                    String[] params = mod.split(Server.SLASH);
                    // check
                    if (params.length == 3) {
                        installer.installModule(server.getServerId(), params[0], params[1], params[2]);
                    }
                    else throw new MojoExecutionException("Properties file parse error - cannot read user modules list");
                }
            }
        }
    }

    public void installModules(List<Artifact> artifacts, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModules(artifacts, outputDir, goal);
    }

    public void installModule(Artifact artifact, String outputDir) throws MojoExecutionException {
        final String goal = "copy";
        prepareModule(artifact, outputDir, goal);
    }

    public DistroProperties downloadDistroProperties(File serverPath, Server server) throws MojoExecutionException {

        Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
        artifact.setDestFileName("openmrs-distro.jar");
        List<Element> artifactItems = new ArrayList<Element>();
        Element element = artifact.toElement(serverPath.toString());
        artifactItems.add(element);

        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"), artifactItems.toArray(new Element[0]))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );

        DistroProperties distroProperties = null;
        File file = new File(serverPath, artifact.getDestFileName());
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()){
                ZipEntry zipEntry = entries.nextElement();
                if("openmrs-distro.properties".equals(zipEntry.getName())){
                    Properties properties = new Properties();
                    properties.load(zipFile.getInputStream(zipEntry));
                    distroProperties = new DistroProperties(properties);
                }
            }

            zipFile.close();


        } catch (IOException e) {
            throw new RuntimeException("Could not read " + file.toString(), e);
        } finally {
            IOUtils.closeQuietly(zipFile);
        }

        return distroProperties;
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

            artifact.setVersion(versionsHelper.inferVersion(artifact));
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
        artifact.setVersion(versionsHelper.inferVersion(artifact));
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
