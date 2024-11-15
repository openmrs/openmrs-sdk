package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.PackageJson;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;
import org.openmrs.maven.plugins.model.Version;
import org.semver4j.Semver;
import org.semver4j.SemverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.openmrs.maven.plugins.model.BaseSdkProperties.PROPERTY_DISTRO_ARTIFACT_ID;
import static org.openmrs.maven.plugins.model.BaseSdkProperties.PROPERTY_DISTRO_GROUP_ID;
import static org.openmrs.maven.plugins.model.BaseSdkProperties.TYPE_DISTRO;
import static org.openmrs.maven.plugins.model.BaseSdkProperties.TYPE_PARENT;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

public class DistroHelper {

	private static final String CONTENT_PROPERTIES = "content.properties";

	private static final String CONTENT_PREFIX = "content.";

	private static final Logger log = LoggerFactory.getLogger(DistroHelper.class);
	/**
	 * The project currently being build.
	 */
	final MavenProject mavenProject;

	/**
	 * The current Maven session.
	 */
	final MavenSession mavenSession;

	/**
	 * The Maven BuildPluginManager component.
	 */
	final BuildPluginManager pluginManager;

	/**
	 *
	 */
	final Wizard wizard;

	final VersionsHelper versionHelper;

	public DistroHelper(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager,
	    Wizard wizard, VersionsHelper versionHelper) {
		this.mavenProject = mavenProject;
		this.mavenSession = mavenSession;
		this.pluginManager = pluginManager;
		this.wizard = wizard;
		this.versionHelper = versionHelper;
	}

	/**
	 * @return distro properties from openmrs-distro.properties file in current directory or null if not exist
	 */
	public static DistroProperties getDistroPropertiesFromDir() {
		File distroFile = new File(new File(System.getProperty("user.dir")), "openmrs-distro.properties");
		return getDistroPropertiesFromFile(distroFile);
	}

	/**
	 * Creates a minimal distro properties for the current server configuration, which is
	 * a platform installation. The database driver must have been configured before this is
	 * called.
	 */
	public DistroProperties createDistroForPlatform(Server server) {
		DistroProperties distroProperties = new DistroProperties(server.getServerId(), server.getPlatformVersion());
		if (server.getDbDriver().equals(SDKConstants.DRIVER_H2)) {
			distroProperties.setH2Support(true);
		}
		return distroProperties;
	}

	/**
	 * @param distroFile file which contains distro properties
	 * @return distro properties loaded from specified file or null if file is not distro properties
	 */
	public static DistroProperties getDistroPropertiesFromFile(File distroFile) {
		if (distroFile.exists()) {
			try {
				return new DistroProperties(distroFile);
			}
			catch (MojoExecutionException ignored) {
			}
		}

		return null;
	}

	/**
	 * Saves all custom properties from distroProperties starting with "property." to server
	 *
	 * @param properties
	 * @param server
	 */
	public void savePropertiesToServer(DistroProperties properties, Server server) throws MojoExecutionException {
		if (properties != null) {
			Properties userBashProperties = mavenSession.getRequest().getUserProperties();
			Set<String> propertiesNames = properties.getPropertiesNames();
			for (String propertyName : propertiesNames) {
				String propertyValue = properties.getPropertyValue(propertyName);
				String propertyValueBash = userBashProperties.getProperty(propertyName);
				String propertyPrompt = properties.getPropertyPrompt(propertyName);
				String propertyDefault = properties.getPropertyDefault(propertyName);
				if (propertyValueBash != null) {
					server.setPropertyValue(propertyName, propertyValueBash);
				} else if (propertyValue != null) {
					server.setPropertyValue(propertyName, propertyValue);
				} else {
					if (propertyPrompt != null) {
						if (propertyDefault != null) {
							propertyValue = wizard
									.promptForValueIfMissingWithDefault(propertyPrompt, null, propertyName, propertyDefault);
						} else {
							propertyValue = wizard
									.promptForValueIfMissingWithDefault(propertyPrompt, null, propertyName, null);
						}
						server.setPropertyValue(propertyName, propertyValue);
					}

				}
			}
		}
	}

