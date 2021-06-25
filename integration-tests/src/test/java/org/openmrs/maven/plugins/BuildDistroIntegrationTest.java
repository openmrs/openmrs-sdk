package org.openmrs.maven.plugins;

import org.junit.Test;

import java.io.File;
import java.util.UUID;

public class BuildDistroIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void testBuildDistroFromDistroFile() throws Exception {
        addTaskParam("dir", "target");
        executeTask("build-distro");

        assertFilePresent("target/docker-compose.yml");
        assertFilePresent("target/docker-compose.override.yml");
        assertFilePresent("target/docker-compose.prod.yml");
        assertFilePresent("target/.env");
        assertFilePresent("target/web/Dockerfile");
        assertFilePresent("target/web/setenv.sh");
        assertFilePresent("target/web/startup.sh");
        assertFilePresent("target/web/wait-for-it.sh");
        assertFilePresent("target/web/modules");
        assertFilePresent("target/web/frontend/index.html");
        assertFilePresent("target/web/owa");
        assertFilePresent("target/web/openmrs.war");
        assertFilePresent("target/web/openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void testBuildDistroRefApp23() throws Exception {
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
