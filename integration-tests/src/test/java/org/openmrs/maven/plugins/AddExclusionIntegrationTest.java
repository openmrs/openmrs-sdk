package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddExclusionIntegrationTest extends AbstractSdkIntegrationTest {

    private String distroFile;

    private DistroProperties originalProperties;

    @Before
    public void setUp() {
        distroFile = testDirectory + File.separator + "openmrs-distro.properties";
        originalProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
    }

    @Test
    public void shouldAddExclusion() throws Exception {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);
        assertFalse(distroProperties.getExclusions().contains("omod.uicommons"));

        addTaskParam("distro", distroFile);
        addTaskParam("property", "omod.uicommons");
        executeTask("exclusion");

        assertSuccess();
        distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
    }


    @After
    public void reset() throws MojoExecutionException {
        originalProperties.saveTo(new File(distroFile).getParentFile());
    }
}
