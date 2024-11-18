package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoveDependencyIT extends AbstractSdkIT {

    @Test
    public void shouldRemoveExistingDependency() throws Exception {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("omod.uicommons"));

        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("property", "omod.uicommons");

        executeTask("remove");
        assertSuccess();

        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertFalse(distroProperties.getAllKeys().contains("omod.uicommons"));
    }
}
