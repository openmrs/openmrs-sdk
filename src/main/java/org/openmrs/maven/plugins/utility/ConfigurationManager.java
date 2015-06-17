package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Class for reading/writing pom.xml
 */
public class ConfigurationManager {
    private String path;
    private Model model;
    private Log log;

    /**
     * Default constructor
     */
    private ConfigurationManager() {
        model = new Model();
    }

    /**
     * Create new configuration object by the path
     * @param pomFile - path to pom
     */
    public ConfigurationManager(String pomFile, Log log) {
        this();
        this.log = log;
        this.path = pomFile;
        File conf = new File(path);
        FileReader reader = null;
        if (conf.exists()) {
            try {
                reader = new FileReader(path);
                model = new MavenXpp3Reader().read(reader);
                reader.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            } catch (XmlPullParserException e) {
                log.error(e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Get list of model profiles
     * @return list of profiles
     */
    public List<Profile> getProfiles() {
        return model.getProfiles();
    }

    /**
     * Add profile to model
     * @param p - profile to add
     */
    public void addProfile(Profile p) {
        model.addProfile(p);
    }

    /**
     * Get profile by id
     * @param id - key for search
     * @return Profile with given id
     */
    public Profile getProfile(String id) {
        for (Profile p: model.getProfiles()) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    /**
     * Check if configuration exists, and add artifact list to configuration
     * @param list - artifact list
     */
    public void addArtifactListToConfiguration(List<Artifact> list) {
        PluginExecution execution = model.getBuild().getPlugins().get(0).getExecutions().get(0);
        Xpp3Dom config = (Xpp3Dom) execution.getConfiguration();
        if (config == null) config = new Xpp3Dom("configuration");
        if (config.getChild("artifactItems") == null) config.addChild(new Xpp3Dom("artifactItems"));
        Xpp3Dom artifactItems = config.getChild("artifactItems");
        for (Artifact artifact: list) {
            Xpp3Dom artifactItem = artifact.toArtifactItem();
            artifactItems.addChild(artifactItem);
        }
        // set config to POM
        execution.setConfiguration(config);
    }

    /**
     * Get artifactItem by groupId, artifactId
     * @param groupId
     * @param artifactId
     * @return
     */
    public Xpp3Dom getArtifactItem(String groupId, String artifactId) {
        PluginExecution execution = model.getBuild().getPlugins().get(0).getExecutions().get(0);
        Xpp3Dom config = (Xpp3Dom) execution.getConfiguration();
        if ((config != null) && (config.getChild("artifactItems") != null)) {
            Xpp3Dom artifactItems = config.getChild("artifactItems");
            for (int x=0;x<artifactItems.getChildCount();x++) {
                Xpp3Dom artifactItem = artifactItems.getChild(x);
                Xpp3Dom id = artifactItem.getChild("artifactId");
                Xpp3Dom group = artifactItem.getChild("groupId");
                if ((id != null) && (id.getValue().equals(artifactId)) && (group.getValue().equals(groupId)))
                    return artifactItems.getChild(x);
            }
        }
        return null;
    }

    /**
     * Get artifactItem by Artifact
     * @param artifact
     * @return
     */
    public Xpp3Dom getArtifactItem(Artifact artifact) {
        return getArtifactItem(artifact.getGroupId(), artifact.getArtifactId());
    }

    /**
     * Get parent property
     * @return
     */
    public Parent getParent() {
       return model.getParent();
    }

    /**
     * Get artifactId
     * @return
     */
    public String getArtifactId() {
        return model.getArtifactId();
    }

    /**
     * Get groupId
     * @return
     */
    public String getGroupId() {
        return model.getGroupId();
    }

    /**
     * Get version
     * @return
     */
    public String getVersion() {
        return model.getVersion();
    }

    /**
     * Update version of selected artifactItem
     * @param groupId
     * @param artifactId
     * @param version
     */
    public void updateArtifactItem(String groupId, String artifactId, String version) {
        Xpp3Dom artifactItem = getArtifactItem(groupId, artifactId);
        if (artifactItem != null) artifactItem.getChild("version").setValue(version);
    }

    /**
     * Update artifact from artifactItems with version
     * @param artifact
     */
    public void updateArtifactItem(Artifact artifact) {
        updateArtifactItem(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    /**
     * Set pom version
     * @param version - server version
     */
    public void setVersion(String version) {
        model.setVersion(version);
    }

    /**
     * Write model to pom file
     */
    public void apply() {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        FileWriter fileWrite = null;
        try {
            fileWrite = new FileWriter(path);
            writer.write(fileWrite, model);
            fileWrite.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(fileWrite);
        }
    }
}
