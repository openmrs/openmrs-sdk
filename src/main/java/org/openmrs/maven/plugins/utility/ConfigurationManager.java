package org.openmrs.maven.plugins.utility;

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
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

    /**
     * Default constructor
     */
    public ConfigurationManager() {
        model = new Model();
    }

    /**
     * Create new configuration object by the path
     * @param projectPath - path to project
     */
    public ConfigurationManager(String projectPath) {
        this();
        path = new File(projectPath, SDKConstants.OPENMRS_SERVER_POM).getPath();
        File conf = new File(path);
        if (conf.exists()) {
            try {
                model = new MavenXpp3Reader().read(new FileReader(path));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
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
    public void addArtifactsToConfiguration(List<Artifact> list) {
        PluginExecution execution = model.getBuild().getPlugins().get(0).getExecutions().get(0);
        Xpp3Dom config = (Xpp3Dom) execution.getConfiguration();
        if (config == null) config = new Xpp3Dom("configuration");
        if (config.getChild("artifactItems") == null) config.addChild(new Xpp3Dom("artifactItems"));
        Xpp3Dom artifactItems = config.getChild("artifactItems");
        for (Artifact artifact: list) {
            Xpp3Dom item = new Xpp3Dom("artifactItem");
            // create groupId
            Xpp3Dom groupId = new Xpp3Dom("groupId");
            groupId.setValue(artifact.getGroupId());
            item.addChild(groupId);
            // create artifactId
            Xpp3Dom artifactId = new Xpp3Dom("artifactId");
            artifactId.setValue(artifact.getArtifactId());
            item.addChild(artifactId);
            // create destFileName
            Xpp3Dom destFileName = new Xpp3Dom("destFileName");
            destFileName.setValue(artifact.getDestFileName());
            item.addChild(destFileName);
            // create type (if it set)
            if (artifact.getType() != null) {
                Xpp3Dom type = new Xpp3Dom("type");
                type.setValue(artifact.getType());
                item.addChild(type);
            }
            // add artifact to list
            artifactItems.addChild(item);
        }
        // set config to POM
        execution.setConfiguration(config);
    }

    /**
     * Set pom version
     * @param version
     */
    public void setVersion(String version) {
        model.setVersion(version);
    }

    /**
     * Write model to pom file
     */
    public void apply() {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(new FileWriter(path), model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
