package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.OwaId;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.PropertiesUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertFalse;
import static org.openmrs.maven.plugins.SdkMatchers.hasNameStartingWith;
import static org.openmrs.maven.plugins.SdkMatchers.hasUserOwa;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class DeployIT extends AbstractSdkIT {

    private String testServerId;

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    public void deploy_shouldReplaceWebapp() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        assertFilePresent(testServerId, "openmrs-1.11.2.war");
        assertFileNotPresent(testServerId, "openmrs-1.11.5.war");
        addTaskParam("platform", "1.11.5");
        addAnswer(testServerId);
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.11.5.war");
        assertPlatformUpdated(testServerId, "1.11.5");
        assertFileNotPresent(testServerId, "modules", "webservices.rest-2.14.omod");
    }

    @Test
    public void deploy_shouldReplaceDistroPlatform() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        assertFilePresent(testServerId, "openmrs-1.11.2.war");
        assertFileNotPresent(testServerId, "webservices.rest-2.16.omod");
        assertFileNotPresent(testServerId, "openmrs-2.0.2.war");
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
        testServerId = setupTestServer("referenceapplication:2.2");
        addAnswer(testServerId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.3.1");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.11.5.war");
        Properties properties = PropertiesUtils.loadPropertiesFromResource("openmrs-distro-2.3.1.properties");
        DistroProperties distroProperties = new DistroProperties(properties);
        assertOnlyModulesInstalled(testServerId, distroProperties);
        assertPlatformUpdated(testServerId, "1.11.5");
        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.3.1"));
    }

    @Test
    public void deploy_shouldDowngradeDistroTo2_1() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        addAnswer(testServerId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.1");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "openmrs-1.10.0.war");
        Properties properties = PropertiesUtils.loadPropertiesFromResource("openmrs-distro-2.1.properties");
        DistroProperties distroProperties = new DistroProperties(properties);
        assertOnlyModulesInstalled(testServerId, distroProperties);
        assertPlatformUpdated(testServerId, "1.10.0");
        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.1"));
    }

    @Test
    public void deploy_shouldUpgradeDistroTo2_13_0() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.3.1");
        addAnswer(testServerId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.13.0");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "openmrs-2.5.9.war");
        Properties properties = PropertiesUtils.loadPropertiesFromResource("integration-test/distributions/referenceapplication-package-2.13.0.properties");
        if ("2.2.6".equals(properties.get("omod.atlas"))) {
            properties.setProperty("omod.atlas", "2.2.7");
        }
        DistroProperties distroProperties = new DistroProperties(properties);
        assertOnlyModulesInstalled(testServerId, distroProperties);
        assertPlatformUpdated(testServerId, "2.5.9");
        assertFilePresent(testServerId, "owa");
        assertFilePresent(testServerId, "owa", "SystemAdministration.owa");
        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("2.13.0"));
    }

    @Test
    public void deploy_shouldUpgradeDistroTo3_0_0() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        addAnswer(testServerId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:3.0.0");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "openmrs-2.6.7.war");
        Properties properties = PropertiesUtils.loadPropertiesFromResource("integration-test/distributions/distro-emr-configuration-3.0.0.properties");

        assertNumFilesPresent(24, Paths.get(testServerId, "modules"), null, ".omod");
        assertPlatformUpdated(testServerId, "2.6.7");
        assertNumFilesPresent(0, Paths.get(testServerId, "owa"), null, null);

        // Spa module added
        assertFalse(properties.containsKey("omod.spa"));
        assertFileContains("omod.spa", testServerId, "openmrs-distro.properties");
        assertNumFilesPresent(1, Paths.get(testServerId, "modules"), "spa-", ".omod");

        assertFilePresent(testServerId, "frontend");
        assertFilePresent(testServerId, "frontend", "index.html");
        assertFilePresent(testServerId, "frontend", "importmap.json");

        assertFilePresent(testServerId, "configuration");
        assertFilePresent(testServerId, "configuration", "conceptclasses", "conceptclasses-core_data.csv");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(testServerId);
        assertThat(server, serverHasVersion("3.0.0"));

        assertLogContains("+ Adds frontend spa");
        assertLogContains("+ Adds frontend configuration");
    }

    @Test
    public void deploy_shouldUpgradeDistroWithConfigPackage() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        includeDistroPropertiesFile("openmrs-distro-configuration.properties");
        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "configuration", "addresshierarchy", "addressConfiguration-core_demo.xml");
        assertFilePresent(testServerId, "configuration", "addresshierarchy", "addresshierarchy-core_demo.csv");
        assertFilePresent(testServerId, "configuration", "conceptclasses", "conceptclasses-core_data.csv");
        assertFilePresent(testServerId, "configuration", "encountertypes", "encountertypes_core-demo.csv");
        assertLogContains("+ Adds frontend configuration");
    }

    @Test
    public void deploy_shouldUpgradeDistroWithContentPackage() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        includeDistroPropertiesFile("openmrs-distro-content-package.properties");
        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "configuration", "conceptclasses", "hiv", "conceptclasses.csv");
        assertFilePresent(testServerId, "configuration", "conceptsources", "hiv", "conceptsources.csv");
        assertFilePresent(testServerId, "configuration", "encountertypes", "hiv", "encountertypes.csv");
        assertLogContains("+ Adds frontend configuration");
    }

    @Test
    public void deploy_shouldReplaceConfigurationAndContentIfChanged() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");

        includeDistroPropertiesFile("openmrs-distro-configuration.properties");
        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFilePresent(testServerId, "configuration", "addresshierarchy", "addressConfiguration-core_demo.xml");
        assertFilePresent(testServerId, "configuration", "addresshierarchy", "addresshierarchy-core_demo.csv");
        assertFilePresent(testServerId, "configuration", "conceptclasses", "conceptclasses-core_data.csv");
        assertFilePresent(testServerId, "configuration", "encountertypes", "encountertypes_core-demo.csv");
        assertLogContains("+ Adds frontend configuration");

        includeDistroPropertiesFile("openmrs-distro-content-package.properties");
        addAnswer(testServerId);
        addAnswer("y");
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertFileNotPresent(testServerId, "configuration", "addresshierarchy", "addressConfiguration-core_demo.xml");
        assertFileNotPresent(testServerId, "configuration", "addresshierarchy", "addresshierarchy-core_demo.csv");
        assertFileNotPresent(testServerId, "configuration", "conceptclasses", "conceptclasses-core_data.csv");
        assertFileNotPresent(testServerId, "configuration", "encountertypes", "encountertypes_core-demo.csv");
        assertFilePresent(testServerId, "configuration", "conceptclasses", "hiv", "conceptclasses.csv");
        assertFilePresent(testServerId, "configuration", "conceptsources", "hiv", "conceptsources.csv");
        assertFilePresent(testServerId, "configuration", "encountertypes", "hiv", "encountertypes.csv");
        assertLogContains("^ Updates frontend configuration");
    }

    @Test
    public void deploy_shouldUpgradeDistroFromDistroProperties() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);
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
        testServerId = setupTestServer("referenceapplication:2.2");
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
        testServerId = setupTestServer("referenceapplication:2.2");
        includePomFile("deployIT", "pom-owa-module.xml");
        addAnswer(testServerId);
        addAnswer("y");
        executeTask("deploy");
        assertSuccess();
        assertModulesInstalled(testServerId, "owa-1.4.omod");
        assertModuleUpdated(testServerId, "owa", "1.4");
    }

    @Test
    public void deploy_shouldInstallOwaAndOwaModule() throws Exception {
        testServerId = setupTestServer("referenceapplication:2.2");
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
