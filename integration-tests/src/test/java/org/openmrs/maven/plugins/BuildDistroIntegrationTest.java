package org.openmrs.maven.plugins;

import net.lingala.zip4j.core.ZipFile;
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
}
