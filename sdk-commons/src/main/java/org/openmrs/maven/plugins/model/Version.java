package org.openmrs.maven.plugins.model;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class Version extends DefaultArtifactVersion {

    public Version(String version) { super(version); }

    public boolean lower(Version that) {
        return this.compareTo(that) < 0;
    }

    public boolean equal(Version that) {
        return this.compareTo(that) == 0;
    }

    public boolean higher(Version that) {
        return this.compareTo(that) > 0;
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
        if (file.contains(SNAPSHOT)) {
            return parts[parts.length - 2] + "-" + parts[parts.length -1];
        }
        else {
            return parts[parts.length - 1];
        }
    }
    public boolean isSnapshot() {
        return getQualifier() != null && getQualifier().toUpperCase().contains("SNAPSHOT");
    }

    public boolean isAlpha() {
        return getQualifier() != null && getQualifier().toLowerCase().contains("alpha");
    }

    public boolean isBeta() {
        return getQualifier() != null && getQualifier().toLowerCase().contains("beta");
    }
}
