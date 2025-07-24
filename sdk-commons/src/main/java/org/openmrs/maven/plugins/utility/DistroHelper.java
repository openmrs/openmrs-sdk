package org.openmrs.maven.plugins.utility;

import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.BaseSdkProperties;
import org.openmrs.maven.plugins.model.ContentPackage;
import org.openmrs.maven.plugins.model.ContentProperties;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.semver4j.Range;
import org.semver4j.RangesList;
import org.semver4j.RangesListFactory;
import org.semver4j.Semver;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

	final MavenEnvironment mavenEnvironment;
	final MavenProject mavenProject;
	final MavenSession mavenSession;
	final BuildPluginManager pluginManager;
	final Wizard wizard;
	final VersionsHelper versionHelper;
	final ArtifactHelper artifactHelper;
	@Setter ContentHelper contentHelper;

	public DistroHelper(MavenEnvironment mavenEnvironment) {
		this.mavenEnvironment = mavenEnvironment;
		this.mavenProject = mavenEnvironment.getMavenProject();
		this.mavenSession = mavenEnvironment.getMavenSession();
		this.pluginManager = mavenEnvironment.getPluginManager();
		this.wizard = mavenEnvironment.getWizard();
		this.versionHelper = mavenEnvironment.getVersionsHelper();
		this.artifactHelper = new ArtifactHelper(mavenEnvironment);
		this.contentHelper = new ContentHelper(mavenEnvironment);
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
			if ("referenceapplication".equals(artifactId) || "distro-emr-configuration".equals(artifactId)) {
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
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not read \"" + distroFile.getAbsolutePath() + "\" to temp folder " + e.getMessage(), e);
		}
		finally {
			distroFile.delete();
		}

		return resultFile;
	}

	/**
	 * Distro can be passed in two ways: either as maven artifact identifier or path to distro file
	 * Returns null if string is invalid as path or identifier
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
	 * This validates the distribution based on distro properties, and indicates any incompatibilities in the declared version
	 * Currently, this only validates content packages, though this could be expanded to validate modules based on config.xml, etc.
	 */
	public void validateDistribution(DistroProperties distroProperties) throws MojoExecutionException {
		List<MissingDependency> missingDependencies = getMissingDependencies(distroProperties);
		if (!missingDependencies.isEmpty()) {
			StringBuilder message = new StringBuilder();
			for (MissingDependency d : missingDependencies) {
				message.append("\n");
				message.append(d.getDependentComponent()).append(" requires ").append(d.getRequiredType());
				message.append(" ").append(d.getRequiredComponent()).append(" version ").append(d.getRequiredVersion());
				if (StringUtils.isBlank(d.getCurrentVersion())) {
					message.append(" but this ").append(d.getRequiredType()).append(" is not included");
				}
				else {
					message.append(" but found version ").append(d.getCurrentVersion());
				}
			}
			throw new MojoExecutionException(message.toString());
		}
	}

	/**
	 * This inspects the distribution based on distro properties, and indicates any incompatibilities in the declared version
	 * Currently, this only reviews dependencies declared within content packages, though this could be expanded to validate
	 * modules based on config.xml, etc.
	 */
	public List<MissingDependency> getMissingDependencies(DistroProperties distroProperties) throws MojoExecutionException {
		List<MissingDependency> ret = new ArrayList<>();
		for (ContentPackage contentPackage : distroProperties.getContentPackages()) {
			String packageName = contentPackage.getGroupIdAndArtifactId();
			ContentProperties contentProperties = contentHelper.getContentProperties(contentPackage);
			ret.addAll(getMissingDependencies(packageName, "war", contentProperties.getWarArtifacts(), distroProperties.getWarArtifacts()));
			ret.addAll(getMissingDependencies(packageName, "module", contentProperties.getModuleArtifacts(), distroProperties.getModuleArtifacts()));
			ret.addAll(getMissingDependencies(packageName, "owa", contentProperties.getOwaArtifacts(), distroProperties.getOwaArtifacts()));
			ret.addAll(getMissingDependencies(packageName, "config", contentProperties.getConfigArtifacts(), distroProperties.getConfigArtifacts()));
			ret.addAll(getMissingDependencies(packageName, "content", contentProperties.getContentPackageArtifacts(), distroProperties.getContentPackageArtifacts()));
			ret.addAll(getMissingFrontendModuleDependencies(packageName, contentProperties, distroProperties));
		}
		return ret;
	}

	/**
	 * @return the missing dependencies for the given set of artifacts
	 */
	List<MissingDependency> getMissingDependencies(String dependentComponent, String requiredType, List<Artifact> requiredArtifacts, List<Artifact> currentArtifacts) {
		List<MissingDependency> ret = new ArrayList<>();
		Map<String, String> currentArtifactVersions = new HashMap<>();
		for (Artifact artifact : currentArtifacts) {
			currentArtifactVersions.put(artifact.getGroupIdAndArtifactId(), artifact.getVersion());
		}
		for (Artifact requiredArtifact : requiredArtifacts) {
			String requiredKey = requiredArtifact.getGroupIdAndArtifactId();
			String currentVersion = currentArtifactVersions.get(requiredKey);
			if (!versionSatisfiesRange(requiredArtifact.getVersion(), currentVersion)) {
				ret.add(new MissingDependency(dependentComponent, requiredType, requiredKey, requiredArtifact.getVersion(), currentVersion));
			}
		}
		return ret;
	}

	/**
	 * @return the missing dependencies for frontend artifacts
	 */
	List<MissingDependency> getMissingFrontendModuleDependencies(String dependentComponent, ContentProperties contentProperties, DistroProperties distroProperties) throws MojoExecutionException {
		List<MissingDependency> ret = new ArrayList<>();
		Map<String, String> requiredModules = contentProperties.getSpaBuildFrontendModules();
		for (Artifact artifact : contentProperties.getSpaArtifacts()) {
			String includes = contentProperties.getSpaArtifactProperties().get(BaseSdkProperties.INCLUDES);
			requiredModules.putAll(getFrontendModulesFromArtifact(artifact, includes));
		}
		Map<String, String> currentModules = distroProperties.getSpaBuildFrontendModules();
		for (Artifact artifact : distroProperties.getSpaArtifacts()) {
			String includes = distroProperties.getSpaArtifactProperties().get(BaseSdkProperties.INCLUDES);
			currentModules.putAll(getFrontendModulesFromArtifact(artifact, includes));
		}
		for (String requiredModule : requiredModules.keySet()) {
			String requiredVersion = requiredModules.get(requiredModule);
			String currentVersion = currentModules.get(requiredModule);
			if (!versionSatisfiesRange(requiredVersion, currentVersion)) {
				ret.add(new MissingDependency(dependentComponent, "esm", requiredModule, requiredVersion, currentVersion));
			}
		}
		return ret;
	}

	/**
	 * @return the missing dependencies for a given frontend artifact.
	 */
	Map<String, String> getFrontendModulesFromArtifact(Artifact artifact, String includes) throws MojoExecutionException {
		Map<String, String> ret = new LinkedHashMap<>();
		try (TempDirectory tempDirectory = TempDirectory.create(artifact.getGroupIdAndArtifactId())) {
			artifactHelper.downloadArtifact(artifact, tempDirectory.getFile(), true);
			File moduleDir = (StringUtils.isNotBlank(includes) ? tempDirectory.getPath().resolve(includes).toFile() : tempDirectory.getFile());
			if (moduleDir.exists() && moduleDir.isDirectory()) {
				for (File file : Objects.requireNonNull(moduleDir.listFiles())) {
					if (file.isDirectory()) {
						String[] fileComponents = file.getName().split("-");
						StringBuilder moduleName = new StringBuilder();
						StringBuilder version = new StringBuilder();
						for (int i = 1; i < fileComponents.length; i++) {
							String component = fileComponents[i];
							if (Semver.isValid(component) || version.length() > 0) {
								if (version.length() > 0) {
									version.append("-");
								}
								version.append(component);
							} else {
								if (moduleName.length() == 0) {
									moduleName.append("@").append(fileComponents[0]).append("/");
								}
								else {
									moduleName.append("-");
								}
								moduleName.append(component);
							}
						}
						ret.put(moduleName.toString(), version.toString());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Alternative version of the `satisfies` functionality from semver4j that allows snapshot and pre-releases from
	 * later versions to satisfy earlier release ranges
	 * This also will always return false if the passed version is blank or null, and always return true if the version is "next"
	 */
	boolean versionSatisfiesRange(String allowedRanges, String version) {
		if (StringUtils.isBlank(version)) {
			return false;
		}
		if (version.equalsIgnoreCase("next")) {
			return true;
		}
		Semver semver = new Semver(version);
		RangesList allowedRangesList = RangesListFactory.create(allowedRanges.trim());
		return allowedRangesList.get().stream().anyMatch(ranges -> {
			for (Range range : ranges) {
				if (!range.isSatisfiedBy(semver)) {
					return false;
				}
			}
			return true;
		});
	}
}
