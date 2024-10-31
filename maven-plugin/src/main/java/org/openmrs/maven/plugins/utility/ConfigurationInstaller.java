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
import java.util.UUID;

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
		if (directory.exists()) {
			throw new MojoExecutionException("Server configuration directory already exists");
		}
		File tempDir = new File(server.getServerTmpDirectory(), UUID.randomUUID().toString());
		installToDirectory(directory, tempDir, distroProperties);
	}

	public void installToDirectory(File installDir, DistroProperties distroProperties) throws MojoExecutionException {
		File tempDir = new File(installDir.getParentFile(), UUID.randomUUID().toString());
		installToDirectory(installDir, tempDir, distroProperties);
	}

	protected void installToDirectory(File installDir, File tempDir, DistroProperties distroProperties) throws MojoExecutionException {
		if (distroProperties.getConfigArtifacts().isEmpty()) {
			return;
		}
		if (installDir.exists()) {
			throw new MojoExecutionException("Server configuration directory already exists");
		}
		if (!installDir.mkdirs()) {
			throw new MojoExecutionException("Unable to create directory: " + installDir);
		}

		wizard.showMessage("Downloading Configuration...\n");

		List<Artifact> configs = distroProperties.getConfigArtifacts();
		for (Artifact configArtifact : configs) {
			// Some config artifacts have their configuration packaged in an "openmrs_config" subfolder within the zip
			// If such a folder is found in the downloaded artifact, use it.  Otherwise, use the entire zip contents
			try {
				if (!tempDir.mkdirs()) {
					throw new MojoExecutionException("Unable to create temporary directory " + tempDir + "\n");
				}
				moduleInstaller.installAndUnpackModule(configArtifact, tempDir.getAbsolutePath());
				File directoryToCopy = tempDir;
				for (File f : Objects.requireNonNull(tempDir.listFiles())) {
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
			finally {
				FileUtils.deleteQuietly(tempDir);
			}
		}
	}
}
