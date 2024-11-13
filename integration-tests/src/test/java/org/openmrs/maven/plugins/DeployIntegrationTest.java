package org.openmrs.maven.plugins;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.OwaId;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
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
        addTaskParam("platform", "1.11.5");

        addAnswer(testServerId);

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.11.5.war");
        assertPlatformUpdated(testServerId, "1.11.5");
        assertFileNotPresent(testServerId, "modules", "webservices.rest-2.14.omod");
    }

    @Test
    public void deploy_shouldReplaceDistroPlatform() throws Exception{
        addTaskParam("platform", "2.0.2");

        addAnswer(testServerId);
        addAnswer("y");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId, "openmrs-2.0.2.war");
        assertModulesInstalled(testServerId, "webservices.rest-2.16.omod");
    }

    @Test
    public void deploy_shouldUpgradeDistroTo2_3_1() throws Exception {

        addAnswer(testServerId);
        addAnswer("n");
        addAnswer("n");
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.3.1");
        addAnswer("y");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.11.5.war");
        DistroProperties distroProperties = new DistroProperties("2.3.1");
        assertModulesInstalled(testServerId, distroProperties);
        assertPlatformUpdated(testServerId, "1.11.5");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.3.1"));
    }

    @Test
    public void deploy_shouldUpgradeDistroFromDistroProperties() throws Exception {
        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");

        assertFilePresent(testServerId, "modules", "appui-1.3.omod");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.11.5.war");
        assertFileNotPresent(testServerId, "modules", "appui-1.3.omod");
        assertModulesInstalled(testServerId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
        assertModuleUpdated(testServerId, "owa", "1.4");
    }

    @Test
    public void deploy_shouldInstallModule() throws Exception {
        addTaskParam("artifactId", "owa");
        addTaskParam("groupId", Artifact.GROUP_MODULE);
        addTaskParam("version", "1.4");

        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");
        executeTask("deploy");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
        assertModuleUpdated(testServerId, "owa", "1.4");
    }

    @Test
    public void deploy_shouldInstallModuleFromPomInDir() throws Exception {

        addAnswer(testServerId);
        addAnswer("n");
        addAnswer("y");
        executeTask("deploy");

        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
        assertModuleUpdated(testServerId, "owa", "1.4");
    }

    @Test
    public void deploy_shouldInstallOwaAndOwaModule() throws Exception {
        addTaskParam("owa", "conceptdictionary:1.0.0");

        addAnswer(testServerId);
        addAnswer("y");

        executeTask("deploy");

        assertSuccess();
        assertFilePresent(testServerId, "owa");
        assertFilePresent(testServerId, "owa", "conceptdictionary.owa");

        //check if any owa module is installed
        File modulesDir = getTestFile(testServerId, "modules");

        //can't check for specific file, as always the latest release is installed
        assertThat(modulesDir.listFiles(), hasItemInArray(hasNameStartingWith("owa")));

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, hasUserOwa(new OwaId("conceptdictionary","1.0.0")));
    }
}
