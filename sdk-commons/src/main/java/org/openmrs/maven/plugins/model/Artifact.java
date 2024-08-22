package org.openmrs.maven.plugins.model;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;

/**
 * Class for Artifact model
 */
public class Artifact {
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String classifier;
    private String fileExtension;
    private String destFileName;

    public static final String GROUP_MODULE = "org.openmrs.module";
    public static final String GROUP_OWA = "org.openmrs.owa";
    public static final String GROUP_WEB = "org.openmrs.web";
    public static final String GROUP_OPENMRS = "org.openmrs";
    public static final String GROUP_H2 = "com.h2database";
    public static final String GROUP_DISTRO = "org.openmrs.distro";
    public static final String GROUP_CONTENT = "org.openmrs.content";
    public static final String TYPE_OMOD = "omod";
    public static final String TYPE_WAR = "war";
    public static final String TYPE_JAR = "jar";
    public static final String TYPE_ZIP = "zip";
    public static final String DEST_TEMPLATE = "%s-%s.%s";

    public Artifact() {}

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
        this(artifactId, version, groupId, TYPE_JAR);
    }

    public Artifact(String artifactId, String version, String groupId, String type) {
        this(artifactId, version, groupId, type, type);
    }

    /**
     * Default constructor for all parameters
     * @param artifactId
     * @param version
     * @param groupId
     * @param type
     */
    public Artifact(String artifactId, String version, String groupId, String type, String fileExtension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.fileExtension = fileExtension;
        classifier = null;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() { return groupId; }

    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getDestFileName() {
        if (destFileName == null) {
            String[] parts = StringUtils.reverse(artifactId).split("-", 2);

            String id;
            if (parts.length == 1) {
                id = artifactId;
            } else {
                id = StringUtils.reverse(parts[1]);

                String remainder = StringUtils.reverse(parts[0]);
                if (!remainder.equals("omod") && !remainder.equals("webapp")) {
                    id += "-" + remainder;
                }
            }

            return String.format(DEST_TEMPLATE, id, version, fileExtension);
        } else {
            return destFileName;
        }
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /**
     * Convert Artifact to Element
     * String outputDir
     * @return
     */
    public Element toElement(String outputDir) {
        List<Element> attributes = new ArrayList<>();
        attributes.add(element("groupId", groupId));
        attributes.add(element("artifactId", artifactId));
        attributes.add(element("version", version));
        attributes.add(element("destFileName", getDestFileName()));
        if (type != null) {
            attributes.add(element("type", type));
        }
        if (classifier != null) {
            attributes.add(element("classifier", classifier));
        }
        attributes.add(element("outputDirectory", outputDir));
        Element[] arrayElements = attributes.toArray(new Element[0]);
        return element("artifactItem", arrayElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artifact artifact = (Artifact) o;
        return Objects.equal(version, artifact.version) &&
                Objects.equal(groupId, artifact.groupId) &&
                Objects.equal(artifactId, artifact.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, groupId, artifactId);
    }

    @Override
    public String toString() {
        return groupId + ':' + artifactId + ':' + version;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(groupId)
                && StringUtils.isNotBlank(artifactId)
                && StringUtils.isNotBlank(version)
                && StringUtils.isNotBlank(type);
    }
}