	/**
	 * Valid formats are 'groupId:artifactId:version' and 'artifactId:version'.
	 * Parser makes user-friendly assumptions, like inferring default groupId or full artifactId for referenceapplication.
	 * If the group ID is org.openmrs.module, the artifact ID will have '-omod' appended to it.
	 */
	public static Artifact parseDistroArtifact(String distro, VersionsHelper versionsHelper) throws MojoExecutionException {
		String[] split = distro.split(":");
		if (split.length > 3) {
			throw new MojoExecutionException("Invalid distro: " + distro);
		}
		String groupId = split.length == 3 ? split[0] : Artifact.GROUP_DISTRO;
		String artifactId = split[split.length - 2];
		String version = split[split.length - 1];
		return normalizeArtifact(new Artifact(artifactId, version, groupId), versionsHelper);
	}

	public static Artifact normalizeArtifact(Artifact artifact, VersionsHelper versionsHelper) {
		if (artifact == null) {
			return null;
		}
		String artifactId = artifact.getArtifactId();
		String groupId = artifact.getGroupId();
		String version = artifact.getVersion();
		String type = artifact.getType();

		if (Artifact.GROUP_DISTRO.equals(groupId)) {
			if ("referenceapplication".equals(artifactId)) {
				Version v = new Version(version);
				if (v.getMajorVersion() <= 2) {
					artifactId += "-package";
					type = "jar";
				}
				else {
					artifactId += "-distro";
					type = "zip";
				}
			}
		}

		if (Artifact.GROUP_MODULE.equals(groupId) && !artifactId.endsWith("-omod")) {
			artifactId += "-omod";
			type = "jar";
		}

		if (versionsHelper != null) {
			if (version.contains(SDKConstants.LATEST_VERSION_BATCH_KEYWORD)) {
				if (version.equalsIgnoreCase(SDKConstants.LATEST_SNAPSHOT_BATCH_KEYWORD)) {
					version = versionsHelper.getLatestSnapshotVersion(new Artifact(artifactId, version, groupId, type));
				}
				else if (version.equalsIgnoreCase((SDKConstants.LATEST_VERSION_BATCH_KEYWORD))) {
					version = versionsHelper.getLatestReleasedVersion(new Artifact(artifactId, version, groupId, type));
				}
			}
		}

		artifact.setArtifactId(artifactId);
		artifact.setGroupId(groupId);
		artifact.setVersion(version);
		artifact.setType(type);
		return artifact;
	}

	/**
	 * openmrs-sdk has hardcoded distro properties for certain versions of refapp which don't include them
	 *
	 * @return
	 */
	public static boolean isRefapp2_3_1orLower(String artifactId, String version) {
		if (artifactId != null && artifactId.equals(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID)) {
			return SDKConstants.SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER.contains(version);
		} else
			return false;
	}

	public static boolean isRefapp2_3_1orLower(Artifact artifact) {
		return isRefapp2_3_1orLower(artifact.getArtifactId(), artifact.getVersion());
	}

	public static boolean isRefappBelow2_1(String artifactId, String version) {
		if (artifactId != null && artifactId.equals(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID)) {
			return new Version(version).lower(new Version("2.1"));
		} else
			return false;
	}

	public static boolean isRefappBelow2_1(Artifact artifact) {
		return isRefappBelow2_1(artifact.getArtifactId(), artifact.getVersion());
	}

	public File downloadDistro(File path, Artifact artifact) throws MojoExecutionException {
		return downloadDistro(path, artifact, "openmrs-distro.jar");
	}

	public File downloadDistro(File path, Artifact artifact, String fileName) throws MojoExecutionException {
		artifact.setDestFileName(fileName);
		List<MojoExecutor.Element> artifactItems = new ArrayList<>();
		MojoExecutor.Element element = artifact.toElement(path.toString());
		artifactItems.add(element);

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

		return new File(path, artifact.getDestFileName());
	}

