package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyEqualTo;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyThatContains;
import static org.openmrs.maven.plugins.SdkMatchers.hasPropertyThatNotContains;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasName;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasDebugPort;
import static org.openmrs.maven.plugins.SdkMatchers.serverHasVersion;

public class SetupIntegrationTest extends AbstractSdkIntegrationTest {

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
        assertFilePresent(serverId + File.separator + "openmrs-1.11.5.war");
        assertFilePresent(serverId + File.separator + "modules");
        assertFileNotPresent(serverId + File.separator + "tmp");

        DistroProperties distroProperties = new DistroProperties("2.3.1");
        assertModulesInstalled(serverId, distroProperties);

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.3.1"));
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
        assertFilePresent(serverId + File.separator + "openmrs-1.11.5.war");
        assertFilePresent(serverId + File.separator + "h2-1.4.190.jar");
        assertFileNotPresent(serverId + File.separator + "modules");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("1.0"));
        assertFilePresent(serverId + File.separator + "openmrs-distro.properties");
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
        assertFilePresent(serverId + File.separator + "openmrs-2.0.2.war");
        assertFilePresent(serverId + File.separator + "modules");

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasVersion("2.0.2"));
        assertFilePresent(serverId + File.separator + "openmrs-distro.properties");
        assertThat(server, serverHasDebugPort("1044"));
        assertThat(server, hasModuleVersion("webservices.rest", "2.16"));
    }

    @Test
    public void setup_shouldInstallServerFromGivenDistroProperties() throws Exception{
        String serverId = UUID.randomUUID().toString();

        addTaskParam("distro", testDirectory.getAbsolutePath() + File.separator + "openmrs-distro.properties");
        addTaskParam("debug", "1044");
        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("nameValue");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId + File.separator + "openmrs-1.11.5.war");
        assertFilePresent(serverId + File.separator + "modules");
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
        addTaskParam("serverId", serverId);
        addTaskParam("debug", "1044");
        addMockDbSettings();

        addAnswer("OpenMRS Concepts OWA server 1.0 from current directory");
        addAnswer("nameValue");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");

        assertSuccess();
        assertServerInstalled(serverId);
        assertFilePresent(serverId + File.separator + "openmrs-1.11.5.war");
        assertFilePresent(serverId + File.separator + "modules");
        assertModulesInstalled(serverId, "owa-1.4.omod", "uicommons-1.7.omod", "uiframework-3.6.omod");
        assertFilePresent(serverId + File.separator + "frontend");
        String indexFilePath = serverId + File.separator + "frontend" + File.separator + "index.html";
        assertFilePresent(indexFilePath);
        assertFilePresent(serverId + File.separator + "frontend" + File.separator + "importmap.json");
        assertFilePresent(serverId + File.separator + "frontend" + File.separator + "openmrs-esm-login-app-3.1.0");
        assertFilePresent(serverId + File.separator + "frontend" + File.separator + "openmrs-esm-patient-chart-app-3.0.0");
        String indexContents = FileUtils.readFileToString(new File(testDirectory.getAbsolutePath(), indexFilePath));
        assertThat(indexContents, containsString("apiUrl: \"notopenmrs\""));

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

        addMockDbSettings();

        addAnswer(serverId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.2");
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
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

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");
        assertThat(server, hasPropertyThatContains("version", "SNAPSHOT"));

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
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

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");
        assertThat(server, hasPropertyThatContains("version", "SNAPSHOT"));

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
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

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");
        assertThat(server, hasPropertyThatNotContains("version", "SNAPSHOT"));

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
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

        assertFilePresent(serverId + File.separator + "openmrs-server.properties");
        assertThat(server, hasPropertyThatNotContains("version", "SNAPSHOT"));

        File serverPropertiesFile = new File(testDirectory.getAbsolutePath() + File.separator + serverId, "openmrs-server.properties");
        String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
        assertThat(javaHomeServerProperty, is(nullValue()));
    }

    @Test
    public void setup_shouldInstallServerWithGivenJavaHomeAndAddJavaHomeToSdkProperties() throws Exception{
		String customJavaHome = System.getProperty("java.home");

		String serverId = UUID.randomUUID().toString();

        addTaskParam("javaHome", customJavaHome);

        addAnswer(serverId);
        addAnswer("Distribution");
        addAnswer("referenceapplication:2.2");
        addAnswer("8080");
        addAnswer("1044");

        addMockDbSettings();

        executeTask("setup");
        assertSuccess();
        assertServerInstalled(serverId);

		assertFilePresent(serverId + File.separator + "openmrs-server.properties");

		File serverPropertiesFile = new File(testDirectory.getAbsoluteFile(), serverId + File.separator + "openmrs-server.properties");
		String javaHomeServerProperty = readValueFromPropertyKey(serverPropertiesFile, "javaHome");
		assertThat(javaHomeServerProperty, is(customJavaHome));

		assertFilePresent("sdk.properties");
		File sdkProperties = new File(testDirectory.getAbsoluteFile(), "sdk.properties");
		String javaHomes = readValueFromPropertyKey(sdkProperties, "javaHomeOptions");
		assertThat(javaHomes, is(customJavaHome));

        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, serverHasDebugPort("1044"));
    }

    @Test
    public void setup_shouldInstallWithParentDistroSpecifiedInDistroProperties() throws Exception{
        addTaskParam("distro", testDirectory.toString() + File.separator + "parent-distro-properties" + File.separator + "openmrs-distro.properties");
        addMockDbSettings();

        String serverId = UUID.randomUUID().toString();
        addAnswer(serverId);
        addAnswer("1044");
        addAnswer("8080");

        executeTask("setup");

        assertFilePresent( serverId + File.separator + "openmrs-2.0.1.war");
        assertModulesInstalled(serverId, "htmlformentry-3.3.1.omod");
        assertFileNotPresent(serverId + File.separator + "modules" + File.separator + "htmlformentry-3.3.0.omod");
    }

    private String readValueFromPropertyKey(File propertiesFile, String key) throws Exception {

        InputStream in = new FileInputStream(propertiesFile);
        Properties sdkProperties = new Properties();
        sdkProperties.load(in);

        return sdkProperties.getProperty(key);
    }
}
