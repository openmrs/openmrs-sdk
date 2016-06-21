package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class SetupIntegrationTest extends AbstractSdkIntegrationTest {

    private void addMockDbSettings() {
        addMojoParam("dbDriver", "mysql");
        addMojoParam("dbUser", "mysql");
        addMojoParam("dbPassword", "mysql");
        addMojoParam("dbUri", "@DBNAME@");
    }

    @Test
    public void setup_shouldInstallRefapp2_3_1() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addMojoParam("serverId", serverId);
        addMojoParam("distro", "referenceapplication:2.3.1");
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

        addMojoParam("serverId", serverId);
        addMojoParam("platform", "1.11.5");
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
        addMojoParam("serverId", serverId);

        addMojoParam("distro", testDirectory.getAbsolutePath()+"/openmrs-distro.properties");
        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }

    @Test
    public void setup_shouldInstallServerFromDistroPropertiesDir() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addMojoParam("serverId", serverId);

        addMockDbSettings();

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId+"/openmrs-1.11.5.war");
        assertFilePresent(serverId+"/modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }
}
