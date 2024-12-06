package org.openmrs.maven.plugins.model;

import lombok.Data;

/**
 * Represents a Content Package as defined within a properties file
 */
@Data
public class ContentPackage {

    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String namespace;

    public Artifact getArtifact() {
        return new Artifact(artifactId, version, groupId, type);
    }

    public String getGroupIdAndArtifactId() {
        return groupId + ":" + artifactId;
    }
}
