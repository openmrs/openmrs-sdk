package org.openmrs.maven.plugins;

import org.junit.Test;

import java.io.File;

public class BuildDistroIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void testBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication-2.3.1");
        executeTask("build-distro");

        testDirectory = new File(testDirectory, "referenceapplication-2.3.1");
        assertFilePresent("Dockerfile");
        assertFilePresent("docker-compose.yml");
        assertFilePresent("setenv.sh");
        assertFilePresent("startup.sh");
        assertFilePresent("wait-for-it.sh");
        assertFilePresent("modules");
        assertFilePresent("openmrs.war");
        assertSuccess();
    }

    @Test
    public void testBundledBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication-2.3.1");
        addTaskParam("bundled", "true");
        executeTask("build-distro");

        testDirectory = new File(testDirectory, "referenceapplication-2.3.1");
        assertFilePresent("Dockerfile");
        assertFilePresent("docker-compose.yml");
        assertFilePresent("setenv.sh");
        assertFilePresent("startup.sh");
        assertFilePresent("wait-for-it.sh");
        assertFilePresent("openmrs.war");

        assertFileNotPresent("modules");
        assertFileNotPresent("WEB-INF");

        assertZipEntryPresent("openmrs.war", "WEB-INF/bundledModules");

        assertSuccess();
    }
}
