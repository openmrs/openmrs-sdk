package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.utility.PropertiesUtils;

import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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

        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("omod.webservices.rest"), equalTo("2.30.0"));
    }

    @Test
    public void shouldAddSpaDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "SPA");
        addTaskParam("moduleName", "@openmrs/esm-system-admin-app");
        addTaskParam("version", "4.0.3");

        executeTask("add");
        assertSuccess();

        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("spa.frontendModules.@openmrs/esm-system-admin-app"), equalTo("4.0.3"));
    }

    @Test
    public void shouldAddOwaDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "OWA");
        addTaskParam("artifactId", "sysadmin");
        addTaskParam("version", "1.2.0");

        executeTask("add");
        assertSuccess();

        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("owa.sysadmin"), equalTo("1.2.0"));
    }

    @Test
    public void shouldAddWarDependency() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "WAR");
        addTaskParam("version", "2.6.1");

        executeTask("add");
        assertSuccess();

        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("war.openmrs"), equalTo("2.6.1"));
    }

    @Test
    public void shouldAddCustomDependencyIfTypeIsNotSpecified() throws Exception {
        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("property", "custom.property");
        addTaskParam("version", "1.2.3");

        executeTask("add");
        assertSuccess();

        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertThat(distroProperties.getProperty("custom.property"), equalTo("1.2.3"));
    }

    @Test
    public void shouldOverrideIfPropertyAlreadyExists() throws Exception {
        Properties distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("omod.uiframework"), equalTo("3.6"));

        addTaskParam("distro", distroFile.getAbsolutePath());
        addTaskParam("type", "OMOD");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", "uiframework");
        addTaskParam("version", "2.30.0");

        executeTask("add");
        assertSuccess();

        distroProperties = PropertiesUtils.loadPropertiesFromFile(distroFile);
        assertNotNull(distroProperties);
        assertThat(distroProperties.getProperty("omod.uiframework"), equalTo("2.30.0"));
    }
}
