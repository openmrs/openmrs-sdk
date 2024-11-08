package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.BaseSdkProperties;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SpaInstallerTest {

    SpaInstaller spaInstaller;

    @Mock
    DistroHelper distroHelper;

    @Mock
    ModuleInstaller moduleInstaller;

    @Mock
    Wizard wizard;

    @Mock
    NodeHelper nodeHelper;

    File appDataDir;

    @Before
    public void setup() throws Exception {
        appDataDir = new File(ResourceExtractor.simpleExtractResources(getClass(), "/test-tmp"), "server1");
        appDataDir.mkdirs();
        spaInstaller = new SpaInstaller(distroHelper, nodeHelper, moduleInstaller, wizard);
    }

    @After
    public void tearDown() {
        appDataDir.delete();
    }

    @Test
    public void spaInstall_shouldParseSingleConfigUrlCorrectly() throws MojoExecutionException, IOException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.configUrls", "foo");
        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));
        File spaConfigFile = new File(appDataDir, "spa-build-config.json");
        String spaConfig = FileUtils.readFileToString(spaConfigFile, StandardCharsets.UTF_8);
        Assert.assertThat(spaConfig, Matchers.containsString("\"configUrls\":[\"foo\"]"));
    }

    @Test
    public void spaInstall_shouldParseConfigUrlsCorrectly() throws MojoExecutionException, IOException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.configUrls", "foo,bar,baz");
        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));
        File spaConfigFile = new File(appDataDir, "spa-build-config.json");
        String spaConfig = FileUtils.readFileToString(spaConfigFile, StandardCharsets.UTF_8);
        Assert.assertThat(spaConfig, Matchers.containsString("\"configUrls\":[\"foo\",\"bar\",\"baz\"]"));
    }

    @Test
    public void spaInstall_shouldSupportConfiguringMavenArtifact() throws MojoExecutionException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.artifactId", "openmrs-frontend-example");
        distroProperties.setProperty("spa.groupId", "org.openmrs.frontend");
        distroProperties.setProperty("spa.version", "1.2.3");
        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));

        String expectedOutputDir = new File(appDataDir, "frontend").getAbsolutePath();

        ArgumentCaptor<String> wizardMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(wizard, atLeast(3)).showMessage(wizardMessageCaptor.capture());

        ArgumentCaptor<Artifact> artifactCaptor = ArgumentCaptor.forClass(Artifact.class);
        ArgumentCaptor<File> targetDirectoryCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<String> sourceDirCaptor = ArgumentCaptor.forClass(String.class);
        verify(moduleInstaller, times(1)).installAndUnpackModule(artifactCaptor.capture(), targetDirectoryCaptor.capture(), sourceDirCaptor.capture());
        Artifact artifact = artifactCaptor.getValue();
        assertThat(artifact.getArtifactId(), equalTo("openmrs-frontend-example"));
        assertThat(artifact.getGroupId(), equalTo("org.openmrs.frontend"));
        assertThat(artifact.getVersion(), equalTo("1.2.3"));
        assertThat(artifact.getType(), equalTo(BaseSdkProperties.TYPE_ZIP));
        assertThat(targetDirectoryCaptor.getValue().getAbsolutePath(), equalTo(expectedOutputDir));

        // Validate that the build and install from node process did not run
        verifyNoInteractions(nodeHelper);
    }
}
