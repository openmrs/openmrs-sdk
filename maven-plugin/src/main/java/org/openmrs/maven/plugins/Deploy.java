package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Deploys an artifact (OMOD, OWA, or WAR) to an SDK server instance. If run from an appropriate Maven project, will prompt
 * to deploy the project.
 */
@Mojo(name = "deploy", requiresProject = false)
public class Deploy extends AbstractServerTask {

	private static final String DEFAULT_ABORT_MESSAGE = "Deploying module '%s' aborted";

	private static final String DEFAULT_UPDATE_MESSAGE = "Module '%s' was updated to version '%s'";

	private static final String TEMPLATE_UPDATE = "Do you want to update module '%s' in version '%s' to version '%s'?";

	private static final String TEMPLATE_DOWNGRADE = "Please note that downgrades are not recommended";

	private static final String TEMPLATE_CURRENT_VERSION = "The server currently has the OpenMRS %s in version %s installed.";

	private static final String DEPLOY_MODULE_OPTION = "Module";

	private static final String DEPLOY_OWA_OPTION = "Open Web App";

	private static final String DEPLOY_DISTRO_OPTION = "Distribution";

	private static final String DEPLOY_PLATFORM_OPTION = "Platform";

	/**
	 * Artifact id of an artifact, which you want to deploy.
	 */
	@Parameter(property = "artifactId")
	private String artifactId;

	/**
	 * Group id of an artifact, which you want to deploy
	 */
	@Parameter(property = "groupId")
	private String groupId;

	/**
	 * Version of an artifact, which you want to deploy.
	 */
	@Parameter(property = "version")
	private String version;

	/**
	 * OpenMRS Distribution to set up in the format 'groupId:artifactId:version'.
	 * You can skip groupId, if it is 'org.openmrs.distro'. You can also give Path to distro.properties file
	 */
	@Parameter(property = "distro")
	private String distro;

	/**
	 * OpenMRS platform version.
	 */
	@Parameter(property = "platform")
	private String platform;

	/**
	 * Owa property in the format 'owa-name:version'
	 */
	@Parameter(property = "owa")
	private String owa;

	@Parameter(property = "ignorePeerDependencies", defaultValue = "true")
	private boolean ignorePeerDependencies;

	@Parameter(property = "reuseNodeCache")
	public Boolean overrideReuseNodeCache;

	public Deploy() {
	}

	public Deploy(AbstractTask other) {
		super(other);
	}

	public Deploy(MavenProject project, MavenSession session, BuildPluginManager manager) {
		super.mavenProject = project;
		super.mavenSession = session;
		super.pluginManager = manager;
	}

