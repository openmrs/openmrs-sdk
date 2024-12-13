package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.PropertiesUtils;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyEqualTo;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyThatContains;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyThatNotContains;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasDebugPort;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasName;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class SetupIT extends AbstractSdkIT {

    @Test
    public void setup_shouldInstallRefapp2_3_1() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("distro", "referenceapplication:2.3.1");
        addMockDbSettings();
        addAnswer(serverId);
        addAnswer("8080");
        addAnswer("1044");
        addAnswer(System.getProperty("java.home"));

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-1.11.5.war");
        assertFilePresent(serverId, "modules");
        assertFileNotPresent(serverId, "tmp");

        Properties properties = PropertiesUtils.loadPropertiesFromResource("openmrs-distro-2.3.1.properties");
        DistroProperties distroProperties = new DistroProperties(properties);
        assertOnlyModulesInstalled(serverId, distroProperties);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.3.1"));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallRefapp2_13_0() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("distro", "referenceapplication:2.13.0");
        addMockDbSettings();
        addAnswer(serverId);
        addAnswer("8080");
        addAnswer("1044");
        addAnswer(System.getProperty("java.home"));

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-2.5.9.war");
        assertFilePresent(serverId, "modules");
        assertFileNotPresent(serverId, "tmp");

        Properties properties = PropertiesUtils.loadPropertiesFromResource("integration-test/distributions/referenceapplication-package-2.13.0.properties");
        assertThat(properties.get("omod.atlas"), equalTo("2.2.6"));
        properties.put("omod.atlas", "2.2.7");
        DistroProperties distroProperties = new DistroProperties(properties);
        assertOnlyModulesInstalled(serverId, distroProperties);

        assertNumFilesPresent(42, Paths.get(serverId, "modules"), null, ".omod");

        assertFilePresent(serverId, "owa");
        assertFilePresent(serverId, "owa", "SystemAdministration.owa");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.13.0"));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallRefapp3_0_0() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("distro", "referenceapplication:3.0.0");
        addMockDbSettings();
        addAnswer(serverId);
        addAnswer("8080");
        addAnswer("1044");
        addAnswer(System.getProperty("java.home"));

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-2.6.7.war");
        assertFilePresent(serverId, "modules");

        assertNumFilesPresent(24, Paths.get(serverId, "modules"), null, ".omod");
        assertNumFilesPresent(0, Paths.get(serverId, "owa"), null, null);

        assertFilePresent(serverId, "frontend");
        assertFilePresent(serverId, "frontend", "index.html");
        assertFilePresent(serverId, "frontend", "importmap.json");

        assertFilePresent(serverId, "configuration");
        assertFilePresent(serverId, "configuration", "conceptclasses", "conceptclasses-core_data.csv");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("3.0.0"));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallPlatform1_11_5() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("platform", "1.11.5");
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("8080");
        addAnswer("1044");
        addAnswer(System.getProperty("java.home"));

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-1.11.5.war");
        assertFilePresent(serverId,  "h2-1.4.190.jar");
        assertFileNotPresent(serverId, "modules");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("1.0"));
        assertFilePresent(serverId, DistroProperties.DISTRO_FILE_NAME);
        assertThat(server, serverHasName(serverId));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallDistroPlatform2_0_2() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("platform", "2.0.2");
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("8080");
        addAnswer("1044");
        addAnswer(System.getProperty("java.home"));

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-2.0.2.war");
        assertFilePresent(serverId, "modules");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.0.2"));
        assertFilePresent(serverId, DistroProperties.DISTRO_FILE_NAME);
        assertThat(server, serverHasDebugPort("1044"));
        assertThat(server, hasModuleVersion("webservices.rest", "2.16"));
    }

    @Test
    public void setup_shouldInstallServerFromGivenDistroProperties() throws Exception{
        String serverId = UUID.randomUUID().toString();
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);

        addTaskParam("distro", testDirectory.getAbsolutePath() + File.separator + "openmrs-distro.properties");
        addTaskParam("debug", "1044");
        addTaskParam("ignorePeerDependencies", "false");
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("nameValue");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-1.11.5.war");
        assertFilePresent(serverId, "modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasName("OpenMRS Concepts OWA server"));
        assertThat(server, hasPropertyEqualTo("test", "testValue"));
        assertThat(server, hasPropertyEqualTo("pih.default.config", "nameValue"));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallServerFromDistroPropertiesDir() throws Exception{
        String serverId = UUID.randomUUID().toString();
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);

        addTaskParam("serverId", serverId);
        addTaskParam("debug", "1044");
        addTaskParam("ignorePeerDependencies", "false");
        addMockDbSettings();

        addAnswer("OpenMRS Concepts OWA server 1.0 from current directory");
        addAnswer("nameValue");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId, "openmrs-1.11.5.war");
        assertFilePresent(serverId, "modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasName("OpenMRS Concepts OWA server"));
        assertThat(server, hasPropertyEqualTo("test", "testValue"));
        assertThat(server, hasPropertyEqualTo("pih.default.config", "nameValue"));
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallServerWithDefaultJavaHome() throws Exception{
        String serverId = UUID.randomUUID().toString();
        addTaskParam("debug", "1044");
        addTaskParam("ignorePeerDependencies", "false");

        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("Platform");
        addAnswer("2.6.1");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
	}

    @Test
    public void setup_shouldInstallServerWithSpecifiedLatestSnapshotDistroVersionByUsingKeywordInBatchMode() throws Exception {
        String keyword = "LATEST-SNAPSHOT";

        String serverId = UUID.randomUUID().toString();
        addTaskParam("debug", "1044");
        addTaskParam("platform", keyword);
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        assertThat(server, hasPropertyThatContains("version", "SNAPSHOT"));

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
    }

    @Test
    public void setup_shouldInstallServerWithSpecifiedLatestSnapshotDistroVersionByUsingKeywordInInteractiveMode() throws Exception {
        String keyword = "LATEST-SNAPSHOT";

        String serverId = UUID.randomUUID().toString();
        addTaskParam("debug", "1044");

        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("Platform");
        addAnswer(keyword);
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        assertThat(server, hasPropertyThatContains("version", "SNAPSHOT"));

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
    }

    @Test
    public void setup_shouldInstallServerWithSpecifiedLatestReleasedDistroVersionByUsingKeywordInBatchMode() throws Exception {
        String keyword = "LATEST";

        String serverId = UUID.randomUUID().toString();
        addTaskParam("debug", "1044");
        addTaskParam("platform", keyword);
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        assertThat(server, hasPropertyThatNotContains("version", "SNAPSHOT"));

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
    }

    @Test
    public void setup_shouldInstallServerWithSpecifiedLatestReleasedDistroVersionByUsingKeywordInInteractiveMode() throws Exception {
        String keyword = "LATEST";

        String serverId = UUID.randomUUID().toString();
        addTaskParam("debug", "1044");

        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("Platform");
        addAnswer(keyword);
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        assertThat(server, hasPropertyThatNotContains("version", "SNAPSHOT"));

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
    }

    @Test
    public void setup_shouldInstallServerWithGivenJavaHomeAndAddJavaHomeToSdkProperties() throws Exception{
		String customJavaHome = System.getProperty("java.home");

		String serverId = UUID.randomUUID().toString();

        addTaskParam("javaHome", customJavaHome);
        addTaskParam("ignorePeerDependencies", "false");

        addAnswer(serverId);
        addAnswer("Platform");
        addAnswer("2.6.1");
        addAnswer("8080");
        addAnswer("1044");

        addMockDbSettings();

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);

        File serverPropertiesFile = getTestFile(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
		String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
		assertThat(javaHomeServerProperty, is(customJavaHome));

		assertFilePresent(SDKConstants.OPENMRS_SDK_PROPERTIES);
		File sdkProperties = getTestFile(SDKConstants.OPENMRS_SDK_PROPERTIES);
		String javaHomes = readValueFromPropertyKey(sdkProperties, "javaHomeOptions");
		assertThat(javaHomes, is(customJavaHome));

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallWithParentDistroSpecifiedInDistroProperties() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-parent-as-distro.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId, "openmrs-2.0.5.war");
        assertModulesInstalled(serverId, "htmlformentry-3.3.1.omod");
        assertFileNotPresent(serverId, "modules", "htmlformentry-3.10.0.omod");
    }

    @Test
    public void setup_shouldInstallWithSpaSpecifiedAsBuildProperties() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-spa-build.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId, "openmrs-1.11.5.war");
        assertModulesInstalled(serverId, "spa-2.0.0.omod");

        assertFilePresent(serverId, "frontend");
        Path indexFilePath = testDirectoryPath.resolve(Paths.get(serverId, "frontend", "index.html"));
        assertPathPresent(indexFilePath);
        assertFilePresent(serverId, "frontend", "importmap.json");
        assertFilePresent(serverId,  "frontend", "openmrs-esm-login-app-5.6.0");
        assertFilePresent(serverId, "frontend", "openmrs-esm-patient-chart-app-8.0.0");
        String indexContents = FileUtils.readFileToString(getTestFile(indexFilePath), StandardCharsets.UTF_8);
        assertThat(indexContents, containsString("apiUrl: \"notopenmrs\""));
        assertThat(indexContents, containsString("configUrls: [\"foo\"]"));
    }

    @Test
    public void setup_shouldInstallWithSpaSpecifiedAsMavenArtifacts() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-spa-artifacts.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId, "openmrs-2.6.9.war");
        assertModulesInstalled(serverId, "spa-2.0.0.omod");
        assertFilePresent(serverId, "frontend", "index.html");
        assertFileContains("@openmrs/esm-dispensing-app", serverId, "frontend", "importmap.json");
    }

    @Test
    public void setup_shouldInstallWithContentPackage() throws Exception {
        setupContentPackage("testpackage2");
        includeDistroPropertiesFile("openmrs-distro-content-package.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId, "openmrs-2.6.9.war");
        assertFilePresent(serverId, "configuration", "globalproperties", "testpackage2", "gp.xml");
        assertFilePresent(serverId, "configuration", "patientidentifiertypes", "testpackage2", "patientidentifiertypes.csv");
    }

    @Test
    public void setup_shouldInstallWithContentPackageWithNoNamespace() throws Exception {
        setupContentPackage("testpackage2");
        includeDistroPropertiesFile("openmrs-distro-content-package-no-namespace.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId, "openmrs-2.6.9.war");
        assertFilePresent(serverId, "configuration", "globalproperties", "gp.xml");
        assertFilePresent(serverId, "configuration", "patientidentifiertypes", "patientidentifiertypes.csv");
    }

    @Test
    public void testSetupWithMissingContentDependencies() throws Exception {
        setupContentPackage("testpackage1");

        includeDistroPropertiesFile("openmrs-distro-content-package-missing-dependencies.properties");
        addTaskParam("distro", testDirectory.toString() + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        Exception exception = null;
        try {
            executeTask("setup");
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertLogContains("requires module org.openmrs.module:fhir2-omod version >=1.8.0");
        assertLogContains("requires module org.openmrs.module:webservices.rest-omod version >=2.42.0");
        assertLogContains("requires module org.openmrs.module:openconceptlab-omod version >=2.3.0");
        assertLogContains("requires module org.openmrs.module:initializer-omod version >=2.5.2");
        assertLogContains("requires module org.openmrs.module:o3forms-omod version >=2.2.0");
        assertLogContains("requires esm @openmrs/esm-patient-chart-app version >=8.1.0");
    }

    private String readValueFromPropertyKey(File propertiesFile, String key) throws Exception {
        Properties sdkProperties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile.toPath())) {
            sdkProperties.load(in);
        }
        return sdkProperties.getProperty(key);
    }
}
