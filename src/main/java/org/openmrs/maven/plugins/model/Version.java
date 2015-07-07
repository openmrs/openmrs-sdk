package org.openmrs.maven.plugins.model;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class Version extends DefaultArtifactVersion {

    public Version(String version) {
        super(version);
    }

    public boolean lower(Version that) {
        return this.compareTo(that) == -1;
    }

    public boolean equal(Version that) {
        return this.compareTo(that) == 0;
    }

    public boolean higher(Version that) {
        return this.compareTo(that) == 1;
    }
}