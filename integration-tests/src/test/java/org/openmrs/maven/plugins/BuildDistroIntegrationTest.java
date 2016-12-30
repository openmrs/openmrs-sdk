package org.openmrs.maven.plugins;

import org.junit.Test;

import java.io.File;

public class BuildDistroIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void testBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication");
        executeTask("build-distro");

        assertFilePresent("referenceapplication/docker-compose.yml");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/Dockerfile");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/setenv.sh");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/startup.sh");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/wait-for-it.sh");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/modules");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/openmrs.war");
        assertFilePresent("referenceapplication/referenceapplication-docker-image/openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void testBundledBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication-bundled");
        addTaskParam("bundled", "true");
        executeTask("build-distro");

        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/Dockerfile");
        assertFilePresent("referenceapplication-bundled/docker-compose.yml");
        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/setenv.sh");
        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/startup.sh");
        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/wait-for-it.sh");
        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/openmrs.war");
        assertFilePresent("referenceapplication-bundled/referenceapplication-docker-image/openmrs-distro.properties");

        assertFileNotPresent("referenceapplication-bundled/referenceapplication-docker-image/modules");
        assertFileNotPresent("referenceapplication-bundled/referenceapplication-docker-image/WEB-INF");

        assertZipEntryPresent("referenceapplication-bundled/referenceapplication-docker-image/openmrs.war",
         "WEB-INF/bundledModules");

        assertSuccess();
    }
}
