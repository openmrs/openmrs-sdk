package org.openmrs.maven.plugins.utility;


import lombok.Getter;
import lombok.Setter;
import org.apache.maven.it.VerificationException;
import org.junit.Test;
import org.openmrs.maven.plugins.AbstractMavenIT;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Getter @Setter
public class ArtifactHelperIT extends AbstractMavenIT {

	@Test
	public void downloadArtifact_shouldDownloadToSpecifiedDirectory() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.14.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), false);
			File downloadedArtifact = new File(getMavenTestDirectory(), "idgen-4.14.0.jar");
			assertTrue(downloadedArtifact.exists());
		});
	}

	@Test
	public void downloadArtifact_shouldUnpackToSpecifiedDirectory() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.14.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), true);
			File downloadedArtifact = new File(getMavenTestDirectory(), "idgen-4.14.0.jar");
			assertFalse(downloadedArtifact.exists());
			File liquibaseXml = new File(getMavenTestDirectory(), "liquibase.xml");
			assertTrue(liquibaseXml.exists());
			File configXml = new File(getMavenTestDirectory(), "config.xml");
			assertTrue(configXml.exists());
		});
	}

	@Test(expected = VerificationException.class)
	public void downloadArtifact_shouldFailIfNoArtifactIsFound() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.0.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), false);
		});
	}
}
