package org.openmrs.maven.plugins.model;

/**
 * Class for Artifact model
 */
public class Artifact {
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String destFileName;

    public Artifact() {};

    public static final String MODEL = "org.openmrs.module";

    public Artifact(String artifactId, String destFileName) {
        this.groupId = MODEL;
        this.artifactId = artifactId;
        this.destFileName = destFileName;
    }

    public Artifact(String artifactId, String destFileName, String groupId) {
        this(artifactId, destFileName);
        this.groupId = groupId;
    }

    public Artifact(String artifactId, String destFileName, String groupId, String type) {
        this(groupId, artifactId, destFileName);
        this.type = type;
    };

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }
}
