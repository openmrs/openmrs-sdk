package org.openmrs.maven.plugins.model;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class Version extends DefaultArtifactVersion {

    public static final String PRIOR = "2.2";

    public Version(String version) { super(version); }

    public boolean lower(Version that) {
        return this.compareTo(that) == -1;
    }

    public boolean equal(Version that) {
        return this.compareTo(that) == 0;
    }

    public boolean higher(Version that) {
        return this.compareTo(that) == 1;
    }

    /**
     * Parse version from module file
     * @param file
     * @return
     */
    public static String parseVersionFromFile(String file) {
        final String SNAPSHOT = "SNAPSHOT";
        String withoutType = file.substring(0, file.lastIndexOf("."));
        String[] parts = withoutType.split("-");
        if (file.indexOf(SNAPSHOT) != -1) {
            return parts[parts.length - 2] + "-" + parts[parts.length -1];
        }
        else return parts[parts.length - 1];
    }
    public boolean isSnapshot(){
        return getQualifier()!= null && getQualifier().contains("SNAPSHOT");
    }
}