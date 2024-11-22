package org.openmrs.maven.plugins.utility;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.maven.plugins.AbstractMavenIT;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.nio.file.Path;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@Getter @Setter
public class DistributionBuilderIT extends AbstractMavenIT {

	@Override
	protected void addTestResources() throws Exception {
		super.addTestResources();
		Path sourcePath = testResourceDir.resolve(TEST_DIRECTORY).resolve("distributions");
		FileUtils.copyDirectoryToDirectory(sourcePath.toFile(), testDirectory);
	}

	@Test
	public void build_shouldBuildRefapp_2_1_0() throws Exception {
		executeTest(() -> {
			DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
			Artifact artifact = new Artifact(SDKConstants.REFAPP_2X_ARTIFACT_ID, "2.1", SDKConstants.REFAPP_2X_GROUP_ID, SDKConstants.REFAPP_2X_TYPE);
			Distribution distribution = builder.buildFromArtifact(artifact);
			assertNotNull(distribution);
			assertThat(distribution.getName(), equalTo("Reference Application"));
			assertThat(distribution.getVersion(), equalTo("2.1"));
			assertNull(distribution.getParent());
			assertThat(distribution.getArtifact(), equalTo(artifact));
			assertNull(distribution.getArtifactPath());
			assertThat(distribution.getResourcePath(), equalTo("openmrs-distro-2.1.properties"));
			assertNull(distribution.getFile());
			DistroProperties distroProperties = distribution.getProperties();
			Properties allProperties = distroProperties.getAllProperties();
			Properties expected = getExpectedPropertiesFromResource(artifact);
			assertThat(allProperties.size(), equalTo(expected.size()));
			for (String p : expected.stringPropertyNames()) {
				assertThat(allProperties.getProperty(p), equalTo(expected.getProperty(p)));
			}
		});
	}

	@Test
	public void build_shouldBuildRefapp_2_13_0() throws Exception {
		executeTest(() -> {
			DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
			Artifact artifact = new Artifact(SDKConstants.REFAPP_2X_ARTIFACT_ID, "2.13.0", SDKConstants.REFAPP_2X_GROUP_ID, SDKConstants.REFAPP_2X_TYPE);
			Distribution distribution = builder.buildFromArtifact(artifact);
			assertNotNull(distribution);
			assertThat(distribution.getName(), equalTo("Reference Application"));
			assertThat(distribution.getVersion(), equalTo("2.13.0"));
			assertNull(distribution.getParent());
			assertThat(distribution.getArtifact(), equalTo(artifact));
			assertThat(distribution.getArtifactPath(), equalTo("openmrs-distro.properties"));
			assertNull(distribution.getResourcePath());
			assertNull(distribution.getFile());
			DistroProperties distroProperties = distribution.getProperties();
			Properties allProperties = distroProperties.getAllProperties();
			Properties expected = getExpectedPropertiesFromResource(artifact);
			assertThat(allProperties.size(), equalTo(expected.size()));
			for (String p : expected.stringPropertyNames()) {
				assertThat(allProperties.getProperty(p), equalTo(expected.getProperty(p)));
			}
		});
	}

	@Test
	public void build_shouldBuildRefapp_3_0_0() throws Exception {
		executeTest(() -> {
			DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
			Artifact artifact = new Artifact(SDKConstants.REFAPP_3X_ARTIFACT_ID, "3.0.0", SDKConstants.REFAPP_3X_GROUP_ID, SDKConstants.REFAPP_3X_TYPE);
			Distribution distribution = builder.buildFromArtifact(artifact);
			assertNotNull(distribution);
			assertThat(distribution.getName(), equalTo("Ref 3.x distro"));
			assertThat(distribution.getVersion(), equalTo("3.0.0"));
			assertNull(distribution.getParent());
			assertThat(distribution.getArtifact(), equalTo(artifact));
			assertThat(distribution.getArtifactPath(), equalTo("distro.properties"));
			assertNull(distribution.getResourcePath());
			assertNull(distribution.getFile());
			DistroProperties distroProperties = distribution.getProperties();
			Properties allProperties = distroProperties.getAllProperties();
			Properties expected = getExpectedPropertiesFromResource(artifact);
			for (String p : expected.stringPropertyNames()) {
				assertThat(allProperties.getProperty(p), equalTo(expected.getProperty(p)));
			}
			// Extra 3.x special properties
			assertNotNull(allProperties.get("omod.spa"));
			getMavenEnvironment().getWizard().showMessage("omod version: " + allProperties.get("omod.spa"));
			assertThat(allProperties.get("config." + artifact.getArtifactId()), equalTo(artifact.getVersion()));
			assertThat(allProperties.get("config." + artifact.getArtifactId() + ".groupId"), equalTo(artifact.getGroupId()));
			assertThat(allProperties.get("spa.core"), equalTo("5.6.0"));
			assertThat(distroProperties.getSpaProperties().size(), equalTo(35));
		});
	}

	Properties getExpectedPropertiesFromResource(Artifact artifact) throws Exception {
		Path distroPath = getMavenTestDirectory().toPath().getParent().resolve("distributions");
		distroPath = distroPath.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".properties");
		return PropertiesUtils.loadPropertiesFromFile(distroPath.toFile());
	}
}
