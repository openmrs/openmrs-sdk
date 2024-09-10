package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.DistroHelper;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddDependencyTest extends AbstractSdkIntegrationTest {

    private String distroFile;

    private DistroProperties originalProperties;


    @Before
    public void setUp() {
        distroFile = testDirectory + File.separator + "openmrs-distro.properties";
        originalProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
    }

    @Test
    public void shouldAddOmodDependency() throws Exception {
        addTaskParam("distro", distroFile);
        addTaskParam("type", "OMOD");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", "webservices.rest");
        addTaskParam("version", "2.30.0");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getAllKeys().contains("omod.webservices.rest"));
        assertEquals(distroProperties.getParam("omod.webservices.rest"), "2.30.0");
    }

    @Test
    public void shouldAddSpaDependency() throws Exception {
        addTaskParam("distro", distroFile);
        addTaskParam("type", "SPA");
        addTaskParam("moduleName", "@openmrs/esm-system-admin-app");
        addTaskParam("version", "4.0.3");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("spa.frontendModules.@openmrs/esm-system-admin-app"));
        assertEquals(distroProperties.getParam("spa.frontendModules.@openmrs/esm-system-admin-app"), "4.0.3");
    }

    @Test
    public void shouldAddOwaDependency() throws Exception {
        addTaskParam("distro", distroFile);
        addTaskParam("type", "OWA");
        addTaskParam("artifactId", "sysadmin");
        addTaskParam("version", "1.2.0");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("owa.sysadmin"));
        assertEquals(distroProperties.getParam("owa.sysadmin"), "1.2.0");
    }

    @Test
    public void shouldAddWarDependency() throws Exception {
        addTaskParam("distro", distroFile);
        addTaskParam("type", "WAR");
        addTaskParam("version", "2.6.1");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("war.openmrs"));
        assertEquals(distroProperties.getParam("war.openmrs"), "2.6.1");
    }

    @Test
    public void shouldAddCustomDependencyIfTypeIsNotSpecified() throws Exception {
        addTaskParam("distro", distroFile);
        addTaskParam("property", "custom.property");
        addTaskParam("version", "1.2.3");

        executeTask("add");
        assertSuccess();

        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertTrue(distroProperties.getAllKeys().contains("custom.property"));
        assertEquals(distroProperties.getParam("custom.property"), "1.2.3");

    }

    @Test
    public void shouldOverrideIfPropertyAlreadyExists() throws Exception {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);
        assertTrue(distroProperties.getAllKeys().contains("omod.uiframework"));

        addTaskParam("distro", distroFile);
        addTaskParam("type", "OMOD");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", "uiframework");
        addTaskParam("version", "2.30.0");

        executeTask("add");
        assertSuccess();

        distroProperties = DistroHelper.getDistroPropertiesFromFile(new File(distroFile));
        assertNotNull(distroProperties);

        assertTrue(distroProperties.getAllKeys().contains("omod.uiframework"));
        assertEquals(distroProperties.getParam("omod.uiframework"), "2.30.0");
    }

    @After
    public void reset() throws MojoExecutionException {
        originalProperties.saveTo(new File(distroFile).getParentFile());
    }
}
