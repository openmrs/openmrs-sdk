package org.openmrs.maven.plugins.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OwaId {

    private String version;
    private String name;

    @SuppressWarnings("unused")
    public OwaId() {
    }

    public OwaId(String name, String version) {
        this.version = version;
        this.name = name;
    }

    @Override
    public String toString() {
        return name+":"+version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OwaId owaId = (OwaId) o;
        return Objects.equal(version, owaId.version) &&
                Objects.equal(name, owaId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, name);
    }
}
