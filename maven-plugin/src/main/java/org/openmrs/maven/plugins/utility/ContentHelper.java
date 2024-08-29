package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;

/**
 * This class is responsible for
 */
public class ContentHelper {
	
	File serverDirectory;
	
	public ContentHelper(File serverDirectory) {
		this.serverDirectory = serverDirectory;
	}
	
	public void downloadContent(Artifact contentArtifact, ModuleInstaller moduleInstaller, File tempContentDir) throws MojoExecutionException {
		
		String artifactId = contentArtifact.getArtifactId();
		//create a artifact folder under temp_content
		File contentDir = new File(tempContentDir, artifactId);
		contentDir.mkdirs();
		
		moduleInstaller.installUnpackModule(contentArtifact, contentDir.getAbsolutePath());
		moveBackendConfig(artifactId, contentDir);
		
	}
	
	private void moveBackendConfig(String artifactId, File contentDir) throws MojoExecutionException {
		
		File configurationDir = new File(serverDirectory, SDKConstants.OPENMRS_SERVER_CONFIGURATION);	
		
		if (!contentDir.isDirectory() || !configurationDir.isDirectory()) {
			throw new MojoExecutionException("Both inputDir and configurationDir must be valid directories.");
		}
		
		File backendConfigFiles = new File(contentDir, "configs" + File.separator + "backend_config");
		
		for (File config : backendConfigFiles.listFiles()) {
			// Find a corresponding directory in the configurationDir with the same name as backendConfig
			File matchingConfigurationDir = new File(configurationDir, config.getName());
			File destDir = null;
			if (!matchingConfigurationDir.exists() || !matchingConfigurationDir.isDirectory()) {
				// Folder doesn't exist under configuration, we will be creating a new configuration here 
				matchingConfigurationDir.mkdirs();
				destDir = matchingConfigurationDir;				
			} 
			// Create a new artifact folder under the matching configuration folder
			destDir = new File(matchingConfigurationDir, artifactId);
			
			//copy config files to the matching configuration folder
			copyDirectory(config, destDir);
		}
	}
	
	private void copyDirectory(File config, File destDir) throws MojoExecutionException {
		try {
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			for (File file : config.listFiles()) {
				File destFile = new File(destDir, file.getName());
				if (file.isDirectory()) {
					copyDirectory(file, destFile);
				} else {
					Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}
	
}
