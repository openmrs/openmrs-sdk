package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.PropertiesUtils;

import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoveDependencyIT extends AbstractSdkIT {

    @Test
    public void shouldRemoveExistingDependency() throws Exception {
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);
        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.containsKey("omod.uicommons"));

        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("property", "omod.uicommons");

        executeTask("remove");
        assertSuccess();

        distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertFalse(distroProperties.containsKey("omod.uicommons"));
    }
}
