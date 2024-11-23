package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.PropertiesUtils;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddExclusionIT extends AbstractSdkIT {

    @Override
    protected void addTestResources() throws Exception {
        super.addTestResources();
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);
    }

    public DistroProperties getDistroProperties() throws Exception {
        Properties properties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        return new DistroProperties(properties);
    }

    @Test
    public void shouldAddExclusionIfNoParentDefined() throws Exception {
        includeDistroPropertiesFile("openmrs-distro.properties");
        assertNotNull(getDistroProperties());
        assertNull(getDistroProperties().getParentDistroArtifact());
        assertTrue(getDistroProperties().getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        assertNotNull(getDistroProperties());
        assertTrue(getDistroProperties().getExclusions().contains("omod.uicommons"));
        assertTrue(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
    }

    @Test
    public void shouldAddExclusionForPropertyContainedInParent() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-parent-as-parent.properties");
        assertNotNull(getDistroProperties());
        assertNotNull(getDistroProperties().getParentDistroArtifact());
        assertTrue(getDistroProperties().getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        assertNotNull(getDistroProperties());
        assertTrue(getDistroProperties().getExclusions().contains("omod.uicommons"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }

    @Test
    public void shouldAddExclusionForPropertyContainedInDistro() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-parent-as-distro.properties");
        assertNotNull(getDistroProperties());
        assertNotNull(getDistroProperties().getParentDistroArtifact());
        assertTrue(getDistroProperties().getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.uicommons");
        assertNotNull(getDistroProperties());
        assertTrue(getDistroProperties().getExclusions().contains("omod.uicommons"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }

    @Test
    public void shouldAddExclusionForPropertyNotContainedInParent() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-parent-as-parent.properties");
        assertNotNull(getDistroProperties());
        assertNotNull(getDistroProperties().getParentDistroArtifact());
        assertTrue(getDistroProperties().getExclusions().isEmpty());
        executeExclusionTask(distroFile, "omod.invalidmodulename");
        assertNotNull(getDistroProperties());
        assertTrue(getDistroProperties().getExclusions().contains("omod.invalidmodulename"));
        assertFalse(getLogContents().contains(AddExclusion.WARNING_NO_PARENT_DISTRO));
        assertTrue(getLogContents().contains(AddExclusion.WARNING_PROPERTY_NOT_IN_PARENT));
    }

    private String getLogContents() throws Exception {
        File logFile = new File(testDirectory, "log.txt");
        assertTrue(logFile.exists());
        return FileUtils.readFileToString(logFile, "UTF-8");
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
    }
}
