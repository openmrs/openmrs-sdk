package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.ConfigurationInstaller;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DeployTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Deploy deploy;

    @Before
    public void setUp() {
        deploy = new Deploy();
        deploy.distroHelper = mock(DistroHelper.class);
        deploy.configurationInstaller = mock(ConfigurationInstaller.class);
        deploy.contentHelper = mock(ContentHelper.class);
        deploy.wizard = mock(Wizard.class);
        deploy.versionsHelper = mock(org.openmrs.maven.plugins.utility.VersionsHelper.class);
    }

    @Test
    public void executeTask_shouldThrowWhenConfigOnlyAndPlatformBothSpecified() throws Exception {
        setField("configOnly", true);
        setField("platform", "2.5.9");
        expectedException.expect(org.apache.maven.plugin.MojoExecutionException.class);
        deploy.executeTask();
    }

    @Test
    public void executeTask_shouldThrowWhenConfigOnlyAndOwaBothSpecified() throws Exception {
        setField("configOnly", true);
        setField("owa", "some-owa:1.0");
        expectedException.expect(org.apache.maven.plugin.MojoExecutionException.class);
        deploy.executeTask();
    }

    @Test
    public void executeTask_shouldThrowWhenConfigOnlyAndArtifactIdBothSpecified() throws Exception {
        setField("configOnly", true);
        setField("artifactId", "some-module");
        expectedException.expect(org.apache.maven.plugin.MojoExecutionException.class);
        deploy.executeTask();
    }

    @Test
    public void deployConfigOnly_shouldInstallConfigAndContentAndUpdateServerTracking() throws Exception {
        Properties serverProps = new Properties();
        serverProps.put("config.old-config", "1.0.0");
        serverProps.put("content.old-content", "1.0.0");
        Server server = serverWithProperties(serverProps);

        Properties distroProps = new Properties();
        distroProps.put("config.referenceapplication", "3.0.0");
        distroProps.put("config.referenceapplication.groupId", "org.openmrs.distro");
        distroProps.put("content.hiv", "1.0.0");
        distroProps.put("content.hiv.groupId", "org.openmrs.content");
        Distribution distribution = distributionFromProperties(distroProps);

        setField("distro", "some:distro:1.0");
        when(deploy.distroHelper.resolveDistributionForStringSpecifier(eq("some:distro:1.0"), any()))
                .thenReturn(distribution);

        deploy.deployConfigOnly(server);

        verify(deploy.configurationInstaller).installToServer(eq(server), any(DistroProperties.class));
        verify(deploy.contentHelper).installBackendConfig(any(DistroProperties.class), any(File.class));
        assertThat(server.getConfigArtifacts(), hasSize(1));
        assertThat(server.getContentPackageArtifacts(), hasSize(1));
    }

    @Test
    public void deployConfigOnly_shouldSkipInstallWhenDistroHasNoConfigOrContent() throws Exception {
        Server server = serverWithProperties(new Properties());

        Properties distroProps = new Properties();
        distroProps.put("war.openmrs", "2.5.9");
        Distribution distribution = distributionFromProperties(distroProps);

        setField("distro", "some:distro:1.0");
        when(deploy.distroHelper.resolveDistributionForStringSpecifier(eq("some:distro:1.0"), any()))
                .thenReturn(distribution);

        deploy.deployConfigOnly(server);

        verifyNoInteractions(deploy.configurationInstaller);
        verifyNoInteractions(deploy.contentHelper);
    }

    @Test
    public void deployConfigOnly_shouldThrowWhenNoDistroCanBeResolved() throws Exception {
        Server server = serverWithProperties(new Properties());
        when(deploy.distroHelper.getDistroPropertiesFileFromDir()).thenReturn(null);

        expectedException.expect(org.apache.maven.plugin.MojoExecutionException.class);
        deploy.deployConfigOnly(server);
    }

    // --- helpers ---

    private Server serverWithProperties(Properties props) throws Exception {
        File dir = tempFolder.newFolder();
        new File(dir, DistroProperties.DISTRO_FILE_NAME).createNewFile();
        return new Server(dir, props);
    }

    private Distribution distributionFromProperties(Properties props) {
        Distribution distribution = new Distribution();
        distribution.setEffectiveProperties(new DistroProperties(props));
        return distribution;
    }

    private void setField(String name, Object value) throws Exception {
        Field f = Deploy.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(deploy, value);
    }
}