	public void executeTask() throws MojoExecutionException, MojoFailureException {
		Server server = getServer();
		/*
		 * workflow:
		 * -if user specified both distro and platform, ignore them and enter interactive mode
		 * -if user specified distro or platform, always upgrade distro/platform
		 * -if user don't specify any:
		 * -- if in module project dir/ core dir, install it
		 * -- if in directory containing distro-properties file, use it to upgrade
		 * -- if those conditions are not met or user doesn't agree, start interactive mode
		 */
		ServerUpgrader serverUpgrader = new ServerUpgrader(this);

		if ((platform == null && distro == null && owa == null) && artifactId == null) {
			Artifact artifact = checkCurrentDirectoryForOpenmrsWebappUpdate(server);
			DistroProperties distroProperties = checkCurrentDirectoryForDistroProperties();
			if (artifact != null) {
				deployOpenmrsFromDir(server, artifact);
			} else if (distroProperties != null) {
				serverUpgrader.upgradeToDistro(server, distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
			} else if (checkCurrentDirForModuleProject()) {
				deployModule(groupId, artifactId, version, server);
			} else {
				runInteractiveMode(server, serverUpgrader);
			}
		} else if (distro != null) {
			DistroProperties distroProperties = distroHelper
					.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
			serverUpgrader.upgradeToDistro(server, distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
		} else if (platform != null) {
			deployOpenmrs(server, platform);
		} else if (owa != null) {
			String[] owaComponents = owa.split(":");
			if (owaComponents.length != 2) {
				throw new MojoExecutionException(
						"Could not understand owa property " + owa + ". This should be in the format owa-name:version");
			}
			deployOwa(server, owaComponents[0], owaComponents[1]);
		} else {
			deployModule(server, groupId, artifactId, version);
		}
	}

	private void runInteractiveMode(Server server, ServerUpgrader upgrader)
			throws MojoExecutionException, MojoFailureException {
		DistroProperties distroProperties;
		List<String> options = new ArrayList<>(Arrays.asList(
				DEPLOY_MODULE_OPTION,
				DEPLOY_OWA_OPTION,
				DEPLOY_DISTRO_OPTION,
				DEPLOY_PLATFORM_OPTION
		));
		String choice = wizard.promptForMissingValueWithOptions("What would you like to deploy?%s", null, "", options);

		switch (choice) {
			case (DEPLOY_MODULE_OPTION): {
				deployModule(server, groupId, artifactId, version);
				break;
			}
			case (DEPLOY_DISTRO_OPTION): {
				wizard.showMessage(String.format(
						TEMPLATE_CURRENT_VERSION,
						server.getName(),
						server.getVersion()));

				if (server.getName().equals("Platform") || server.getDistroGroupId() == null
						|| server.getDistroArtifactId() == null) {
					// If its impossible to define distro, prompt refapp distro versions
					distro = wizard.promptForRefAppVersion(versionsHelper);
				} else {
					// If its possible to define distro, prompt that distro's versions
					distro = wizard.promptForDistroVersion(server.getDistroGroupId(), server.getDistroArtifactId(),
							server.getVersion(), server.getName(), versionsHelper);
				}
				distroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
				upgrader.upgradeToDistro(server, distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
				break;
			}
			case (DEPLOY_PLATFORM_OPTION): {
				wizard.showMessage(String.format(
						TEMPLATE_CURRENT_VERSION,
						"platform",
						server.getPlatformVersion()));
				Artifact webapp = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID,
						SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_DISTRO);
				platform = wizard.promptForPlatformVersion(versionsHelper.getSuggestedVersions(webapp, 3));
				deployOpenmrs(server, platform);
				break;
			}
			case (DEPLOY_OWA_OPTION): {
				deployOwa(server, null, null);
				break;
			}
			default: {
				throw new MojoExecutionException(
						String.format("Invalid installation option only '%s', '%s', '%s' are available",
								DEPLOY_MODULE_OPTION, DEPLOY_DISTRO_OPTION, DEPLOY_PLATFORM_OPTION));
			}
		}
	}

	private void deployOwa(Server server, String name, String version) throws MojoExecutionException {
		if (name == null) {
			name = wizard.promptForValueIfMissingWithDefault("Which OWA would you like to deploy?%s", "", "", "");
		}

		if (version == null) {
			Artifact artifact = new Artifact();
			artifact.setGroupId(Artifact.GROUP_OWA);
			artifact.setArtifactId(name);
			artifact.setType(Artifact.TYPE_ZIP);
			List<String> versions = versionsHelper.getSuggestedVersions(artifact, 6);
			version = wizard
					.promptForMissingValueWithOptions("Which version would you like to deploy?%s", null, "", versions,
							"Please specify OWA version", null);
		}

		boolean installOwaModule = true;
		List<Artifact> serverModules = server.getServerModules();
		Artifact owaModule = new Artifact("owa-omod", "1.0.0");
		for (Artifact module : serverModules) {
			if (owaModule.getArtifactId().equals(module.getArtifactId())) {
				installOwaModule = false;
				break;
			}
		}

		if (installOwaModule) {
			wizard.showMessage("No installation of OWA module found on this server, will install latest version");
			owaModule.setVersion(versionsHelper.getLatestReleasedVersion(owaModule));
			deployModule(
					owaModule.getGroupId(),
					owaModule.getArtifactId(),
					owaModule.getVersion(),
					server
			);
		}

		File owaDir = new File(server.getServerDirectory(), "owa");
		if (!owaDir.exists()) {
			// OWA module has option to set custom app folder
			boolean useDefaultDir = wizard.promptYesNo(String.format(
					"\nThere is no default directory '%s' on server %s, would you like to create it? (if not, you will be asked for path to custom directory)",
					Server.OWA_DIRECTORY,
					server.getServerId()));
			if (useDefaultDir) {
				owaDir.mkdir();
			} else {
				String path = wizard.promptForValueIfMissing(null, "owa directory path");
				owaDir = new File(path);
			}
		}

		deployOwa(owaDir, name, version);
		server.saveUserOWA(name, version);
		server.save();
		getLog().info(
				String.format("OWA %s %s was successfully deployed on server %s", name, version, server.getServerId()));
	}

	public void deployOwa(File owaDir, String name, String version) throws MojoExecutionException {
		Artifact artifact = new Artifact(name, version, "org.openmrs.owa", Artifact.TYPE_ZIP);
		owaHelper.downloadOwa(owaDir, artifact, moduleInstaller);
	}

	/**
	 * Install module to selected server
	 *
	 * @param server
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @throws MojoExecutionException
	 */
	public void deployModule(Server server, String groupId, String artifactId, String version)
			throws MojoExecutionException {
		groupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", Artifact.GROUP_MODULE);
		artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
		deployModule(groupId, artifactId, version, server);
	}

	public void deployOpenmrs(Server server, String version) throws MojoFailureException, MojoExecutionException {
		Artifact artifact = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID, version, Artifact.GROUP_DISTRO);
		try {
			deployOpenmrsPlatform(server, artifact);
		}
		catch (MojoExecutionException e) {
			ServerUpgrader serverUpgrader = new ServerUpgrader(this);
			serverUpgrader.upgradePlatform(server, platform);
		}
	}

	public void deployOpenmrsFromDir(Server server, Artifact artifact) throws MojoExecutionException, MojoFailureException {
		String artifactId = artifact.getArtifactId();
		if (artifactId.equals("openmrs-webapp")) {
			deployOpenmrsWar(server, artifact);
		} else if (artifactId.equals("openmrs-distro-platform")) {
			artifact.setArtifactId("platform");
			deployOpenmrsPlatform(server, artifact);
		}
	}

	private void deployOpenmrsPlatform(Server server, Artifact artifact)
			throws MojoExecutionException, MojoFailureException {
		DistroProperties platformDistroProperties = distroHelper
				.downloadDistroProperties(server.getServerDirectory(), artifact);
		DistroProperties serverDistroProperties = server.getDistroProperties();

		List<Artifact> warArtifacts = platformDistroProperties.getWarArtifacts(distroHelper, server.getServerDirectory());
		List<Artifact> moduleArtifacts = platformDistroProperties
				.getModuleArtifacts(distroHelper, server.getServerDirectory());

		serverDistroProperties.setArtifacts(warArtifacts, moduleArtifacts);
		serverDistroProperties.saveTo(server.getServerDirectory());
		ServerUpgrader serverUpgrader = new ServerUpgrader(this);
		serverUpgrader.upgradeToDistro(server, serverDistroProperties, ignorePeerDependencies, overrideReuseNodeCache);
	}

	/**
	 * Deploy openmrs.war file to server
	 *
	 * @param server
	 * @param artifact
	 * @return tru if success
	 * @throws MojoExecutionException
	 */
	public void deployOpenmrsWar(Server server, Artifact artifact) throws MojoExecutionException {
		File openmrsCorePath = new File(server.getServerDirectory(), "openmrs-" + server.getPlatformVersion() + ".war");
		openmrsCorePath.delete();
		server.deleteServerTmpDirectory();

		List<Element> artifactItems = new ArrayList<>();
		artifactItems.add(artifact.toElement(server.getServerDirectory().getPath()));

		executeMojoPlugin(artifactItems);

		server.setPlatformVersion(mavenProject.getVersion());
		server.saveAndSynchronizeDistro();
		getLog().info("OpenMRS war has been successfully deployed");
	}

	/**
	 * Deploy Module to server
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param server
	 * @throws MojoExecutionException
	 */
	public void deployModule(String groupId, String artifactId, String version, Server server)
			throws MojoExecutionException {
		List<Element> artifactItems = new ArrayList<>();
		Artifact artifact = getModuleArtifactForSelectedParameters(groupId, artifactId, version);

		File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
		modules.mkdirs();
		artifactItems.add(artifact.toElement(modules.getPath()));

		boolean moduleRemoved = deleteModuleFromServer(artifact, modules, server);

		if (moduleRemoved) {
			executeMojoPlugin(artifactItems);

			server.setModuleProperties(artifact);
			server.saveAndSynchronizeDistro();
			getLog().info(String.format(DEFAULT_UPDATE_MESSAGE, artifact.getArtifactId(), artifact.getVersion()));
		} else
			getLog().info(String.format(DEFAULT_ABORT_MESSAGE, artifact.getArtifactId()));
	}

	/**
	 * Install modules form artifactItems
	 *
	 * @param artifactItems
	 * @throws MojoExecutionException
	 */
	private void executeMojoPlugin(List<Element> artifactItems) throws MojoExecutionException {
		executeMojo(
				plugin(
						groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
				),
				goal("copy"),
				configuration(
						element(name("artifactItems"), artifactItems.toArray(new Element[0]))
				),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}

	/**
	 * Deletes old module from the server after updating
	 *
	 * @param artifact
	 * @param serverModules
	 * @param server
	 * @return true if module has been removed or module does not exist
	 * @throws MojoExecutionException
	 */
	private boolean deleteModuleFromServer(Artifact artifact, File serverModules, Server server)
			throws MojoExecutionException {
		File[] listOfModules = serverModules.listFiles();
		String moduleId = StringUtils.removeEnd(artifact.getArtifactId(), "-omod");
		for (File itemModule : listOfModules) {
			String[] parts = itemModule.getName().split("-");
			String oldV = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "-");
			if (moduleId.equals(parts[0])) {
				Version oldVersion = new Version(oldV.substring(0, oldV.lastIndexOf('.')));
				Version newVersion = new Version(artifact.getVersion());

				if (!oldVersion.equals(newVersion)) {
					if (oldVersion.higher(newVersion)) {
						wizard.showMessage(TEMPLATE_DOWNGRADE);
					}

					boolean agree = wizard
							.promptYesNo(String.format(TEMPLATE_UPDATE, moduleId, oldVersion, artifact.getVersion()));
					if (!agree) {
						return false;
					}
				}

				server.removeModuleProperties(new Artifact(moduleId, oldVersion.toString(), artifact.getGroupId()));
				server.saveAndSynchronizeDistro();
				return itemModule.delete();
			}
		}
		return true;
	}

	/**
	 * Check if the command openmrs-sdk:install was invoked from the openmrs-core directory and then check the version
	 *
	 * @param server
	 * @return artifact to update, if update requested or null
	 */
	public Artifact checkCurrentDirectoryForOpenmrsWebappUpdate(Server server) throws MojoExecutionException {
		String moduleName = mavenProject.getArtifactId();
		if (moduleName.equals("openmrs")) {
			if (!new Version(mavenProject.getVersion()).equals(new Version(server.getPlatformVersion())) || new Version(
					mavenProject.getVersion()).isSnapshot()) {
				String message = String
						.format("The server currently has openmrs.war in version %s. Would you like to update it to %s from the current directory?",
								server.getPlatformVersion(), mavenProject.getVersion());
				boolean agree = wizard.promptYesNo(message);
				if (agree) {
					return new Artifact("openmrs-webapp", mavenProject.getVersion(), Artifact.GROUP_WEB, Artifact.TYPE_WAR);
				}
			}
		} else if ("platform".equals(moduleName)) {
			if (!new Version(mavenProject.getVersion()).equals(new Version(server.getPlatformVersion())) || new Version(
					mavenProject.getVersion()).isSnapshot()) {
				String message = String
						.format("The server currently has openmrs platform in version %s. Would you like to update it to %s from the current directory?",
								server.getPlatformVersion(), mavenProject.getVersion());
				boolean agree = wizard.promptYesNo(message);
				if (agree) {
					return new Artifact("openmrs-distro-platform", mavenProject.getVersion(), Artifact.GROUP_DISTRO);
				}
			}
		}
		return null;
	}

	private DistroProperties checkCurrentDirectoryForDistroProperties() throws MojoExecutionException {
		DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromDir();
		if (distroProperties != null) {
			String message = String.format(
					"Would you like to deploy %s %s from the current directory?",
					distroProperties.getName(),
					distroProperties.getVersion());

			boolean agree = wizard.promptYesNo(message);
			if (agree) {
				return distroProperties;
			}
		}

		return null;
	}

	/**
	 * Get attribute values and prompt if not selected
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	public Artifact getModuleArtifactForSelectedParameters(String groupId, String artifactId, String version)
			throws MojoExecutionException {
		String moduleGroupId;
		String moduleArtifactId;
		String moduleVersion;

		File userDir = new File(System.getProperty("user.dir"));
		Project project = null;
		if (Project.hasProject(userDir)) {
			project = Project.loadProject(userDir);
		}
		if (artifactId == null && project != null && project.isOpenmrsModule()) {
			if (project.getParent() != null && !"org.openmrs.maven.parents".equals(project.getParent().getGroupId())) {
				moduleGroupId = project.getParent().getGroupId();
				moduleArtifactId = project.getParent().getArtifactId() + "-omod";
				moduleVersion = (version != null) ? version : project.getParent().getVersion();
			} else {
				moduleGroupId = project.getGroupId();
				moduleArtifactId = project.getArtifactId() + "-omod";
				moduleVersion = (version != null) ? version : project.getVersion();
			}
		} else {
			moduleGroupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", Artifact.GROUP_MODULE);
			moduleArtifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
			if (!moduleArtifactId.endsWith("-omod")) {
				moduleArtifactId += "-omod";
			}
			List<String> availableVersions = versionsHelper
					.getSuggestedVersions(new Artifact(moduleArtifactId, "1.0", moduleGroupId), 5);
			moduleVersion = wizard.promptForMissingValueWithOptions(
					"You can deploy the following versions of the module", version, "version", availableVersions,
					"Please specify module version", null);
		}
		return new Artifact(moduleArtifactId, moduleVersion, moduleGroupId, Artifact.TYPE_JAR, Artifact.TYPE_OMOD);
	}

	public boolean checkCurrentDirForModuleProject() throws MojoExecutionException {
		File dir = new File(System.getProperty("user.dir"));
		Project project = null;
		if (Project.hasProject(dir)) {
			project = Project.loadProject(dir);
		}
		boolean hasProject = (project != null && project.isOpenmrsModule());
		if (hasProject) {
			hasProject = wizard.promptYesNo(String.format("Would you like to deploy %s %s from the current directory?",
					project.getArtifactId(), project.getVersion()));
		}
		return hasProject;
	}

    @Override
    protected Server loadServer() throws MojoExecutionException {
        return loadValidatedServer(serverId);
    }
}
