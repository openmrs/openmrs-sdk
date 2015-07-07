package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class for reading/writing pom.xml
 */
public class ConfigurationManager {
    private Model model;

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
    public ConfigurationManager(String pomFile) throws MojoExecutionException {
        this();
        File conf = new File(pomFile);
        FileReader reader = null;
        if (conf.exists()) {
            try {
                reader = new FileReader(pomFile);
                model = new MavenXpp3Reader().read(reader);
                reader.close();
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            } catch (XmlPullParserException e) {
                throw new MojoExecutionException(e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
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
}
