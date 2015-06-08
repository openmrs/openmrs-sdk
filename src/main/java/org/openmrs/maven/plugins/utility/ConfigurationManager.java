package org.openmrs.maven.plugins.utility;

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
     * @param configPath - path to pom.xml
     */
    public ConfigurationManager(String configPath) {
        this();
        path = configPath;
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
