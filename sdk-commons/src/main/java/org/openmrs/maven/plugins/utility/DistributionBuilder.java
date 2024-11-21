package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Version;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

import static org.openmrs.maven.plugins.model.BaseSdkProperties.TYPE_DISTRO;
import static org.openmrs.maven.plugins.model.BaseSdkProperties.TYPE_PARENT;

/**
 * The purpose of this class is to build OpenMRS distributions out of distro properties files
 * This aims to bring a consistent process to retrieving distribution configuration across all Maven goals,
 * whether loading from a file, a classpath resource, a Properties object, or a Maven artifact
 */
public class DistributionBuilder {

	private final MavenEnvironment mavenEnvironment;

	public DistributionBuilder(MavenEnvironment mavenEnvironment) {
		this.mavenEnvironment = mavenEnvironment;
	}

	/**
	 * Build from a distro properties file that is stored in the given file
	 */
	public Distribution build(File propertiesFile) throws MojoExecutionException {
		Distribution distribution = new Distribution();
		distribution.setFile(propertiesFile);
		Properties properties = PropertiesUtils.loadPropertiesFromFile(propertiesFile);
		return populateDistributionFromProperties(distribution, properties);
	}

	/**
	 * Build from a distro properties file that is stored in the given classpath resource
	 */
	public Distribution build(String resourcePath) throws MojoExecutionException {
		Distribution distribution = new Distribution();
		distribution.setResourcePath(resourcePath);
		Properties properties = PropertiesUtils.loadPropertiesFromResource(resourcePath);
		return populateDistributionFromProperties(distribution, properties);
	}

	/**
	 * Build from the provided properties
	 */
	public Distribution build(Properties properties) throws MojoExecutionException {
		Distribution distribution = new Distribution();
		return populateDistributionFromProperties(distribution, properties);
	}

	/**
	 * Build from a distro properties file bundled in a zip or jar in Maven with the given artifact coordinates
	 */
	public Distribution build(Artifact artifact) throws MojoExecutionException {
		Distribution distribution = new Distribution();
		distribution.setArtifact(artifact);
		ArtifactHelper artifactHelper = new ArtifactHelper(mavenEnvironment);
		Properties properties = null;
		try (TempDirectory tempDir = TempDirectory.create(artifact.getArtifactId())) {
			artifactHelper.downloadArtifact(artifact, tempDir.getFile(), true);
			for (File f : Objects.requireNonNull(tempDir.getFile().listFiles())) {
				if (f.getName().equals(SDKConstants.DISTRO_PROPERTIES_NAME) || f.getName().equals(SDKConstants.DISTRO_PROPERTIES_NAME_SHORT)) {
					distribution.setArtifactPath(tempDir.getPath().relativize(f.toPath()).toString());
					properties = PropertiesUtils.loadPropertiesFromFile(f);
				}
			}
		}
		if (properties == null) {
			throw new MojoExecutionException("Unable to build from artifact, no distro properties file found");
		}
		// Special handling for referenceapplication 3.x
		if (artifact.getGroupId().equals(SDKConstants.REFAPP_3X_GROUP_ID) && artifact.getArtifactId().equals(SDKConstants.REFAPP_3X_ARTIFACT_ID)) {
			populateRefApp3xProperties(distribution, properties);
		}
		return populateDistributionFromProperties(distribution, properties);
	}

