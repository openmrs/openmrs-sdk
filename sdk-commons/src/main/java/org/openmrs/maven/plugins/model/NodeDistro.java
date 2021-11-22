package org.openmrs.maven.plugins.model;

public class NodeDistro {

    private String version;
    private String npm;

    public String getVersion() {
        if (version.startsWith("v")) {
            return version.substring(1);
        } else {
            return version;
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNpm() {
        return npm;
    }

    public void setNpm(String npm) {
        this.npm = npm;
    }
}
