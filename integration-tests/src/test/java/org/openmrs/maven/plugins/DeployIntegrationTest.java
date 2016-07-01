package org.openmrs.maven.plugins;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.maven.plugins.bintray.BintrayId;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openmrs.maven.plugins.SdkMatchers.hasNameStartingWith;
import static org.openmrs.maven.plugins.SdkMatchers.hasUserOwa;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class DeployIntegrationTest extends AbstractSdkIntegrationTest {

    private static String testServerId;

    @Before
    public void setupServer() throws Exception{
        testServerId = setupTestServer();
    }

    @After
    public void deleteServer() throws Exception {
        deleteTestServer(testServerId);
    }

    @Test
    public void deploy_shouldReplaceWebapp() throws Exception {
        addTaskParam("serverId", testServerId);
        addTaskParam("platform", "1.11.5");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
    }

    @Test
    public void deploy_shouldUpgradeDistroTo2_3_1() throws Exception {
        addTaskParam("serverId", testServerId);
        addTaskParam("distro", "referenceapplication:2.3.1");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
        DistroProperties distroProperties = new DistroProperties("2.3.1");
        assertModulesInstalled(testServerId, distroProperties);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.3.1"));
    }

    @Test
    public void deploy_shouldUpgradeDistroFromDistroProperties() throws Exception {
        addTaskParam("serverId", testServerId);

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId+"/openmrs-1.11.5.war");
        assertModulesInstalled(testServerId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
    }

    @Test
    public void deploy_shouldInstallModule() throws Exception {
        addTaskParam("serverId", testServerId);
        addTaskParam("artifactId", "owa");
        addTaskParam("groupId", Artifact.GROUP_MODULE);
        addTaskParam("version", "1.4");

        executeTask("deploy");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
    }

    @Test
    public void deploy_shouldInstallModuleFromPomInDir() throws Exception {
        addTaskParam("serverId", testServerId);

        executeTask("deploy");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
    }

    @Test
    public void deploy_shouldInstallOwaAndOwaModule() throws Exception {
        addTaskParam("serverId", testServerId);
        addTaskParam("owa", "conceptdictionary:1.0.0-beta.6");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId+ File.separator+"owa");
        assertFilePresent(testServerId+ File.separator+"owa"+File.separator+"conceptdictionary");

        //check if any owa module is installed
        File modulesDir = new File(testDirectory, testServerId+File.separator+"modules");

        //can't check for specific file, as always the latest release is installed
        assertThat(Arrays.asList(modulesDir.listFiles()), hasItem(hasNameStartingWith("owa")));

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, hasUserOwa(new BintrayId("openmrs-owa-conceptdictionary","1.0.0-beta.6")));
    }
}