	/**
	 * For RefApp 3.x, there is some special-handling to avoid having to declare fixed versions within distro properties,
	 * or to account for changes to compatibility and standards during the course of the development.
	 * This encapsulates these and applies them only for distributions that match the 3.x refapp Maven coordinates
	 * Ideally this would not be needed
	 */
	protected void populateRefApp3xProperties(Distribution distribution, Properties properties) throws MojoExecutionException {
		// Add in the latest spa omod
		Artifact spaModule = new Artifact("spa", "latest", Artifact.GROUP_MODULE);
		String latestSpaSnapshot = mavenEnvironment.getVersionsHelper().getLatestSnapshotVersion(spaModule);
		if (VersionsHelper.NO_VERSION_AVAILABLE_MSG.equals(latestSpaSnapshot)) {
			throw new MojoExecutionException("Unable to retrieve latest snapshot of the spa module");
		}
		properties.put("omod.spa", latestSpaSnapshot);

		// Add distro artifact as the config artifact
		String distroArtifactId = distribution.getArtifact().getArtifactId();
		String distroGroupId = distribution.getArtifact().getGroupId();
		String distroVersion = distribution.getArtifact().getVersion();
		properties.put("config." + distroArtifactId, distroVersion);
		properties.put("config." + distroArtifactId + ".groupId", distroGroupId);

		// Add spa properties
		Properties frontendProperties;
		if (new Version(distroVersion).higher(new Version("3.0.0-beta.16"))) {
			com.github.zafarkhaja.semver.Version v = com.github.zafarkhaja.semver.Version.parse(distroVersion);
			String frontendArtifactId = v.satisfies(">=3.0.0") ? "distro-emr-frontend" : "referenceapplication-frontend";
			Artifact frontendArtifact = new Artifact(frontendArtifactId, distroVersion, distroGroupId, "zip");
			try (TempDirectory tempDir = TempDirectory.create(frontendArtifactId)) {
				mavenEnvironment.getArtifactHelper().downloadArtifact(frontendArtifact, tempDir.getFile(), true);
				File spaAssembleConfig = null;
				for (File f : Objects.requireNonNull(tempDir.getFile().listFiles())) {
					if (f.getName().equals("spa-assemble-config.json")) {
						spaAssembleConfig = f;
					}
				}
				if (spaAssembleConfig == null) {
					throw new MojoExecutionException("Unable to retrieve spa assemble config file from " + frontendArtifact);
				}
				try (FileInputStream inputStream = new FileInputStream(spaAssembleConfig)) {
					frontendProperties = PropertiesUtils.getFrontendPropertiesFromJson(inputStream);
				}
				catch (Exception e) {
					throw new MojoExecutionException("Unable to load frontend config from file: " + spaAssembleConfig, e);
				}
			}
		}
		else {
			String url = "https://raw.githubusercontent.com/openmrs/openmrs-distro-referenceapplication/" + distroVersion + "/frontend/spa-build-config.json";
			frontendProperties = PropertiesUtils.getFrontendPropertiesFromSpaConfigUrl(url);
		}

		for (String propertyName : frontendProperties.stringPropertyNames()) {
			properties.put(propertyName, frontendProperties.getProperty(propertyName));
		}
	}

	/**
	 * Consistently populate the common properties of the distribution from the given properties
	 * This includes handling parent distributions and building a set of effective properties based on
	 * what is provided by ancestors and excluded or overridden by children of these ancestors
	 */
	protected Distribution populateDistributionFromProperties(Distribution distribution, Properties properties) throws MojoExecutionException {
		distribution.setName(properties.getProperty("name"));
		distribution.setVersion(properties.getProperty("version"));
		DistroProperties distroProperties = new DistroProperties(properties);
		distribution.setProperties(distroProperties);
		Artifact parentArtifact = distroProperties.getParentDistroArtifact();
		Properties effectiveProperties = new Properties();
		if (parentArtifact != null) {
			distribution.setParent(build(parentArtifact));
			effectiveProperties.putAll(distribution.getParent().getEffectiveProperties().getAllProperties());
			for (String exclusion : distroProperties.getExclusions()) {
				effectiveProperties.remove(exclusion);
			}
		}
		for (String property : effectiveProperties.stringPropertyNames()) {
			if (!property.startsWith(TYPE_PARENT + ".") && !property.startsWith(TYPE_DISTRO + ".")) {
				effectiveProperties.put(property, properties.getProperty(property));
			}
		}
		distribution.setEffectiveProperties(new DistroProperties(effectiveProperties));
		return distribution;
	}
}
