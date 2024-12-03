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
import static org.openmrs.maven.plugins.model.BaseSdkProperties.TYPE_ZIP;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_ARTIFACT_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_GROUP_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_ARTIFACT_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_GROUP_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER;

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
	public Distribution buildFromFile(File propertiesFile) throws MojoExecutionException {
		mavenEnvironment.getWizard().showMessage("Building distribution from file: " + propertiesFile.getName());
		Distribution distribution = new Distribution();
		distribution.setFile(propertiesFile);
		Properties properties = PropertiesUtils.loadPropertiesFromFile(propertiesFile);
		PropertiesUtils.resolveMavenPropertyPlaceholders(properties, mavenEnvironment.getMavenProject());
		return populateDistributionFromProperties(distribution, properties);
	}

	/**
	 * Build from a distro properties file bundled in a zip or jar in Maven with the given artifact coordinates
	 */
	public Distribution buildFromArtifact(Artifact artifact) throws MojoExecutionException {
		mavenEnvironment.getWizard().showMessage("Building distribution from artifact: " + artifact);
		Distribution distribution = new Distribution();
		artifact = DistroHelper.normalizeArtifact(artifact, mavenEnvironment.getVersionsHelper());
		distribution.setArtifact(artifact);
		ArtifactHelper artifactHelper = new ArtifactHelper(mavenEnvironment);
		Properties properties = null;

		// Special Handling for referenceapplication 2.x versions that are not published to Maven
		if (REFAPP_2X_GROUP_ID.equals(artifact.getGroupId()) && REFAPP_2X_ARTIFACT_ID.equals(artifact.getArtifactId())) {
			if (SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER.contains(artifact.getVersion())) {
				String resourcePath = "openmrs-distro-" + artifact.getVersion() + ".properties";
				distribution.setArtifact(artifact);
				distribution.setResourcePath(resourcePath);
				mavenEnvironment.getWizard().showMessage("This is an early refapp 2.x version, loading from: " + resourcePath);
				properties = PropertiesUtils.loadPropertiesFromResource(resourcePath);
			}
		}

		// Normal handling is to download the distro artifact from Maven, and extract the distro properties file
		if (properties == null) {
			try (TempDirectory tempDir = TempDirectory.create(artifact.getArtifactId())) {
				artifactHelper.downloadArtifact(artifact, tempDir.getFile(), true);
				for (File f : Objects.requireNonNull(tempDir.getFile().listFiles())) {
					if (f.getName().equals(SDKConstants.DISTRO_PROPERTIES_NAME) || f.getName().equals(SDKConstants.DISTRO_PROPERTIES_NAME_SHORT)) {
						distribution.setArtifactPath(tempDir.getPath().relativize(f.toPath()).toString());
						properties = PropertiesUtils.loadPropertiesFromFile(f);
					}
				}
			}
		}

		if (properties == null) {
			throw new MojoExecutionException("Unable to build from artifact, no distro properties file found");
		}

		// Special handling for referenceapplication 2.x issues
		if (REFAPP_2X_GROUP_ID.equals(artifact.getGroupId()) && REFAPP_2X_ARTIFACT_ID.equals(artifact.getArtifactId())) {
			mavenEnvironment.getWizard().showMessage("This is a 2.x refapp distribution");
			populateRefApp2xProperties(distribution, properties);
		}

		// Special handling for referenceapplication 3.x, which does not define everything needed in the published distro properties file
		if (REFAPP_3X_GROUP_ID.equals(artifact.getGroupId()) && REFAPP_3X_ARTIFACT_ID.equals(artifact.getArtifactId())) {
			mavenEnvironment.getWizard().showMessage("This is a 3.x refapp distribution");
			populateRefApp3xProperties(distribution, properties);
		}
		return populateDistributionFromProperties(distribution, properties);
	}

	public void populateRefApp2xProperties(Distribution distribution, Properties properties) {
		// Some refapp versions (eg. 2.13.0) include atlas version 2.2.6, which is not published in Maven.  Adjust this.
		if ("2.2.6".equals(properties.getProperty("omod.atlas"))) {
			properties.put("omod.atlas", "2.2.7");
		}
	}

	/**
	 * For RefApp 3.x, there is some special-handling to avoid having to declare fixed versions within distro properties,
	 * or to account for changes to compatibility and standards during the course of the development.
	 * This encapsulates these and applies them only for distributions that match the 3.x refapp Maven coordinates
	 * Ideally this would not be needed
	 */
	protected void populateRefApp3xProperties(Distribution distribution, Properties properties) throws MojoExecutionException {

		String distroArtifactId = distribution.getArtifact().getArtifactId();
		String distroGroupId = distribution.getArtifact().getGroupId();
		String distroVersion = distribution.getArtifact().getVersion();

		DistroProperties includedProperties = new DistroProperties(properties);

		// If the spa module is not included explicitly, include the latest snapshot
		if (!includedProperties.contains("omod.spa")) {
			Artifact spaModule = new Artifact("spa", "latest", Artifact.GROUP_MODULE);
			String latestSpaSnapshot = mavenEnvironment.getVersionsHelper().getLatestSnapshotVersion(spaModule);
			if (VersionsHelper.NO_VERSION_AVAILABLE_MSG.equals(latestSpaSnapshot)) {
				throw new MojoExecutionException("Unable to retrieve latest snapshot of the spa module");
			}
			properties.put("omod.spa", latestSpaSnapshot);
		}

		// Add distro artifact as the config artifact if no config or content are included specifically
		if (includedProperties.getConfigArtifacts().isEmpty() && includedProperties.getContentArtifacts().isEmpty()) {
			properties.put("config." + distroArtifactId, distroVersion);
			properties.put("config." + distroArtifactId + ".groupId", distroGroupId);
			properties.put("config." + distroArtifactId + ".type", TYPE_ZIP);
		}

		// Add spa properties if they are not included explicitly
		if (includedProperties.getSpaProperties().isEmpty()) {
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
					} catch (Exception e) {
						throw new MojoExecutionException("Unable to load frontend config from file: " + spaAssembleConfig, e);
					}
				}
			} else {
				String url = "https://raw.githubusercontent.com/openmrs/openmrs-distro-referenceapplication/" + distroVersion + "/frontend/spa-build-config.json";
				frontendProperties = PropertiesUtils.getFrontendPropertiesFromSpaConfigUrl(url);
			}

			for (String propertyName : frontendProperties.stringPropertyNames()) {
				properties.put(propertyName, frontendProperties.getProperty(propertyName));
			}
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

		Properties effectiveProperties = new Properties();
		Artifact parentArtifact = distroProperties.getParentDistroArtifact();
		if (parentArtifact != null) {
			distribution.setParent(buildFromArtifact(parentArtifact));
			effectiveProperties.putAll(distribution.getParent().getEffectiveProperties().getAllProperties());
			for (String exclusion : distroProperties.getExclusions()) {
				effectiveProperties.remove(exclusion);
			}
		}
		for (String property : distroProperties.getAllProperties().stringPropertyNames()) {
			if (!property.startsWith(TYPE_PARENT + ".") && !property.startsWith(TYPE_DISTRO + ".")) {
				effectiveProperties.put(property, properties.getProperty(property));
			}
		}

		distribution.setEffectiveProperties(new DistroProperties(effectiveProperties));
		return distribution;
	}
}
