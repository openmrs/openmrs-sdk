package org.openmrs.maven.plugins.utility;


import lombok.Getter;
import lombok.Setter;
import org.apache.maven.it.VerificationException;
import org.junit.Test;
import org.openmrs.maven.plugins.AbstractMavenIT;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;

import static org.junit.Assert.assertTrue;

@Getter @Setter
public class ArtifactHelperIT extends AbstractMavenIT {

	@Test
	public void test_downloadModuleWithDefaultName() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.14.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), null);
			File expectedFile = new File(getMavenTestDirectory(), "idgen-omod-4.14.0.jar");
			assertTrue(expectedFile.exists());
		});
	}

	@Test
	public void downloadModuleWithSpecificName() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.14.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), "idgen.omod");
			File expectedFile = new File(getMavenTestDirectory(), "idgen.omod");
			assertTrue(expectedFile.exists());
		});
	}

	@Test(expected = VerificationException.class)
	public void downloadModuleThatDoesNotExist() throws Exception {
		executeTest(() -> {
			ArtifactHelper artifactHelper = new ArtifactHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("idgen-omod", "4.0.0", "org.openmrs.module", "jar");
			artifactHelper.downloadArtifact(artifact, getMavenTestDirectory(), "idgen.omod");
		});
	}
}
