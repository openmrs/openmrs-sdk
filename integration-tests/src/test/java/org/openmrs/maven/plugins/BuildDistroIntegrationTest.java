package org.openmrs.maven.plugins;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class BuildDistroIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void testBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication");
        executeTask("build-distro");

        assertFilePresent("referenceapplication/docker-compose.yml");
        assertFilePresent("referenceapplication/docker-compose.override.yml");
        assertFilePresent("referenceapplication/docker-compose.prod.yml");
        assertFilePresent("referenceapplication/.env");
        assertFilePresent("referenceapplication/web/Dockerfile");
        assertFilePresent("referenceapplication/web/setenv.sh");
        assertFilePresent("referenceapplication/web/startup.sh");
        assertFilePresent("referenceapplication/web/wait-for-it.sh");
        assertFilePresent("referenceapplication/web/modules");
        assertFilePresent("referenceapplication/web/openmrs.war");
        assertFilePresent("referenceapplication/web/openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void testBundledBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication-bundled");
        addTaskParam("bundled", "true");
        executeTask("build-distro");

        assertFilePresent("referenceapplication-bundled/web/Dockerfile");
        assertFilePresent("referenceapplication-bundled/docker-compose.yml");
        assertFilePresent("referenceapplication-bundled/docker-compose.override.yml");
        assertFilePresent("referenceapplication-bundled/docker-compose.prod.yml");
        assertFilePresent("referenceapplication-bundled/.env");
        assertFilePresent("referenceapplication-bundled/web/setenv.sh");
        assertFilePresent("referenceapplication-bundled/web/startup.sh");
        assertFilePresent("referenceapplication-bundled/web/wait-for-it.sh");
        assertFilePresent("referenceapplication-bundled/web/openmrs.war");
        assertFilePresent("referenceapplication-bundled/web/openmrs-distro.properties");

        assertFileNotPresent("referenceapplication-bundled/web/modules");
        assertFileNotPresent("referenceapplication-bundled/web/WEB-INF");

        assertZipEntryPresent("referenceapplication-bundled/web/openmrs.war",
         "WEB-INF/bundledModules");

        assertSuccess();
    }
}
