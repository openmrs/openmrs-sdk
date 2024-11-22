package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.StringUtils;
import org.openmrs.maven.plugins.model.*;
import org.openmrs.maven.plugins.utility.DistroHelper;
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

	public void upgradeToDistro(Server server, DistroProperties distroProperties, boolean ignorePeerDependencies, Boolean overrideReuseNodeCache) throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(server, distroProperties);
        boolean confirmed = parentTask.wizard.promptForConfirmDistroUpgrade(upgradeDifferential, server, distroProperties);
		if(confirmed){
			server.saveBackupProperties();

			String modulesDir = server.getServerDirectory().getPath()+File.separator+SDKConstants.OPENMRS_SERVER_MODULES;
			if(upgradeDifferential.getPlatformArtifact()!=null){
				server.deleteServerTmpDirectory();
				replaceWebapp(server, upgradeDifferential.getPlatformArtifact().getVersion());
			}
			if(!upgradeDifferential.getModulesToAdd().isEmpty()){
				parentTask.moduleInstaller.installModules(upgradeDifferential.getModulesToAdd(), modulesDir);
				for(Artifact artifact: upgradeDifferential.getModulesToAdd()){
					server.setModuleProperties(artifact);
				}
			}
			if(!upgradeDifferential.getModulesToDelete().isEmpty()){
				for(Artifact artifact: upgradeDifferential.getModulesToDelete()){
					File moduleToDelete = new File(modulesDir, artifact.getDestFileName());
					moduleToDelete.delete();
					server.removeModuleProperties(artifact);
				}
			}
			if(!upgradeDifferential.getUpdateOldToNewMap().isEmpty()){
				for(Map.Entry<Artifact, Artifact> updateEntry : upgradeDifferential.getUpdateOldToNewMap().entrySet()){
					updateModule(server, modulesDir, updateEntry);
				}
			}
			if(!upgradeDifferential.getDowngradeNewToOldMap().isEmpty()){
				for(Map.Entry<Artifact, Artifact> downgradeEntry : upgradeDifferential.getDowngradeNewToOldMap().entrySet()){
					updateModule(server, modulesDir, downgradeEntry);
				}
			}
			parentTask.spaInstaller.installFromDistroProperties(server.getServerDirectory(), distroProperties);

			server.setVersion(distroProperties.getVersion());
			server.setName(distroProperties.getName());
			server.getDistroPropertiesFile().delete();
			distroProperties.saveTo(server.getServerDirectory());
			server.deleteBackupProperties();
			deleteDependencyPluginMarker();
			server.saveAndSynchronizeDistro();
			parentTask.getLog().info("Server upgraded successfully");
		} else {
			parentTask.wizard.showMessage("Server upgrade aborted");
		}

	}

	private void updateModule(Server server, String modulesDir, Map.Entry<Artifact, Artifact> updateEntry) throws MojoExecutionException {
		File oldModule = new File(modulesDir, updateEntry.getKey().getDestFileName());
		oldModule.delete();
		server.removeModuleProperties(updateEntry.getKey());
		parentTask.moduleInstaller.installModule(updateEntry.getValue(), modulesDir);
		server.setModuleProperties(updateEntry.getValue());
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
        File webapp = new File(server.getServerDirectory(), "openmrs-"+server.getPlatformVersion()+".war");
        webapp.delete();
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
				server.setValuesFromDistroPropertiesModules(
						server.getDistroProperties().getWarArtifacts(),
						server.getDistroProperties().getModuleArtifacts(),
						server.getDistroProperties()
				);
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