	public File extractFileFromDistro(File path, Artifact artifact, String filename) throws MojoExecutionException {
		File distroFile = downloadDistro(path, artifact);
		File resultFile;

		try (ZipFile zipFile = new ZipFile(distroFile)) {
			resultFile = File.createTempFile(filename, ".tmp");

			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.getName().equals(filename)) {
					FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), resultFile);
				}
			}
			zipFile.close();
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not read \"" + distroFile.getAbsolutePath() + "\" to temp folder " + e.getMessage(), e);
		}
		finally {
			distroFile.delete();
		}

		return resultFile;
	}

	public DistroProperties downloadDistroProperties(File path, Artifact artifact) throws MojoExecutionException {
		File file = downloadDistro(path, artifact);
		DistroProperties distroProperties = null;
		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if ("openmrs-distro.properties".equals(zipEntry.getName()) || "distro.properties".equals(zipEntry.getName())) {
					Properties properties = new Properties();
					properties.load(zipFile.getInputStream(zipEntry));
					distroProperties = new DistroProperties(properties);
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not read \"" + file.getAbsolutePath() + "\" " + e.getMessage(), e);
		}
		finally {
			file.delete();
		}

		return distroProperties;
	}

	public DistroProperties downloadDistroProperties(File serverPath, Server server, String fileType) throws MojoExecutionException {
		Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(),
				fileType);
		if (StringUtils.isNotBlank(artifact.getArtifactId())) {
			return downloadDistroProperties(serverPath, artifact);
		} else {
			return null;
		}
	}

	public DistroProperties downloadDistroProperties(File serverPath, Server server) throws MojoExecutionException {
		return downloadDistroProperties(serverPath, server, "jar");
	}

	/**
	 * Distro can be passed in two ways: either as maven artifact identifier or path to distro file
	 * Returns null if string is invalid as path or identifier
	 *
	 * @param distro
	 * @return
	 */
	public DistroProperties resolveDistroPropertiesForStringSpecifier(String distro, VersionsHelper versionsHelper)
			throws MojoExecutionException {
		DistroProperties result;
		result = getDistroPropertiesFromFile(new File(distro));
		if (result != null && mavenProject != null) {
			result.resolvePlaceholders(getProjectProperties());
		} else {
			Artifact artifact = parseDistroArtifact(distro, versionsHelper);
			if (isRefapp2_3_1orLower(artifact)) {
				result = new DistroProperties(artifact.getVersion());
			} else if (isRefappBelow2_1(artifact)) {
				throw new MojoExecutionException("Reference Application versions below 2.1 are not supported!");
			} else {
				result = downloadDistroProperties(new File(System.getProperty("user.dir")), artifact);
			}
		}
		return result;
	}

	private Properties getProjectProperties() {
		Properties properties = mavenProject.getProperties();
		properties.setProperty("project.parent.version", mavenProject.getVersion());
		properties.setProperty("project.version", mavenProject.getVersion());
		return properties;
	}

	/**
	 * resolves distro based on passed artifact and saves distro.properties file in destination
	 */
	public void saveDistroPropertiesTo(File destination, String distro) throws MojoExecutionException {
		DistroProperties distroProperties = resolveDistroPropertiesForStringSpecifier(distro, null);
		if (distroProperties != null) {
			distroProperties.saveTo(destination);
		}
	}

	/**
	 * should:
	 * - ignore modules which are already on server, but not included in distro properties of upgrade
	 * - keep new platform artifact if distro properties declares newer version
	 * - updateMap include modules which are already on server with newer/equal SNAPSHOT version
	 * - add modules which are not installed on server yet
	 */
	public static UpgradeDifferential calculateUpdateDifferential(DistroHelper distroHelper, Server server,
																  DistroProperties distroProperties) throws MojoExecutionException {
		List<Artifact> newList = new ArrayList<>(
				distroProperties.getWarArtifacts(distroHelper, server.getServerDirectory()));
		newList.addAll(distroProperties.getModuleArtifacts(distroHelper, server.getServerDirectory()));
		return calculateUpdateDifferential(server.getServerModules(), newList);
	}

	static UpgradeDifferential calculateUpdateDifferential(List<Artifact> oldList, List<Artifact> newList)
			throws MojoExecutionException {
		UpgradeDifferential upgradeDifferential = new UpgradeDifferential();
		for (Artifact newListModule : newList) {
			boolean toAdd = true;
			for (Artifact oldListModule : oldList) {
				if (isSameArtifact(oldListModule, newListModule)) {
					if (isHigherVersion(oldListModule, newListModule)) {
						if (isOpenmrsWebapp(newListModule)) {
							upgradeDifferential.setPlatformArtifact(newListModule);
							upgradeDifferential.setPlatformUpgraded(true);
						} else {
							upgradeDifferential.putUpdateEntry(oldListModule, newListModule);
						}
					} else if (isLowerVersion(oldListModule, newListModule)) {
						if (isOpenmrsWebapp(newListModule)) {
							upgradeDifferential.setPlatformArtifact(newListModule);
							upgradeDifferential.setPlatformUpgraded(false);
						} else {
							upgradeDifferential.putDowngradeEntry(oldListModule, newListModule);
						}
					}
					toAdd = false;
					break;
				}
			}
			if (toAdd) {
				upgradeDifferential.addModuleToAdd(newListModule);
			}
		}
		for (Artifact oldListModule : oldList) {
			boolean moduleNotFound = true;
			for (Artifact newListModule : newList) {
				if (isSameArtifact(newListModule, oldListModule)) {
					moduleNotFound = false;
					break;
				}
			}
			if (moduleNotFound) {
				if (isOpenmrsWebapp(oldListModule)) {
					throw new MojoExecutionException("You can delete only modules. Deleting openmrs core is not possible");
				} else {
					upgradeDifferential.addModuleToDelete(oldListModule);
				}
			}
		}
		return upgradeDifferential;
	}

	private static boolean isOpenmrsWebapp(Artifact artifact) {
		return Artifact.TYPE_WAR.equals(artifact.getType()) && SDKConstants.WEBAPP_ARTIFACT_ID
				.equals(artifact.getArtifactId());
	}

	private static boolean isSameArtifact(Artifact left, Artifact right) {
		return getId(left.getDestFileName()).equals(getId(right.getDestFileName()));
	}

	private static String getId(String name) {
		int index = name.indexOf('-');
		if (index == -1)
			return name;
		return name.substring(0, index);
	}

	/**
	 * checks if next artifact is higher version of the same artifact
	 * returns true for equal version snapshots
	 */
	private static boolean isHigherVersion(Artifact previous, Artifact next) {
		if (artifactsToCompareAreInvalid(previous, next)) {
			return false;
		}

		Version previousVersion = new Version(previous.getVersion());
		Version nextVersion = new Version(next.getVersion());

		if (nextVersion.higher(previousVersion)) {
			return true;
		} else if (nextVersion.equal(previousVersion)) {
			return (previousVersion.isSnapshot() && nextVersion.isSnapshot());
		} else {
			return false;
		}
	}

	private static boolean isLowerVersion(Artifact previous, Artifact next) {
		if (artifactsToCompareAreInvalid(previous, next)) {
			return false;
		}

		Version previousVersion = new Version(previous.getVersion());
		Version nextVersion = new Version(next.getVersion());

		if (nextVersion.lower(previousVersion)) {
			return true;
		} else if (nextVersion.equal(previousVersion)) {
			return (previousVersion.isSnapshot() && nextVersion.isSnapshot());
		} else {
			return false;
		}
	}

	private static boolean artifactsToCompareAreInvalid(Artifact previous, Artifact next) {
		return previous == null || next == null
				|| previous.getArtifactId() == null || next.getArtifactId() == null
				|| previous.getVersion() == null || next.getVersion() == null
				|| !isSameArtifact(previous, next);
	}

	public Properties getFrontendProperties(Artifact distroArtifact, File directory) throws MojoExecutionException {
		com.github.zafarkhaja.semver.Version v = com.github.zafarkhaja.semver.Version.parse(distroArtifact.getVersion());

		Artifact artifact;
		if (v.satisfies(">=3.0.0")) {
			artifact = new Artifact("distro-emr-frontend", distroArtifact.getVersion(), distroArtifact.getGroupId(), "zip");
		} else {
			artifact = new Artifact("referenceapplication-frontend", distroArtifact.getVersion(), distroArtifact.getGroupId(), "zip");
		}

		File frontendDistroFile = downloadDistro(directory, artifact, "frontend.zip");
		Properties frontendProperties = new Properties();

		try (ZipFile zipFile = new ZipFile(frontendDistroFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if ("spa-assemble-config.json".equals(zipEntry.getName())) {
					try (InputStream inputStream = zipFile.getInputStream(zipEntry)){
						frontendProperties = PropertiesUtils.getFrontendPropertiesFromJson(inputStream);
					}
					break;
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not read \"" + frontendDistroFile.getAbsolutePath() + "\" " + e.getMessage(), e);
		}
		finally {
			if (frontendDistroFile != null && frontendDistroFile.exists()) {
				frontendDistroFile.delete();
			}
		}
		return frontendProperties;
	}

	public Properties getFrontendProperties(Server server) throws MojoExecutionException {
		Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId());
		return getFrontendProperties(artifact, server.getServerDirectory());
	}

	public Properties getFrontendPropertiesForServer(Artifact artifact, File directory) throws MojoExecutionException {
		String artifactId = artifact.getArtifactId();
		if (artifactId.equals(SDKConstants.REFAPP_DISTRO) || artifactId.equals(SDKConstants.REFAPP_DISTRO_EMR_CONFIGURATION)) {
			if (new Version(artifact.getVersion()).higher(new Version("3.0.0-beta.16"))) {
				return getFrontendProperties(artifact, directory);
			} else {
				return PropertiesUtils.getFrontendPropertiesFromSpaConfigUrl(
						"https://raw.githubusercontent.com/openmrs/openmrs-distro-referenceapplication/" + artifact.getVersion() + "/frontend/spa-build-config.json");
			}
		}
		return new Properties();
	}

    public Properties getFrontendPropertiesForServer(Server server) throws MojoExecutionException {
		Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId());
  		return getFrontendPropertiesForServer(artifact, server.getServerDirectory());
    }

	public Properties getArtifactProperties(Artifact artifact, File directory, String appShellVersion) throws MojoExecutionException {
		File file = downloadDistro(directory, artifact);
		Properties properties = new Properties();
		properties.putAll(PropertiesUtils.getDistroProperties(file));
		properties.putAll(getFrontendPropertiesForServer(artifact, directory));
		properties.putAll(PropertiesUtils.getConfigurationProperty(artifact));
		properties.put(PROPERTY_DISTRO_GROUP_ID, artifact.getGroupId());
		properties.put(PROPERTY_DISTRO_ARTIFACT_ID, artifact.getArtifactId());
		if(appShellVersion != null) {
			properties.setProperty("spa.core", appShellVersion);
		}
		properties.setProperty("omod.spa", versionHelper.getLatestSnapshotVersion(new Artifact("spa", "latest")));
		return properties;
	}

	// TODO: This needs unit tests, but to do so will require more refactoring to allow downloadDistroProperties to be mocked.
	public DistroProperties getDistroPropertiesForFullAncestry(DistroProperties distroProperties, File directory) throws MojoExecutionException {
		Properties mergedProperties = new Properties();

		// First we add the properties from any parent distributions, recursively, excluding any in the exclusions list
		Artifact parentArtifact = distroProperties.getParentDistroArtifact();
		if (parentArtifact != null) {
			DistroProperties parentProperties = downloadDistroProperties(directory, parentArtifact);
			DistroProperties parentFullAncestry = getDistroPropertiesForFullAncestry(parentProperties, directory);
			List<String> exclusions = distroProperties.getExclusions();
			for (Object key : parentFullAncestry.getAllKeys()) {
				String keyStr = key.toString();
				// TODO: Should we widen this to exclude anything that startsWith an exclusion, or allow wildcards?
				if (!exclusions.contains(keyStr)) {
					mergedProperties.put(key, parentFullAncestry.getParam(keyStr));
				}
			}
		}

		// Next, we add the properties from this distribution in.  These will override any defined in a parent
		for (Object key : distroProperties.getAllKeys()) {
			String keyStr = key.toString();
			// We remove the parent distro properties once these have been applied
			if (keyStr.startsWith(TYPE_PARENT + ".") || keyStr.startsWith(TYPE_DISTRO + ".")) {
				continue;
			}
			mergedProperties.put(key, distroProperties.getParam(keyStr));
		}

		return new DistroProperties(mergedProperties);
	}


	public Properties getArtifactProperties(Artifact artifact, Server server, String appShellVersion) throws MojoExecutionException {
		return getArtifactProperties(artifact, server.getServerDirectory(), appShellVersion);
	}

	public DistroProperties resolveParentArtifact(Artifact parentArtifact, File directory, DistroProperties distroProperties, String appShellVersion) throws MojoExecutionException {
		Properties properties = getArtifactProperties(parentArtifact, directory, appShellVersion);
		for (Object key : distroProperties.getAllKeys()) {
			String keyStr = (String) key;
			properties.setProperty(keyStr, distroProperties.getParam(keyStr));
		}
		List<String> exclusions = distroProperties.getExclusions();

		for (String exclusion : exclusions) {
			properties.remove(exclusion);
		}
		return new DistroProperties(properties);
	}

	public DistroProperties resolveParentArtifact(Artifact parentArtifact, Server server, DistroProperties distroProperties, String appShellVersion) throws MojoExecutionException {
		server.setDistroArtifactId(parentArtifact.getArtifactId());
		server.setDistroGroupId(parentArtifact.getGroupId());
		server.setVersion(parentArtifact.getVersion());
		return resolveParentArtifact(parentArtifact, server.getServerDirectory(), distroProperties, appShellVersion);
	}

	/**
	 * Parses and processes content properties from content packages defined in the given {@code DistroProperties} object.
	 *
	 * <p>This method creates a temporary directory to download and process content package ZIP files specified
	 * in the {@code distroProperties} file. The method delegates the download and processing of content packages
	 * to the {@code downloadContentPackages} method, ensuring that the content packages are correctly handled
	 * and validated.</p>
	 *
	 * <p>After processing, the temporary directory used for storing the downloaded ZIP files is deleted,
	 * even if an error occurs during processing. If the temporary directory cannot be deleted, a warning is logged.</p>
	 *
	 * @param distroProperties The {@code DistroProperties} object containing key-value pairs specifying
	 *                         content packages and other properties needed to build a distribution.
	 *
	 * @throws MojoExecutionException If there is an error during the processing of content packages,
	 *                                such as issues with creating the temporary directory, downloading
	 *                                the content packages, or IO errors during file operations.
	 */
	public void parseContentProperties(DistroProperties distroProperties) throws MojoExecutionException {
		File tempDirectory = null;
		try {
			tempDirectory = Files.createTempDirectory("content-packages").toFile();
			downloadContentPackages(tempDirectory, distroProperties);

		} catch (IOException e) {
			throw new MojoExecutionException("Failed to process content packages", e);
		} finally {
			if (tempDirectory != null && tempDirectory.exists()) {
				try {
					FileUtils.deleteDirectory(tempDirectory);
				} catch (IOException e) {
					log.warn("Failed to delete temporary directory: {}", tempDirectory.getAbsolutePath(), e);
				}
			}
		}
	}

	/**
	 * Downloads and processes content packages specified in the given distro properties.
	 *
	 * <p>This method filters out properties starting with a specific prefix (defined by {@code CONTENT_PREFIX})
	 * from the {@code distroProperties} file, identifies the corresponding versions, and downloads the
	 * associated ZIP files from the Maven repository. It then processes each downloaded ZIP file to locate
	 * and parse a {@code content.properties} file, ensuring that the content package is valid and meets
	 * the expected requirements.</p>
	 *
	 * <p>If a {@code groupId} is overridden for a particular content package, the method uses the overridden
	 * value when fetching the package from Maven. The ZIP files are temporarily stored and processed to extract
	 * the {@code content.properties} file, which is then validated and compared against the dependencies specified
	 * in the {@code distro.properties} file.</p>
	 *
	 * @param contentPackageZipFile The directory where content package ZIP files will be temporarily stored.
	 * @param distroProperties The {@code DistroProperties} object containing key-value pairs that specify
	 *                         content packages and other properties needed to build a distribution.
	 *
	 * @throws MojoExecutionException If there is an error during the download or processing of the content packages,
	 *                                such as missing or invalid {@code content.properties} files, or any IO issues.
	 */
	public void downloadContentPackages(File contentPackageZipFile, DistroProperties distroProperties)
			throws MojoExecutionException {

		Properties contentProperties = new Properties();

		for (Artifact artifact : distroProperties.getContentArtifacts()) {

			String zipFileName = artifact.getArtifactId() + "-" + artifact.getVersion() + ".zip";
			File zipFile = downloadDistro(contentPackageZipFile, artifact, zipFileName);

			if (zipFile == null) {
				log.warn("ZIP file not found for content package: {}", artifact.getArtifactId());
				continue;
			}

			try (ZipFile zip = new ZipFile(zipFile)) {
				boolean foundContentProperties = false;
				Enumeration<? extends ZipEntry> entries = zip.entries();

				while (entries.hasMoreElements()) {
					ZipEntry zipEntry = entries.nextElement();
					if (zipEntry.getName().equals(CONTENT_PROPERTIES)) {
						foundContentProperties = true;

						try (InputStream inputStream = zip.getInputStream(zipEntry)) {
							contentProperties.load(inputStream);
							log.info("content.properties file found in {} and parsed successfully.",
									contentPackageZipFile.getName());

							if (contentProperties.getProperty("name") == null
									|| contentProperties.getProperty("version") == null) {
								throw new MojoExecutionException(
										"Content package name or version not specified in content.properties in "
												+ contentPackageZipFile.getName());
							}

							processContentProperties(contentProperties, distroProperties, contentPackageZipFile.getName());
						}
						break;
					}
				}

				if (!foundContentProperties) {
					throw new MojoExecutionException(
							"No content.properties file found in ZIP file: " + contentPackageZipFile.getName());
				}

			}
			catch (IOException e) {
				throw new MojoExecutionException("Error reading content.properties from ZIP file: "
						+ contentPackageZipFile.getName() + ": " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Processes the {@code content.properties} file of a content package and validates the dependencies
	 * against the {@code DistroProperties} provided. This method ensures that the dependencies defined
	 * in the {@code content.properties} file are either present in the {@code distroProperties} file with
	 * a version that matches the specified version range, or it finds the latest matching version if not
	 * already specified in {@code distroProperties}.
	 *
	 * <p>The method iterates over each dependency listed in the {@code content.properties} file, focusing on
	 * dependencies that start with specific prefixes such as {@code omod.}, {@code owa.}, {@code war},
	 * {@code spa.frontendModule}, or {@code content.}. For each dependency, the method performs the following:</p>
	 * <ul>
	 *   <li>If the dependency is not present in {@code distroProperties}, it attempts to find the latest version
	 *   matching the specified version range and adds it to {@code distroProperties}.</li>
	 *   <li>If the dependency is present, it checks whether the version specified in {@code distroProperties}
	 *   falls within the version range specified in {@code content.properties}. If it does not, an error is thrown.</li>
	 * </ul>
	 *
	 * @param contentProperties The {@code Properties} object representing the {@code content.properties} file
	 *                          of a content package.
	 * @param distroProperties  The {@code DistroProperties} object containing key-value pairs specifying
	 *                          the current distribution's dependencies and their versions.
	 * @param zipFileName       The name of the ZIP file containing the {@code content.properties} file being processed.
	 *                          Used in error messages to provide context.
	 *
	 * @throws MojoExecutionException If no matching version is found for a dependency not defined in
	 *                                {@code distroProperties}, or if the version specified in {@code distroProperties}
	 *                                does not match the version range in {@code content.properties}.
	 */
	protected void processContentProperties(Properties contentProperties, DistroProperties distroProperties, String zipFileName) throws MojoExecutionException {
		for (String dependency : contentProperties.stringPropertyNames()) {
			if (dependency.startsWith("omod.") || dependency.startsWith("owa.") || dependency.startsWith("war")
					|| dependency.startsWith("spa.frontendModule") || dependency.startsWith("content.")) {
				String versionRange = contentProperties.getProperty(dependency);
				String distroVersion = distroProperties.get(dependency);

				if (distroVersion == null) {
					String latestVersion = findLatestMatchingVersion(dependency, versionRange);
					if (latestVersion == null) {
						throw new MojoExecutionException(
								"No matching version found for dependency " + dependency + " in " + zipFileName);
					}
					distroProperties.add(dependency, latestVersion);
				} else {
					checkVersionInRange(dependency, versionRange, distroVersion, contentProperties.getProperty("name"));
				}
			}
		}
	}

	/**
	 * Checks if the version from distro.properties satisfies the range specified in content.properties.
	 * Throws an exception if there is a mismatch.
	 *
	 * @param contentDependencyKey The key of the content dependency.
	 * @param contentDependencyVersionRange The version range specified in content.properties.
	 * @param distroPropertyVersion The version specified in distro.properties.
	 * @param contentPackageName The name of the content package.
	 * @throws MojoExecutionException If the version does not fall within the specified range or if the
	 *             range format is invalid.
	 */
	private static void checkVersionInRange(String contentDependencyKey, String contentDependencyVersionRange, String distroPropertyVersion, String contentPackageName) throws MojoExecutionException {
		Semver semverVersion = new Semver(distroPropertyVersion);

		try {
			boolean inRange = semverVersion.satisfies(contentDependencyVersionRange.trim());
			if (!inRange) {
				throw new MojoExecutionException("Incompatible version for " + contentDependencyKey + " in content package "
						+ contentPackageName + ". Specified range: " + contentDependencyVersionRange
						+ ", found in distribution: " + distroPropertyVersion);
			}
		} catch (SemverException e) {
			throw new MojoExecutionException("Invalid version range format for " + contentDependencyKey
					+ " in content package " + contentPackageName + ": " + contentDependencyVersionRange, e);
		}
	}

	public String findLatestMatchingVersion(String dependency, String versionRange) {
		if (dependency.startsWith("omod") || dependency.startsWith("owa") || dependency.startsWith("content.") || dependency.startsWith("war.")) {
			return versionHelper.getLatestReleasedVersion(new Artifact(dependency, "latest"));
		} else if (dependency.startsWith("spa.frontendModule")) {
			PackageJson packageJson = createPackageJson(dependency);
			return getResolvedVersionFromNpmRegistry(packageJson, versionRange);
		}
		throw new IllegalArgumentException("Unsupported dependency type: " + dependency);
	}

	private PackageJson createPackageJson(String dependency) {
		PackageJson packageJson = new PackageJson();
		packageJson.setName(dependency.substring("spa.frontendModules.".length()));
		return packageJson;
	}

	private String getResolvedVersionFromNpmRegistry(PackageJson packageJson, String versionRange) {
		NpmVersionHelper npmVersionHelper = new NpmVersionHelper();
		return npmVersionHelper.getResolvedVersionFromNpmRegistry(packageJson, versionRange);
	}
}
