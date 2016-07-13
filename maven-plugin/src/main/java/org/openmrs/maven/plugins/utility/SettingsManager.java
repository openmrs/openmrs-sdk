package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Configure settings.xml
 */
public class SettingsManager {

    private Settings settings;

    public SettingsManager() {
        settings = new Settings();
    };

    public SettingsManager(InputStream stream) throws MojoExecutionException {
        this();
        SettingsXpp3Reader reader = new SettingsXpp3Reader();
        try {
            settings = reader.read(stream);
            stream.close();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (XmlPullParserException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(stream);
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
        if(settings.getServer("bintray") == null) {
            ArrayList<Server> servers = new ArrayList<>();
            servers.add(other.getServer("bintray"));
            settings.setServers(servers);
        }
        // remove already created OpenMRS profile
        List<Profile> profilesToRemove = new ArrayList<Profile>();
        for (Profile p: settings.getProfiles()) {
            if (p.getId().toLowerCase().equals(profile.getId().toLowerCase())) {
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

    /**
     * Write settings to file
     * @param stream
     */
    public void apply(OutputStream stream) throws MojoExecutionException {
        if (settings != null) {
            SettingsXpp3Writer writer = new SettingsXpp3Writer();
            try {
                writer.write(stream, settings);
                stream.close();
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
    }
}
