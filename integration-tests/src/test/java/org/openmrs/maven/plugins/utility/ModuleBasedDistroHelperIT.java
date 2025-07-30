package org.openmrs.maven.plugins.utility;

import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;
import org.openmrs.maven.plugins.AbstractMavenIT;
import org.openmrs.maven.plugins.model.Artifact;
import static org.hamcrest.Matchers.equalTo;

public class ModuleBasedDistroHelperIT extends AbstractMavenIT {

    @Test
	public void build_shouldBuildGeneratePropertiesBasedOnModuleArtifacts() throws Exception {
		executeTest(() -> {
			// setup
            ModuleBasedDistroHelper helper = new ModuleBasedDistroHelper(getMavenEnvironment());
            Artifact[] artifacts = Arrays.stream("org.openmrs.module:legacyui-omod:1.23.0, org.openmrs.module:webservices.rest:2.49.0, org.openmrs.module:fhir2:2.5.0, org.openmrs.module:metadatadeploy:1.13.0".split(","))
            .map(String::trim)
            .map(spec -> {
                String[] parts = spec.split(":");
                return new Artifact(parts[1], parts[2], parts[0]);
            }).toArray(Artifact[]::new);

            Properties expectedProperties = new Properties();
            String props = "name=Module based distro, omod.metadatamapping=1.0.1, omod.metadatasharing=1.1.8, war.openmrs.groupId=org.openmrs.web, omod.fhir2=2.5.0, omod.legacyui=1.23.0, omod.metadatadeploy=1.13.0, version=100.0.0-SNAPSHOT, omod.webservices.rest=2.49.0, db.h2.supported=false, war.openmrs=2.7.4";
            String[] pairs = props.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    expectedProperties.setProperty(keyValue[0].trim(), keyValue[1].trim());
                }
            }

            // replay
            Properties actualProperties = helper.generateDitributionPropertiesFromModules(artifacts);

            // verify
			assertThat(actualProperties.size(), equalTo(expectedProperties.size()));
			for (String p : expectedProperties.stringPropertyNames()) {
				assertThat(actualProperties.getProperty(p), equalTo(expectedProperties.getProperty(p)));
			}
		});
	}
}

