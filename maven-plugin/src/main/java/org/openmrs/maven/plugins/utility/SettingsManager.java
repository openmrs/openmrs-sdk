package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Configure settings.xml
 */
public class SettingsManager {

    private Settings settings;

    private File settingsFile;

    public SettingsManager() {
        settings = new Settings();
    }

    public SettingsManager(InputStream stream) throws MojoExecutionException {
        this();
        SettingsXpp3Reader reader = new SettingsXpp3Reader();
        try {
            settings = reader.read(stream);
            stream.close();
        } catch (IOException | XmlPullParserException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Returns Settings from settings.xml in maven home
     *
     * @param mavenSession
     * @throws MojoExecutionException
     */
    public SettingsManager(MavenSession mavenSession) throws MojoExecutionException {
        String localRepository = mavenSession.getSettings().getLocalRepository();
        File mavenHome = new File(localRepository).getParentFile();
        mavenHome.mkdirs();
        settingsFile = new File(mavenHome, SDKConstants.MAVEN_SETTINGS);
        try {
            if (settingsFile.exists()) {
                try (InputStream in = Files.newInputStream(settingsFile.toPath())) {
                    settings = new SettingsXpp3Reader().read(in);
                }
            } else {
                //this machine doesn't have any settings yet, create new...
                settings = new Settings();
                settingsFile.createNewFile();
                try (OutputStream emptySettings = Files.newOutputStream(settingsFile.toPath())) {
                    apply(emptySettings);
                }
            }
        } catch (IOException|XmlPullParserException e) {
            throw new MojoExecutionException("Failed to load settings.xml",e);
        }
    }

    /**
     * Update settings to default settings
     * @param other
     */
    public void updateSettings(Settings other) {
        if (settings == null) {
            settings = new Settings();
        }
        String pluginGroup = other.getPluginGroups().get(0);
        if (settings.getPluginGroups() == null) {
            settings.setPluginGroups(new ArrayList<String>());
        }
        if (!settings.getPluginGroups().contains(pluginGroup)) {
            settings.addPluginGroup(pluginGroup);
        }
        String activeProfile = other.getActiveProfiles().get(0);
        if (settings.getActiveProfiles() == null) {
            settings.setActiveProfiles(new ArrayList<String>());
        }
        if (!settings.getActiveProfiles().contains(activeProfile)) {
            settings.addActiveProfile(activeProfile);
        }
        Profile profile = other.getProfiles().get(0);
        if (settings.getProfiles() == null) {
            settings.setProfiles(new ArrayList<Profile>());
        }
        // remove already created OpenMRS profile
        List<Profile> profilesToRemove = new ArrayList<>();
        for (Profile p: settings.getProfiles()) {
            if (p.getId().equalsIgnoreCase(profile.getId())) {
                profilesToRemove.add(p);
            }
        }
        for (Profile p: profilesToRemove) {
            settings.removeProfile(p);
        }
        settings.addProfile(profile);
    }

    /**
     * Get settings object
     * @return
     */
    public Settings getSettings() {
        return settings;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    /**
     * Write settings to file
     * @param stream
     */
    public void apply(OutputStream stream) throws MojoExecutionException {
        if (settings != null) {
            SettingsXpp3Writer writer = new SettingsXpp3Writer();
            try {
                writer.write(stream, settings);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write setting.xml", e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
    }
}
