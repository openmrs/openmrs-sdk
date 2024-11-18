package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddDependencyIT extends AbstractSdkIT {

    @Test
    public void shouldAddOmodDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "OMOD");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", "webservices.rest");
        addTaskParam("version", "2.30.0");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getAllKeys().contains("omod.webservices.rest"));
        assertEquals(distroProperties.getParam("omod.webservices.rest"), "2.30.0");
    }

    @Test
    public void shouldAddSpaDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "SPA");
        addTaskParam("moduleName", "@openmrs/esm-system-admin-app");
        addTaskParam("version", "4.0.3");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("spa.frontendModules.@openmrs/esm-system-admin-app"));
        assertEquals(distroProperties.getParam("spa.frontendModules.@openmrs/esm-system-admin-app"), "4.0.3");
    }

    @Test
    public void shouldAddOwaDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "OWA");
        addTaskParam("artifactId", "sysadmin");
        addTaskParam("version", "1.2.0");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("owa.sysadmin"));
        assertEquals(distroProperties.getParam("owa.sysadmin"), "1.2.0");
    }

    @Test
    public void shouldAddWarDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "WAR");
        addTaskParam("version", "2.6.1");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("war.openmrs"));
        assertEquals(distroProperties.getParam("war.openmrs"), "2.6.1");
    }

    @Test
    public void shouldAddCustomDependencyIfTypeIsNotSpecified() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("property", "custom.property");
        addTaskParam("version", "1.2.3");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertTrue(distroProperties.getAllKeys().contains("custom.property"));
        assertEquals(distroProperties.getParam("custom.property"), "1.2.3");

    }

    @Test
    public void shouldOverrideIfPropertyAlreadyExists() throws Exception {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getAllKeys().contains("omod.uiframework"));

        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "OMOD");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", "uiframework");
        addTaskParam("version", "2.30.0");

        executeTask("add");
        assertSuccess();

        distroProperties = DistroHelper.getDistroPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("omod.uiframework"));
        assertEquals(distroProperties.getParam("omod.uiframework"), "2.30.0");
    }
}
