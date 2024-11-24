package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

	private static final Logger log = LoggerFactory.getLogger(DistroHelper.class);

	final MavenEnvironment mavenEnvironment;
	final MavenProject mavenProject;
	final MavenSession mavenSession;
	final BuildPluginManager pluginManager;
	final Wizard wizard;

	final VersionsHelper versionHelper;

	public DistroHelper(MavenEnvironment mavenEnvironment) {
		this.mavenEnvironment = mavenEnvironment;
		this.mavenProject = mavenEnvironment.getMavenProject();
		this.mavenSession = mavenEnvironment.getMavenSession();
		this.pluginManager = mavenEnvironment.getPluginManager();
		this.wizard = mavenEnvironment.getWizard();
		this.versionHelper = mavenEnvironment.getVersionsHelper();
	}

	/**
	 * @return distro properties from openmrs-distro.properties file in current directory or null if not exist
	 */
	public File getDistroPropertiesFileFromDir() {
		File distroFile = new File(new File(System.getProperty("user.dir")), "openmrs-distro.properties");
		if (distroFile.exists()) {
			return distroFile;
		}
		return null;
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

		if (Artifact.GROUP_DISTRO.equals(groupId) || Artifact.GROUP_OPENMRS.equals(groupId)) {
			if ("referenceapplication".equals(artifactId)) {
				Version v = new Version(version);
				if (v.getMajorVersion() <= 2) {
					groupId = SDKConstants.REFAPP_2X_GROUP_ID;
					artifactId = SDKConstants.REFAPP_2X_ARTIFACT_ID;
					type = SDKConstants.REFAPP_2X_TYPE;
				}
				else if (v.getMajorVersion() == 3 && (v.isAlpha() || v.isBeta() || v.isSnapshot())) {
					groupId = SDKConstants.REFAPP_2X_GROUP_ID;
					artifactId = SDKConstants.REFAPP_DISTRO;
					type = Artifact.TYPE_ZIP;
				}
				else {
					groupId = SDKConstants.REFAPP_3X_GROUP_ID;
					artifactId = SDKConstants.REFAPP_3X_ARTIFACT_ID;
					type = SDKConstants.REFAPP_3X_TYPE;
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

	public static boolean isRefappBelow2_1(String artifactId, String version) {
		if (artifactId != null && artifactId.equals(SDKConstants.REFAPP_2X_ARTIFACT_ID)) {
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

	/**
	 * Distro can be passed in two ways: either as maven artifact identifier or path to distro file
	 * Returns null if string is invalid as path or identifier
	 *
	 * @param distro
	 * @return
	 */
	public Distribution resolveDistributionForStringSpecifier(String distro, VersionsHelper versionsHelper) throws MojoExecutionException {
		DistributionBuilder builder = new DistributionBuilder(mavenEnvironment);
		File distroFile = new File(distro);
		if (distroFile.exists()) {
			return builder.buildFromFile(distroFile);
		}
		Artifact artifact = parseDistroArtifact(distro, versionsHelper);
		if (isRefappBelow2_1(artifact)) {
			throw new MojoExecutionException("Reference Application versions below 2.1 are not supported!");
		}

		Distribution distribution = builder.buildFromArtifact(artifact);
		if (distribution == null) {
			throw new MojoExecutionException("Distribution " + distro + " not found");
		}
		return distribution;
	}

	/**
	 * Distro can be passed in two ways: either as maven artifact identifier or path to distro file
	 * Returns null if string is invalid as path or identifier
	 *
	 * @param distro
	 * @return
	 */
	public DistroProperties resolveDistroPropertiesForStringSpecifier(String distro, VersionsHelper versionsHelper) throws MojoExecutionException {
		Distribution distribution = resolveDistributionForStringSpecifier(distro, versionsHelper);
		if (distribution == null) {
			throw new MojoExecutionException("Distribution " + distro + " not found");
		}
		return distribution.getEffectiveProperties();
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
	public UpgradeDifferential calculateUpdateDifferential(Server server, Distribution distribution) {

		UpgradeDifferential upgradeDifferential = new UpgradeDifferential();
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
