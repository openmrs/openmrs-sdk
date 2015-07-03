package org.openmrs.maven.plugins.model;

import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Class for Artifact model
 */
public class Artifact {
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String destFileName;

    public static final String GROUP_MODULE = "org.openmrs.module";
    public static final String GROUP_WEB = "org.openmrs.web";
    public static final String GROUP_OPENMRS = "org.openmrs";
    public static final String GROUP_H2 = "com.h2database";
    public static final String TYPE_OMOD = "omod";
    public static final String TYPE_WAR = "war";
    public static final String TYPE_JAR = "jar";
    public static final String DEST_TEMPLATE = "%s-%s.%s";

    public Artifact() {};

    /**
     * Constructor if type is not set, and groupId is default
     * @param artifactId
     * @param version
     */
    public Artifact(String artifactId, String version) {
        this(artifactId, version, GROUP_MODULE);
    }

    /**
     * Constructor if type is not set
     * @param artifactId
     * @param version
     * @param groupId
     */
    public Artifact(String artifactId, String version, String groupId) {
        this(artifactId, version, groupId, null);
    }

    /**
     * Default constructor for all parameters
     * @param artifactId
     * @param version
     * @param groupId
     * @param type
     */
    public Artifact(String artifactId, String version, String groupId, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        // get values for destination FileName
        String artifactType = (type != null) ? type : TYPE_OMOD;
        String id = artifactId.split("-")[0];
        // make destination FileName
        this.destFileName = String.format(DEST_TEMPLATE, id, version, artifactType);
        // set type if not null
        if (type != null) this.type = type;
    }
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
        String artifactType = (type != null) ? type : TYPE_OMOD;
        String id = artifactId.split("-")[0];
        // update file name
        this.destFileName = Artifact.getFileName(id, version, artifactType);
    }

    /**
     * Generate dest file name for artifact
     * @param id
     * @param version
     * @param type
     * @return
     */
    public static String getFileName(String id, String version, String type) {
        return String.format(DEST_TEMPLATE, id, version, type);
    }

    public String getGroupId() { return groupId; }

    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getDestFileName() { return destFileName; }

    public void setDestFileName(String destFileName) { this.destFileName = destFileName; }

    /**
     * Convert Artifact to Element
     * String outputDir
     * @return
     */
    public Element toElement(String outputDir) {
        List<Element> attributes = new ArrayList<Element>();
        attributes.add(element("groupId", groupId));
        attributes.add(element("artifactId", artifactId));
        attributes.add(element("version", version));
        attributes.add(element("destFileName", destFileName));
        if (type != null) {
            attributes.add(element("type", type));
        }
        attributes.add(element("outputDirectory", outputDir));
        Element[] arrayElements = attributes.toArray(new Element[0]);
        return element("artifactItem", arrayElements);
    }
}
