package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddExclusionIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void shouldAddExclusionIfNoParentDefined() throws Exception {
        useDistroProperties("openmrs-distro.properties");
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertNull(distroProperties.getParentDistroArtifact());
        assertTrue(distroProperties.getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
        assertTrue(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
    }

    @Test
    public void shouldAddExclusionForPropertyContainedInParent() throws Exception {
        useDistroProperties("openmrs-distro-parent-as-parent.properties");
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertNotNull(distroProperties.getParentDistroArtifact());
        assertTrue(distroProperties.getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }

    @Test
    public void shouldAddExclusionForPropertyContainedInDistro() throws Exception {
        useDistroProperties("openmrs-distro-parent-as-distro.properties");
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertNotNull(distroProperties.getParentDistroArtifact());
        assertTrue(distroProperties.getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }

    @Test
    public void shouldAddExclusionForPropertyNotContainedInParent() throws Exception {
        useDistroProperties("openmrs-distro-parent-as-parent.properties");
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertNotNull(distroProperties.getParentDistroArtifact());
        assertTrue(distroProperties.getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.invalidmodulename");
        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getExclusions().contains("omod.invalidmodulename"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertTrue(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }



    private String getLogContents() throws Exception {
        File logFile = new File(testDirectory, "log.txt");
        assertTrue(logFile.exists());
        return FileUtils.readFileToString(logFile, "UTF-8");
    }

    private void useDistroProperties(String fileName) throws Exception {
        File testFile = new File(testDirectory, fileName);
        if (!testFile.equals(distroFile)) {
            FileUtils.copyFile(testFile, distroFile);
        }
    }

    private void executeExclusionTask(File distroFile, String exclusion) throws Exception {
        if (distroFile != null) {
            addTaskParam("distro", distroFile.getAbsolutePath());
        }
        if (exclusion != null) {
            addTaskParam("property", exclusion);
        }
        executeTask("exclusion");
        assertSuccess();
        distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertTrue(distroProperties.getExclusions().contains("omod.uicommons"));
    }
}
