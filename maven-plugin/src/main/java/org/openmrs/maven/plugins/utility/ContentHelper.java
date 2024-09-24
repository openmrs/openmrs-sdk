package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;

/**
 * This class is downloads and moves content backend config to respective configuration folders
 */
public class ContentHelper {
	
	public static final String FRONTEND_CONFIG_FOLDER = Paths.get("configs", "frontend_config").toString();
	public static final String BACKEND_CONFIG_FOLDER = Paths.get("configs", "backend_config").toString();
	
	public static void downloadContent(Artifact contentArtifact, ModuleInstaller moduleInstaller, File targetDir) throws MojoExecutionException {
		String artifactId = contentArtifact.getArtifactId();
		// create a temporary artifact folder
		File sourceDir;
		try {
			sourceDir = Files.createTempDirectory("openmrs-sdk-" + artifactId + "-").toFile();
		} catch (IOException e) {
			throw new MojoExecutionException("Exception while trying to create temporary directory", e);
		}

		moduleInstaller.installAndUnpackModule(contentArtifact, sourceDir.getAbsolutePath());
		moveBackendConfig(artifactId, sourceDir, targetDir);

		FileUtils.deleteQuietly(sourceDir);
	}
	
	private static void moveBackendConfig(String artifactId, File sourceDir, File targetDir) throws MojoExecutionException {
		try {				
			File backendConfigFiles = sourceDir.toPath().resolve(BACKEND_CONFIG_FOLDER).toFile();
			Path targetPath = targetDir.toPath().toAbsolutePath();

			if (backendConfigFiles.exists()) {
				File[] configDirectories =  backendConfigFiles.listFiles(File::isDirectory);
				if (configDirectories != null) {
					for (File config : configDirectories) {
						Path destDir = targetPath.resolve(config.getName()).resolve(artifactId);
						Files.createDirectories(destDir);

						//copy config files to the matching configuration folder
						FileUtils.copyDirectory(config, destDir.toFile());
					}
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error copying backend configuration: " + e.getMessage(), e);
		}
	}
	
	public static void downloadAndMoveContentBackendConfig(File serverDirectory, DistroProperties distroProperties, ModuleInstaller moduleInstaller, Wizard wizard) throws MojoExecutionException {
		if (distroProperties != null) {
			File targetDir = new File(serverDirectory, SDKConstants.OPENMRS_SERVER_CONFIGURATION);				
			List<Artifact> contents = distroProperties.getContentArtifacts();
								
			if (contents != null) {
				for (Artifact content : contents) {
					wizard.showMessage("Downloading Content: " + content + "\n");
					ContentHelper.downloadContent(content, moduleInstaller, targetDir);
				}
			}			
		}
	}	
}
