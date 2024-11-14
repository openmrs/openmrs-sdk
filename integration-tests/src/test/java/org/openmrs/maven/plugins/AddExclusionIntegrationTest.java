package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddExclusionIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void shouldAddExclusion() throws Exception {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertFalse(distroProperties.getExclusions().contains("omod.uicommons"));

        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("property", "omod.uicommons");
        executeTask("exclusion");

        assertSuccess();
        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
    }
}
