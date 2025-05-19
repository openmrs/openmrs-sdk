package org.openmrs.maven.plugins.utility;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(BlockJUnit4ClassRunner.class)
public class GitHubPackagesHelperIT {

    @Mock
    private MavenEnvironment mavenEnvironment;
    
    @Mock
    private MavenSession mavenSession;
    
    private Settings settings;
    private GitHubPackagesHelper helper;
    private File testDir;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        testDir = new File("target/test-maven-home");
        testDir.mkdirs();
        
        settings = new Settings();
        settings.setLocalRepository(new File(testDir, "repository").getAbsolutePath());
        
        when(mavenEnvironment.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.getSettings()).thenReturn(settings);
        
        helper = new GitHubPackagesHelper(mavenEnvironment);
        
        // Clean up any existing settings
        Settings settings = getSettingsFromHelper();
        settings.getServers().removeIf(s -> s.getId().equals("github"));
        settings.getProfiles().removeIf(p -> p.getId().equals("github"));
        settings.getActiveProfiles().remove("github");
    }

    @Test
    public void configureGitHubPackages_shouldConfigureAuthenticationAndRepository() throws Exception {
        Properties props = new Properties();
        props.setProperty("test.github.packages.username", "testuser");
        props.setProperty("test.github.packages.token", "testtoken");
        props.setProperty("test.github.packages.owner", "testowner");
        props.setProperty("test.github.packages.repository", "testrepo");
        DistroProperties distroProperties = new DistroProperties(props);

        helper.configureGitHubPackages(distroProperties);

        Settings settings = getSettingsFromHelper();
        Server server = settings.getServers().stream().filter(s -> "github".equals(s.getId())).findFirst().orElse(null);
        assertNotNull(server);
        assertEquals("testuser", server.getUsername());
        assertEquals("testtoken", server.getPassword());
    }

    @Test
    public void configureGitHubPackages_shouldHandleEmptyConfigurations() throws Exception {
        Properties props = new Properties();
        DistroProperties distroProperties = new DistroProperties(props);

        helper.configureGitHubPackages(distroProperties);

        Settings settings = getSettingsFromHelper();
        Server server = settings.getServers().stream().filter(s -> "github".equals(s.getId())).findFirst().orElse(null);
        assertNull(server);
    }

    @Test
    public void configureGitHubPackages_shouldSkipIncompleteConfigurations() throws Exception {
        Properties props = new Properties();
        props.setProperty("test.github.packages.username", "testuser");
        DistroProperties distroProperties = new DistroProperties(props);

        helper.configureGitHubPackages(distroProperties);

        Settings settings = getSettingsFromHelper();
        Server server = settings.getServers().stream().filter(s -> "github".equals(s.getId())).findFirst().orElse(null);
        assertNull(server);
    }

    @Test
    public void configureGitHubPackages_shouldHandleMultiplePackages() throws Exception {
        Properties props = getProperties();
        DistroProperties distroProperties = new DistroProperties(props);

        helper.configureGitHubPackages(distroProperties);

        Settings settings = getSettingsFromHelper();
        Server server = settings.getServers().stream().filter(s -> "github".equals(s.getId())).findFirst().orElse(null);
        assertNotNull(server);
        assertEquals("user2", server.getUsername());
        assertEquals("token2", server.getPassword());
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("package1.github.packages.username", "user1");
        props.setProperty("package1.github.packages.token", "token1");
        props.setProperty("package1.github.packages.owner", "owner1");
        props.setProperty("package1.github.packages.repository", "repo1");
        props.setProperty("package2.github.packages.username", "user2");
        props.setProperty("package2.github.packages.token", "token2");
        props.setProperty("package2.github.packages.owner", "owner2");
        props.setProperty("package2.github.packages.repository", "repo2");
        return props;
    }

    private Settings getSettingsFromHelper() throws Exception {
        Field settingsManagerField = GitHubPackagesHelper.class.getDeclaredField("settingsManager");
        settingsManagerField.setAccessible(true);
        SettingsManager settingsManager = (SettingsManager) settingsManagerField.get(helper);
        return settingsManager.getSettings();
    }
} 