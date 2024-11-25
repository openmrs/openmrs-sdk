package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.StringUtils;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.BaseSdkProperties;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ServerUpgrader {

    private final AbstractTask parentTask;

	public ServerUpgrader(AbstractTask parentTask) {
        this.parentTask = parentTask;
    }

    /**
     * Upgrades platform of given server
     *
     * @throws MojoExecutionException
     */
    public void upgradePlatform(Server server, String version) throws MojoExecutionException {
        server.saveBackupProperties();
		confirmUpgrade(server.getPlatformVersion(), version);
		replaceWebapp(server, version);
	    server.deleteBackupProperties();
        parentTask.getLog().info(String.format("Server %s has been successfully upgraded to %s", server.getServerId(), version));
    }

	public void upgradeToDistro(Server server, Distribution distribution, boolean ignorePeerDependencies, Boolean overrideReuseNodeCache) throws MojoExecutionException {
		boolean serverExists = server.getPropertiesFile().exists();
		UpgradeDifferential upgradeDifferential = calculateUpdateDifferential(server, distribution);
		DistroProperties distroProperties = distribution.getEffectiveProperties();
		if (serverExists) {
			boolean confirmed = parentTask.wizard.promptForConfirmDistroUpgrade(upgradeDifferential);
			if (!confirmed) {
				parentTask.wizard.showMessage("Server upgrade aborted");
				return;
			}
		}
		server.saveBackupProperties();
		server.deleteServerTmpDirectory();

		if (distroProperties.getVersion() != null) {
			server.setVersion(distroProperties.getVersion());
		}

		// Upgrade the war
		File warFile = server.getWarFile();
		UpgradeDifferential.ArtifactChanges warChanges = upgradeDifferential.getWarChanges();
		if (warChanges.hasChanges() || !warFile.exists()) {
			if (warChanges.getNewArtifacts().isEmpty()) {
				throw new MojoExecutionException("Deleting openmrs war is not supported");
			}
			if (warChanges.getNewArtifacts().size() > 1) {
				throw new MojoExecutionException("Only one openmrs war can be configured in a distribution");
			}
			replaceWebapp(server, warChanges.getNewArtifacts().get(0).getVersion());
		}

		// Upgrade modules
		UpgradeDifferential.ArtifactChanges moduleChanges = upgradeDifferential.getModuleChanges();
		if (moduleChanges.hasChanges()) {
			File modulesDir = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
			for (Artifact artifact : moduleChanges.getArtifactsToRemove()) {
				File moduleToDelete = new File(modulesDir, artifact.getDestFileName());
				if (moduleToDelete.delete()) {
					parentTask.wizard.showMessage("Removed module: " + moduleToDelete.getAbsolutePath());
				}
				server.removeModuleProperties(artifact);
			}
			parentTask.moduleInstaller.installModules(moduleChanges.getArtifactsToAdd(), modulesDir.getAbsolutePath());
			for (Artifact artifact : moduleChanges.getArtifactsToAdd()) {
				server.setModuleProperties(artifact);
			}
		}

		// Upgrade owas
		UpgradeDifferential.ArtifactChanges owaChanges = upgradeDifferential.getOwaChanges();
		if (owaChanges.hasChanges()) {
			File owaDir = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_OWA);
			if (owaDir.mkdir()) {
				parentTask.wizard.showMessage("Created directory: " + owaDir.getName());
			}
			for (Artifact artifact : owaChanges.getArtifactsToRemove()) {
				File owaFile = new File(owaDir, artifact.getDestFileName());
				if (owaFile.delete()) {
					parentTask.wizard.showMessage("Removed owa file: " + owaFile.getName());
				}
				File owaExpandedDir = new File(owaDir, artifact.getArtifactId());
				if (owaExpandedDir.exists()) {
					try {
						FileUtils.deleteDirectory(owaExpandedDir);
						parentTask.wizard.showMessage("Removed owa dir: " + owaExpandedDir);
					} catch (IOException e) {
						throw new MojoExecutionException("Failed to delete directory: " + owaExpandedDir.getAbsolutePath(), e);
					}
				}
				server.removePropertiesForArtifact(BaseSdkProperties.TYPE_OWA, artifact);
			}
			for (Artifact artifact : owaChanges.getArtifactsToAdd()) {
				parentTask.wizard.showMessage("Installing OWA: " + artifact.getArtifactId());
				parentTask.owaHelper.downloadOwa(owaDir, artifact, parentTask.moduleInstaller);
				server.addPropertiesForArtifact(BaseSdkProperties.TYPE_OWA, artifact);
			}
		}

		// Upgrade spa
		UpgradeDifferential.ArtifactChanges spaArtifactChanges = upgradeDifferential.getSpaArtifactChanges();
		UpgradeDifferential.PropertyChanges spaBuildChanges = upgradeDifferential.getSpaBuildChanges();
		boolean updateSpa = spaArtifactChanges.hasChanges() || spaBuildChanges.hasChanges();
		if (updateSpa) {
			parentTask.spaInstaller.installFromDistroProperties(server.getServerDirectory(), distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
		}

		// Upgrade config and content
		UpgradeDifferential.ArtifactChanges configChanges = upgradeDifferential.getConfigChanges();
		UpgradeDifferential.ArtifactChanges contentChanges = upgradeDifferential.getContentChanges();

		if (configChanges.hasChanges() || contentChanges.hasChanges()) {

			// TODO: This should happen in a higher-level validation stage and this method needs refactoring
			parentTask.distroHelper.parseContentProperties(distroProperties);

			File configDir = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_CONFIGURATION);
			if (configDir.exists()) {
				parentTask.wizard.showMessage("Removing existing configuration and content packages");
				try {
					FileUtils.deleteDirectory(configDir);
				}
				catch (IOException e) {
					throw new MojoExecutionException("Unable to delete existing configuration directory", e);
				}
			}

			if (configChanges.hasChanges()) {
				parentTask.configurationInstaller.installToServer(server, distroProperties);
			}

			if (contentChanges.hasChanges()) {
				ContentHelper.downloadAndMoveContentBackendConfig(server.getServerDirectory(), distroProperties, parentTask.moduleInstaller, parentTask.wizard);
				// TODO: Where is the frontend config installation?
			}

		}

		server.setVersion(distroProperties.getVersion());
		server.setName(distroProperties.getName());
		if (server.getDistroPropertiesFile().delete()) {
			parentTask.wizard.showMessage("Removed old distro properties file, and saving new one");
		}
		distroProperties.saveTo(server.getServerDirectory());
		server.deleteBackupProperties();
		deleteDependencyPluginMarker();
		server.saveAndSynchronizeDistro();
		parentTask.getLog().info("Server upgraded successfully");
	}

	/**
	 * should:
	 * - ignore modules which are already on server, but not included in distro properties of upgrade
	 * - keep new platform artifact if distro properties declares newer version
	 * - updateMap include modules which are already on server with newer/equal SNAPSHOT version
	 * - add modules which are not installed on server yet
	 */
	public UpgradeDifferential calculateUpdateDifferential(Server server, Distribution distribution) {

		UpgradeDifferential upgradeDifferential = new UpgradeDifferential(server, distribution);
		DistroProperties distroProperties = distribution.getEffectiveProperties();

		// War File
		List<Artifact> oldWars = server.getWarArtifacts();
		List<Artifact> newWars = distroProperties.getWarArtifacts();
		upgradeDifferential.setWarChanges(new UpgradeDifferential.ArtifactChanges(oldWars, newWars));

		// Modules
		List<Artifact> oldModules = server.getModuleArtifacts();
		List<Artifact> newModules = distroProperties.getModuleArtifacts();
		upgradeDifferential.setModuleChanges(new UpgradeDifferential.ArtifactChanges(oldModules, newModules));

		// Owas
		List<Artifact> oldOwas = server.getOwaArtifacts();
		List<Artifact> newOwas = distroProperties.getOwaArtifacts();
		upgradeDifferential.setOwaChanges(new UpgradeDifferential.ArtifactChanges(oldOwas, newOwas));

		// Spa
		List<Artifact> oldSpa = server.getSpaArtifacts();
		List<Artifact> newSpa = distroProperties.getSpaArtifacts();
		upgradeDifferential.setSpaArtifactChanges(new UpgradeDifferential.ArtifactChanges(oldSpa, newSpa));

		Map<String, String> oldSpaProps = server.getSpaBuildProperties();
		Map<String, String> newSpaProps = distroProperties.getSpaBuildProperties();
		upgradeDifferential.setSpaBuildChanges(new UpgradeDifferential.PropertyChanges(oldSpaProps, newSpaProps));

		// Config
		List<Artifact> oldConfig = server.getConfigArtifacts();
		List<Artifact> newConfig = distroProperties.getConfigArtifacts();
		upgradeDifferential.setConfigChanges(new UpgradeDifferential.ArtifactChanges(oldConfig, newConfig));

		// Content
		List<Artifact> oldContent = server.getContentArtifacts();
		List<Artifact> newContent = distroProperties.getContentArtifacts();
		upgradeDifferential.setContentChanges(new UpgradeDifferential.ArtifactChanges(oldContent, newContent));

		return upgradeDifferential;
	}

	/**
	 * Maven dependency plugin leaves directory with marker in directory from which it was executed
	 * This method deletes it to clean up after upgrade
	 * @throws MojoExecutionException
	 */
	private void deleteDependencyPluginMarker() throws MojoExecutionException {
		File tmp = new File(System.getProperty("user.dir"), "${project.basedir}");
		if (tmp.exists()) try {
			FileUtils.deleteDirectory(tmp);
		} catch (IOException e) {
			throw new MojoExecutionException("Error during clean: " + e.getMessage());
		}
	}

	/**
	 * make sure that user is aware that he is downgrading server if target version is older
	 * @param prevVersion
	 * @param nextVersion
	 * @throws MojoExecutionException
	 */
    private void confirmUpgrade(String prevVersion, String nextVersion) throws MojoExecutionException {
        Version prev = new Version(prevVersion);
        Version next = new Version(nextVersion);
        if (prev.higher(next)){
            boolean confirmed =parentTask.wizard.promptYesNo("Note that downgrades are generally not supported by OpenMRS. " +
                    "Please consider setting up a new server with the given version instead.\n" +
                    "Are you sure you would like to downgrade?");
            if(!confirmed) throw new MojoExecutionException("Installation has been aborted");
        }
    }

    /**
     * @param server server to upgrade
     * @throws MojoExecutionException
     */
    private void replaceWebapp(Server server, String version) throws MojoExecutionException {
        File webapp = server.getWarFile();
		if (webapp.delete()) {
			parentTask.wizard.showMessage("Replacing existing war: " + webapp.getName());
		}
        Artifact webappArtifact = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, version, Artifact.GROUP_WEB, Artifact.TYPE_WAR);
        parentTask.moduleInstaller.installModule(webappArtifact, server.getServerDirectory().getPath());
        server.setPlatformVersion(version);
		server.saveAndSynchronizeDistro();
    }

	public void validateServerMetadata(Path serverPath) throws MojoExecutionException {
		File serverProperties = serverPath.resolve(SDKConstants.OPENMRS_SERVER_PROPERTIES).toFile();
		File installationProperties = serverPath.resolve("installation.properties").toFile();

		if (installationProperties.exists()){
			if(serverProperties.exists()){
				FileUtils.deleteQuietly(installationProperties);
			} else {
				installationProperties.renameTo(serverProperties);
			}
		}

		Server server;
		if(serverProperties.exists()){
			server = Server.loadServer(serverPath);
			String webappVersion = server.getWebappVersionFromFilesystem();
			if(!webappVersion.equals(server.getPlatformVersion())){
				String version = server.getVersion();
				String platformVersion = server.getPlatformVersion();
				if(StringUtils.isNotBlank(platformVersion)){
					//case: distribution:
					if(version!=null){
						if(version.equals(server.getWebappVersionFromFilesystem())){
							server.setPlatformVersion(version);
							server.setVersion(platformVersion);
						} else {
							throw new MojoExecutionException("Invalid versions configured in openmrs-server.properties file");
						}
					}
					//case:platform
					else {
						server.setVersion(platformVersion);
					}
				} else {
					server.setPlatformVersion(server.getWebappVersionFromFilesystem());
				}
			}
			if(StringUtils.isNotBlank(server.getParam(Server.PROPERTY_VERSION))){
				if(server.getParam(Server.PROPERTY_VERSION).equals("2.3")){
					this.parentTask.wizard.showMessage("Please note that Reference Application 2.3 is not supported" +
							"\nFunctions other than 'run' will not work properly, " +
							"\nIt is recommended to use version 2.3.1 instead" +
							"\nYou can easily upgrade this server calling 'mvn openmrs-sdk:deploy -DserverId="+server.getServerId() +
							" -Ddistro=referenceapplication:2.3.1'");
				}
				configureMissingDistroArtifact(server);
				if(!serverPath.resolve(DistroProperties.DISTRO_FILE_NAME).toFile().exists()){
					String distro = server.getDistroArtifactId() + ":" + server.getParam(Server.PROPERTY_VERSION);
					parentTask.distroHelper.saveDistroPropertiesTo(serverPath.toAbsolutePath().toFile(), distro);
				}
			} else if(StringUtils.isBlank(server.getVersion())){
				DistroProperties distroProperties = new DistroProperties(server.getServerId(), server.getOpenmrsCoreVersion());
				distroProperties.saveTo(serverPath.toAbsolutePath().toFile());
			}
			if(StringUtils.isNotBlank(server.getParam(Server.PROPERTY_PLATFORM))){
				server.setValuesFromDistroProperties(server.getDistroProperties());
				updateModulesPropertiesWithUserModules(server);
				server.removePlatformVersionProperty();
				server.removeUserModulesProperty();
				server.removeOpenmrsVersionProperty();
				server.save();
			}
		} else throw new MojoExecutionException("There is no server properties file in this directory");

		server.save();
	}

	private void updateModulesPropertiesWithUserModules(Server server) throws MojoExecutionException {
		List<Artifact> userModules = server.getUserModules();
		for(Artifact userModule: userModules){
			server.removeModuleProperties(userModule);
			server.setModuleProperties(userModule);
		}
		server.saveAndSynchronizeDistro();
	}

	private void configureMissingDistroArtifact(Server server) {
		if(server.getDistroArtifactId() == null){
            server.setDistroArtifactId(SDKConstants.REFAPP_2X_ARTIFACT_ID);
        }
		if(server.getDistroGroupId() == null){
            server.setDistroGroupId(Artifact.GROUP_DISTRO);
        }
	}
}
