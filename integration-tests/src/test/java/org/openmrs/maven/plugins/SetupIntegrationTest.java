package org.openmrs.maven.plugins;

import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class SetupIntegrationTest extends AbstractSdkIntegrationTest {

    @Test
    public void setup_shouldInstallRefapp2_3_1() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("serverId", serverId);
        addTaskParam("distro", "referenceapplication:2.3.1");
        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/modules");
        assertFileNotPresent(serverId+"/tmp");

        DistroProperties distroProperties = new DistroProperties("2.3.1");
        assertModulesInstalled(serverId, distroProperties);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.3.1"));
    }

    @Test
    public void setup_shouldInstallPlatform1_11_5() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("serverId", serverId);
        addTaskParam("platform", "1.11.5");
        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/h2-1.4.190.jar");
        assertFileNotPresent(serverId+"/modules");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion(""));

    }

    @Test
    public void setup_shouldInstallServerFromGivenDistroProperties() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addTaskParam("serverId", serverId);

        addTaskParam("distro", testDirectory.getAbsolutePath()+"/openmrs-distro.properties");
        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }

    @Test
    @Ignore("Needs a way to automatically select first option in wizard")
    public void setup_shouldInstallServerFromDistroPropertiesDir() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addTaskParam("serverId", serverId);

        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }

    @Test
    public void setup_shouldInstallServerWithGivenJdkAndCreateSdkPropertiesFileIfNotExist() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addTaskParam("serverId", serverId);

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "java.home");
        String defaultJavaHome = System.getProperty("java.home");
        assertThat(defaultJavaHome, is(javaHomeServerProperty));
        assertFilePresent("/sdk.properties");
    }

    @Test
    public void setup_shouldInstallServerWithGivenJdkInBatchMode() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addTaskParam("serverId", serverId);
        addTaskParam("java.home", System.getProperty("java.home"));

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);
    }

    private String readValueFromPropertyKey(File propertiesFile, String key) throws Exception {

        InputStream in = new FileInputStream(propertiesFile);
        Properties sdkProperties = new Properties();
        sdkProperties.load(in);

        return sdkProperties.getProperty(key);
    }
}
