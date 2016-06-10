package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ServerUpgrader {
    private static final String OLD_PROPERTIES_FILENAME = "old.properties";
	private final static String[] SUPPORTED = new String[]{Artifact.TYPE_WAR, Artifact.TYPE_JAR, Artifact.TYPE_OMOD};
    private AbstractTask parentTask;


	public ServerUpgrader(AbstractTask parentTask) {
        this.parentTask = parentTask;
    }

    /**
     * Upgrades platform of given server
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void upgradePlatform(Server server, String version) throws MojoExecutionException, MojoFailureException {
        saveBackupProperties(server);
		confirmUpgrade(server.getPlatformVersion(), version);
		replaceWebapp(server, version);
	    deleteBackupProperties(server);
        parentTask.getLog().info(String.format("Server %s has been successfully upgraded to %s", server.getServerId(), version));
    }

	public void upgradeToDistro(Server server) throws MojoExecutionException, MojoFailureException {
		Artifact distro = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId());
		upgradeToDistro(server, distro);
	}

	/**
	 * @param server server to be upgraded
	 * @param distro
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
    public void upgradeToDistro(Server server, Artifact distro) throws MojoExecutionException, MojoFailureException {
	    if(distro.getArtifactId().equals(server.getDistroArtifactId())){
			confirmUpgrade(server.getVersion(), distro.getVersion());
		}
		Setup setup = new Setup(parentTask);
	    saveBackupProperties(server);
	    //delete webapp and modules of existing server version, leave out modules installed by user
	    List<Artifact> userModules = parseUserModules(server);
	    if(server.getDbDriver().equals(SDKConstants.DRIVER_H2)){
		    userModules.add(SDKConstants.H2_ARTIFACT);
	    }
	    File modulesFolder = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
	    deleteNonUserModulesFromDir(userModules, server.getServerDirectory());
	    deleteNonUserModulesFromDir(userModules, modulesFolder);
	    //delete installation.properties file on server to avoid Setup task errors with already existing server
	    server.delete();

		server.setVersion(distro.getVersion());
		server.setDistroGroupId(distro.getGroupId());
		server.setDistroArtifactId(distro.getArtifactId());

	    setup.setup(server, false, true, null);
	    deleteBackupProperties(server);
	    deleteDependencyPluginMarker();
    }

	private void deleteNonUserModulesFromDir(List<Artifact> userModules, File dir) {
		File[] files = dir.listFiles();
		if(files != null){
			for (File f: files) {
				String type = f.getName().substring(f.getName().lastIndexOf(".") + 1);
				if (isSupported(type)) {
					boolean toDelete = true;
					String id = getId(f.getName());
					for(Artifact userModule : userModules){
						if(userModule.getArtifactId().equals(id)){
							toDelete = false;
							break;
						}
					}
					if(toDelete){
						f.delete();
					}
				}
			}
		}
	}

	private boolean isSupported(String type) {
		return type.equals(SUPPORTED[0]) || type.equals(SUPPORTED[1]) || type.equals(SUPPORTED[2]);
	}

	/**
	 * saves current properties in backup file to make restoring server available
	 * in case of error occurence during upgrading
	 *
	 * @param server
	 * @throws MojoExecutionException
	 */
    private void saveBackupProperties(Server server) throws MojoExecutionException {
        File backupProperties = new File(server.getServerDirectory(), OLD_PROPERTIES_FILENAME);
        server.saveTo(backupProperties);
    }
    private void deleteBackupProperties(Server server) throws MojoExecutionException {
        File backupProperties = new File(server.getServerDirectory(), OLD_PROPERTIES_FILENAME);
        backupProperties.delete();
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
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void replaceWebapp(Server server, String version) throws MojoFailureException, MojoExecutionException {
        File webapp = new File(server.getServerDirectory(), "openmrs-"+server.getPlatformVersion()+".war");
        webapp.delete();
        Artifact webappArtifact = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, version, Artifact.GROUP_WEB, Artifact.TYPE_WAR);
        parentTask.moduleInstaller.installModule(webappArtifact, server.getServerDirectory().getPath());
        server.setPlatformVersion(version);
        server.save();
    }
	/**
	 * Get artifact id from module name (without -omod, etc)
	 * @param name of file which id will be obtained
	 * @return
	 */
	public String getId(String name) {
		int index = name.indexOf('-');
		if (index == -1) return name;
		return name.substring(0, index);
	}

	/**
	 * @param server
	 * @return list of modules installed by user on given server
	 * @throws MojoExecutionException
	 */
	public List<Artifact> parseUserModules(Server server) throws MojoExecutionException {
		String values = server.getParam(Server.PROPERTY_USER_MODULES);
		List<Artifact> result = new ArrayList<>();
		if (values != null) {
			String[] modules = values.split(Server.COMMA);
			for (String mod: modules) {
				String[] params = mod.split(Server.SLASH);
				// check
				if (params.length == 3) {
					result.add(new Artifact(params[1], params[2], params[0]));
				}
				else throw new MojoExecutionException("Properties file parse error - cannot read user modules list");
			}
		}
		return result;
	}
}
