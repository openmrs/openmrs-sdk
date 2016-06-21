package org.openmrs.maven.plugins;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class ModuleInstallIntegrationTest extends AbstractSdkIntegrationTest {

    private static String testServerId;

    @Before
    public void setUp() throws Exception{
        super.setUp();
        testServerId = setupTestServer();
    }

    @After
    public void tearDownClass() throws Exception {
        super.tearDown();
        deleteTestServer(testServerId);
    }

    @Test
    public void moduleInstall_shouldReplaceWebapp() throws Exception {
        addMojoParam("serverId", testServerId);
        addMojoParam("platform", "1.11.5");

        executeTask("install");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
    }

    @Test
    public void moduleInstall_shouldUpgradeDistroTo2_3_1() throws Exception {
        addMojoParam("serverId", testServerId);
        addMojoParam("distro", "referenceapplication:2.3.1");

        executeTask("install");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
        DistroProperties distroProperties = new DistroProperties("2.3.1");
        assertModulesInstalled(testServerId, distroProperties);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.3.1"));
    }

    @Test
    public void moduleInstall_shouldUpgradeDistroFromDistroProperties() throws Exception {
        addMojoParam("serverId", testServerId);

        executeTask("install");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
        assertModulesInstalled(testServerId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }

    @Test
    public void moduleInstall_shouldInstallModule() throws Exception {
        addMojoParam("serverId", testServerId);
        addMojoParam("artifactId", "owa");
        addMojoParam("groupId", Artifact.GROUP_MODULE);
        addMojoParam("version", "1.4");

        executeTask("install");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
    }

    @Test
    public void moduleInstall_shouldInstallModuleFromPomInDir() throws Exception {
        addMojoParam("serverId", testServerId);

        executeTask("install");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
    }
}
