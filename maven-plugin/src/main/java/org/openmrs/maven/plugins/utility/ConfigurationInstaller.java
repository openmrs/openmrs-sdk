package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Supports retrieving the configuration artifacts specified in the distribution
 * and installing them into an SDK server or directory
 */
public class ConfigurationInstaller {

	private final Wizard wizard;

	private final ModuleInstaller moduleInstaller;

	public ConfigurationInstaller(DistroHelper distroHelper) {
		this.wizard = distroHelper.wizard;
		this.moduleInstaller = new ModuleInstaller(distroHelper.mavenProject, distroHelper.mavenSession, distroHelper.pluginManager, distroHelper.versionHelper);
	}

	/**
	 * Installs the configuration packages defined in the distro properties to the given server
	 * @param server the server to deploy to
	 * @param distroProperties the distro properties containing "config" elements
	 * @throws MojoExecutionException
	 */
	public void installToServer(Server server, DistroProperties distroProperties) throws MojoExecutionException {
		File directory = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_CONFIGURATION);
		installToDirectory(directory, distroProperties);
	}

	public void installToDirectory(File installDir, DistroProperties distroProperties) throws MojoExecutionException {
		if (distroProperties.getConfigArtifacts().isEmpty()) {
			return;
		}
		if (installDir.mkdirs()) {
			wizard.showMessage("Created configuration directory");
		}

		wizard.showMessage("Downloading Configuration...\n");

		List<Artifact> configs = distroProperties.getConfigArtifacts();
		for (Artifact configArtifact : configs) {
			// Some config artifacts have their configuration packaged in an "openmrs_config" subfolder within the zip
			// If such a folder is found in the downloaded artifact, use it.  Otherwise, use the entire zip contents
			try (TempDirectory tempDir = TempDirectory.create(configArtifact.getArtifactId())){
				moduleInstaller.installAndUnpackModule(configArtifact, tempDir.getAbsolutePath());
				File directoryToCopy = tempDir.getFile();
				for (File f : Objects.requireNonNull(tempDir.getFile().listFiles())) {
					if (f.isDirectory() && f.getName().equals("openmrs_config")) {
						directoryToCopy = f;
					}
				}
				try {
					FileUtils.copyDirectory(directoryToCopy, installDir);
				} catch (IOException e) {
					throw new MojoExecutionException("Unable to copy config: " + directoryToCopy + "\n");
				}
			}
		}
	}
}
