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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    public void spaInstall_shouldCreateBuildAndAssembleCommandWithSingleConfigFile() throws MojoExecutionException {
        Properties distroProperties = new Properties();
        distroProperties.setProperty("spa.configUrls", "foo");
        distroProperties.setProperty("spa.configUrls2", "bar");
        distroProperties.setProperty("spa.configUrls3", "baz");

        spaInstaller.installFromDistroProperties(appDataDir, new DistroProperties(distroProperties));

        Mockito.verify(nodeHelper).installNodeAndNpm("16.15.0", "8.5.5", false);
        Mockito.verify(nodeHelper).runNpx("openmrs@next --version", "");
        Mockito.verify(nodeHelper).runNpx(
                "openmrs@next build --target " + appDataDir.getAbsolutePath() + "/frontend --build-config " + appDataDir.getAbsolutePath() + "/spa-build-config.json" ,
                ""
        );

        Mockito.verify(nodeHelper).runNpx(
                "openmrs@next assemble --target " + appDataDir.getAbsolutePath() + "/frontend --mode config --config " + appDataDir.getAbsolutePath() + "/spa-build-config.json" ,
                ""
        );
    }
}
