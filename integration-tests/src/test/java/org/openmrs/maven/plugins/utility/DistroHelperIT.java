package org.openmrs.maven.plugins.utility;


import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.openmrs.maven.plugins.AbstractMavenIT;
import org.openmrs.maven.plugins.model.Artifact;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Getter @Setter
public class DistroHelperIT extends AbstractMavenIT {

	@Test
	public void getFrontendModulesFromArtifact_shouldGetFrontendModulesAndVersions() throws Exception {
		executeTest(() -> {
			DistroHelper distroHelper = new DistroHelper(getMavenEnvironment());
			Artifact artifact = new Artifact("openmrs-frontend-pihemr", "1.8.0", "org.pih.openmrs", "zip");
			Map<String, String> m = distroHelper.getFrontendModulesFromArtifact(artifact, "openmrs-frontend-pihemr-1.8.0");
			assertThat(m.size(), equalTo(39));
			assertThat(m.get("@openmrs/esm-ward-app"), equalTo("8.0.3-pre.4390"));
			assertThat(m.get("@openmrs/esm-login-app"), equalTo("5.8.2-pre.2483"));
		});
	}
}
