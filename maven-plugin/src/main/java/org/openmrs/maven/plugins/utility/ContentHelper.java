package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class downloads and moves content backend config to respective configuration folders.
 */
public class ContentHelper {

    public static final String FRONTEND_CONFIG_FOLDER = Paths.get("configs", "frontend_config").toString();
    public static final String BACKEND_CONFIG_FOLDER = Paths.get("configs", "backend_config").toString();

    private final ModuleInstaller moduleInstaller;
    private final Wizard wizard;

    public ContentHelper(MavenEnvironment mavenEnvironment) {
        this.moduleInstaller = new ModuleInstaller(mavenEnvironment);
        this.wizard = mavenEnvironment.getWizard();
    }

    private File unpackArtifact(Artifact contentArtifact) throws MojoExecutionException {
        String artifactId = contentArtifact.getArtifactId();
        // create a temporary artifact folder
        File sourceDir;
        try {
            sourceDir = Files.createTempDirectory("openmrs-sdk-" + artifactId).toFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while trying to create temporary directory", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(sourceDir)));

        moduleInstaller.installAndUnpackModule(contentArtifact, sourceDir.getAbsolutePath());
        return sourceDir;
    }

    private void moveBackendConfig(Artifact contentArtifact, File targetDir) throws MojoExecutionException {
        File sourceDir = unpackArtifact(contentArtifact);
        try {
            File backendConfigFiles = sourceDir.toPath().resolve(BACKEND_CONFIG_FOLDER).toFile();
            Path targetPath = targetDir.toPath().toAbsolutePath();

            if (backendConfigFiles.exists()) {
                File[] configDirectories = backendConfigFiles.listFiles(File::isDirectory);
                if (configDirectories != null) {
                    for (File config : configDirectories) {
                        Path destDir = targetPath.resolve(config.getName()).resolve(contentArtifact.getArtifactId());
                        Files.createDirectories(destDir);

                        // Copy config files to the matching configuration folder
                        FileUtils.copyDirectory(config, destDir.toFile());
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying backend configuration: " + e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(sourceDir);
        }
    }

    public List<File> extractAndGetAllContentFrontendConfigs(Artifact contentArtifact) throws MojoExecutionException {
        File sourceDir = unpackArtifact(contentArtifact);
        List<File> configFiles = new ArrayList<>();
        File frontendConfigFiles = sourceDir.toPath().resolve(FRONTEND_CONFIG_FOLDER).toFile();

        if (frontendConfigFiles.exists() && frontendConfigFiles.isDirectory()) {
            File[] files = frontendConfigFiles.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.length() > 5) {
                        configFiles.add(file);
                    }
                }
            }
        } else {
            throw new MojoExecutionException("Error: Frontend configuration folder not found.");
        }
        return configFiles;
    }

    // This method now sets the static moduleInstaller
    public void downloadAndMoveContentBackendConfig(File serverDirectory, DistroProperties distroProperties) throws MojoExecutionException {
        if (distroProperties != null) {
            File targetDir = new File(serverDirectory, SDKConstants.OPENMRS_SERVER_CONFIGURATION);
            List<Artifact> contents = distroProperties.getContentArtifacts();

            if (contents != null) {
                for (Artifact content : contents) {
                    wizard.showMessage("Downloading Content: " + content + "\n");
                    moveBackendConfig(content, targetDir);
                }
            }
        }
    }

    public List<File> collectFrontendConfigs(DistroProperties distroProperties) throws MojoExecutionException {
        List<File> allConfigFiles = new ArrayList<>();
        if (distroProperties != null) {
            List<Artifact> contents = distroProperties.getContentArtifacts();
            if (contents != null) {
                for (Artifact contentArtifact : contents) {
                    allConfigFiles.addAll(extractAndGetAllContentFrontendConfigs(contentArtifact));
                }
            }
        }
        return allConfigFiles;
    }
}
