package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentHelper {

	private final MavenSession session;

	private final BuildPluginManager pluginManager;

	private final MavenProject mavenProject;

	private static final Logger logger = LoggerFactory.getLogger(ContentHelper.class);

	public ContentHelper(MavenSession session, MavenProject mavenProject, BuildPluginManager pluginManager) {
		this.session = session;
		this.mavenProject = mavenProject;
		this.pluginManager = pluginManager;
	}

	public void downloadContent(File configurationDir, File tempContentDir, Artifact contentArtifact,
			ModuleInstaller moduleInstaller) throws MojoExecutionException {

		String artifactId = contentArtifact.getArtifactId();
		File contentDir = new File(tempContentDir, artifactId);

		moduleInstaller.installUnpackModule(contentArtifact, contentDir.getAbsolutePath());
		moveBackendConfig(contentDir, configurationDir, artifactId);
	}

	private void moveBackendConfig(File inputDir, File configurationDir, String artifactId)
			throws MojoExecutionException {

		// Check if inputDir and outputDir are valid directories
		if (!inputDir.isDirectory() || !configurationDir.isDirectory()) {
			throw new MojoExecutionException("Both inputDir and outputDir must be valid directories.");
		}

		// List all directories in the inputDir
		File backendConfigs = new File(inputDir, "configs" + File.separator + "backend_config");

		if (backendConfigs == null || backendConfigs.listFiles().length == 0) {
			throw new MojoExecutionException("No directories to process under content configuration directories");
		}

		for (File backendConfig : backendConfigs.listFiles()) {
			// Find a corresponding directory in the outputDir with the same name as
			// inputSubDir
			File matchingConfigurationDir = new File(configurationDir, backendConfig.getName());

			File artifactDir = null;

			if (!matchingConfigurationDir.exists() && !matchingConfigurationDir.isDirectory()) {
				// this folder is from content zip
				artifactDir = matchingConfigurationDir;
			} else {
				// Create a new folder with the artifactId under the matching output directory
				artifactDir = new File(matchingConfigurationDir, artifactId);
				if (!artifactDir.exists()) {
					artifactDir.mkdirs();
				}
			}
			// Copy the content from inputDir to the newly created artifactDir
			copyDirectory(backendConfig, artifactDir);

		}
	}

	private void copyDirectory(File sourceDir, File destDir) throws MojoExecutionException {
		try {
			if (!destDir.exists()) {
				destDir.mkdirs();
			}

			for (File file : sourceDir.listFiles()) {
				File destFile = new File(destDir, file.getName());
				if (file.isDirectory()) {
					copyDirectory(file, destFile);
				} else {
					Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

}
