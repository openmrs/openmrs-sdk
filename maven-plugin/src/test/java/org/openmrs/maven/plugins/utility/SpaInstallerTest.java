package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class SpaInstallerTest {

    SpaInstaller spaInstaller;

    @Mock
    DistroHelper distroHelper;

    @Mock
    NodeHelper nodeHelper;

    File appDataDir;

    @Before
    public void setup() throws Exception{
        appDataDir = new File(ResourceExtractor.simpleExtractResources(getClass(), "/test-tmp"), "server1");
        appDataDir.mkdirs();
        spaInstaller = new SpaInstaller(distroHelper, nodeHelper);
    }

    @After
    public void tearDown() throws Exception {
        appDataDir.delete();
    }

    @Test
    public void spaInstall_shouldParseSingleConfigUrlCorrectly() throws MojoExecutionException, MojoFailureException, IOException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.configUrls", "foo");
        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));
        File spaConfigFile = new File(appDataDir, "spa-build-config.json");
        String spaConfig = FileUtils.readFileToString(spaConfigFile);
        Assert.assertThat(spaConfig, Matchers.containsString("\"configUrls\":[\"foo\"]"));
    }

    @Test
    public void spaInstall_shouldParseConfigUrlsCorrectly() throws MojoExecutionException, MojoFailureException, IOException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.configUrls", "foo,bar,baz");
        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));
        File spaConfigFile = new File(appDataDir, "spa-build-config.json");
        String spaConfig = FileUtils.readFileToString(spaConfigFile);
        Assert.assertThat(spaConfig, Matchers.containsString("\"configUrls\":[\"foo\",\"bar\",\"baz\"]"));
    }
}
