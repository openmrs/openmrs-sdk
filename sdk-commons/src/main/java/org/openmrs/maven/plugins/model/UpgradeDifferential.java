package org.openmrs.maven.plugins.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class UpgradeDifferential {

    private final Server server;
    private final Distribution distribution;
    private ArtifactChanges warChanges = new ArtifactChanges(null, null);
    private ArtifactChanges moduleChanges = new ArtifactChanges(null, null);
    private ArtifactChanges owaChanges = new ArtifactChanges(null, null);
    private ArtifactChanges spaArtifactChanges = new ArtifactChanges(null, null);
    private PropertyChanges spaBuildChanges = new PropertyChanges(null, null);
    private ArtifactChanges configChanges = new ArtifactChanges(null, null);
    private ArtifactChanges contentChanges = new ArtifactChanges(null, null);

    public UpgradeDifferential(Server server, Distribution distribution) {
        this.server = server;
        this.distribution = distribution;
    }

    @Data
    public static class ArtifactChanges {

        private final List<Artifact> oldArtifacts = new ArrayList<>();
        private final List<Artifact> newArtifacts = new ArrayList<>();

        public ArtifactChanges(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
            if (oldArtifacts != null) {
                this.oldArtifacts.addAll(oldArtifacts);
            }
            if (newArtifacts != null) {
                this.newArtifacts.addAll(newArtifacts);
            }
        }

        public boolean hasChanges() {
            return !getAddedArtifacts().isEmpty() || !getRemovedArtifacts().isEmpty() || !getUpgradedArtifacts().isEmpty() || !getDowngradedArtifacts().isEmpty();
        }

        public List<Artifact> getAddedArtifacts() {
            List<Artifact> ret = new ArrayList<>();
            for (Artifact newArtifact : newArtifacts) {
                boolean found = false;
                for (Artifact oldArtifact : oldArtifacts) {
                    found = found || (isSameArtifact(oldArtifact, newArtifact));
                }
                if (!found) {
                    ret.add(newArtifact);
                }
            }
            return ret;
        }

        public List<Artifact> getRemovedArtifacts() {
            List<Artifact> ret = new ArrayList<>();
            for (Artifact oldArtifact : oldArtifacts) {
                boolean found = false;
                for (Artifact newArtifact : newArtifacts) {
                    found = found || (isSameArtifact(oldArtifact, newArtifact));
                }
                if (!found) {
                    ret.add(oldArtifact);
                }
            }
            return ret;
        }

        public Map<Artifact, Artifact> getUpgradedArtifacts() {
            Map<Artifact, Artifact> ret = new HashMap<>();
            for (Artifact newArtifact : newArtifacts) {
                for (Artifact oldArtifact : oldArtifacts) {
                    if (isSameArtifact(oldArtifact, newArtifact)) {
                        if (isHigherVersion(oldArtifact, newArtifact)) {
                            ret.put(oldArtifact, newArtifact);
                        }
                    }
                }
            }
            return ret;
        }

        public Map<Artifact, Artifact> getDowngradedArtifacts() {
            Map<Artifact, Artifact> ret = new HashMap<>();
            for (Artifact newArtifact : newArtifacts) {
                for (Artifact oldArtifact : oldArtifacts) {
                    if (isSameArtifact(oldArtifact, newArtifact)) {
                        if (isLowerVersion(oldArtifact, newArtifact)) {
                            ret.put(oldArtifact, newArtifact);
                        }
                    }
                }
            }
            return ret;
        }

        public List<Artifact> getArtifactsToRemove() {
            List<Artifact> ret = getRemovedArtifacts();
            ret.addAll(getUpgradedArtifacts().keySet());
            ret.addAll(getDowngradedArtifacts().keySet());
            return ret;
        }

        public List<Artifact> getArtifactsToAdd() {
            List<Artifact> ret = getAddedArtifacts();
            ret.addAll(getUpgradedArtifacts().values());
            ret.addAll(getDowngradedArtifacts().values());
            return ret;
        }

        public boolean isSameArtifact(Artifact left, Artifact right) {
            return getId(left.getDestFileName()).equals(getId(right.getDestFileName()));
        }

        public String getId(String name) {
            int index = name.indexOf('-');
            if (index == -1) {
                return name;
            }
            return name.substring(0, index);
        }

        /**
         * checks if next artifact is higher version of the same artifact
         * returns true for equal version snapshots
         */
        public boolean isHigherVersion(Artifact previous, Artifact next) {
            if (artifactsToCompareAreInvalid(previous, next)) {
                return false;
            }
            Version previousVersion = new Version(previous.getVersion());
            Version nextVersion = new Version(next.getVersion());
            return nextVersion.higher(previousVersion) || (previousVersion.isSnapshot() && nextVersion.isSnapshot());
        }

        public boolean isLowerVersion(Artifact previous, Artifact next) {
            if (artifactsToCompareAreInvalid(previous, next)) {
                return false;
            }
            Version previousVersion = new Version(previous.getVersion());
            Version nextVersion = new Version(next.getVersion());
            return nextVersion.lower(previousVersion);
        }

        public boolean artifactsToCompareAreInvalid(Artifact previous, Artifact next) {
            return previous == null || next == null
                    || previous.getArtifactId() == null || next.getArtifactId() == null
                    || previous.getVersion() == null || next.getVersion() == null
                    || !isSameArtifact(previous, next);
        }
    }

    @Data
    public static class PropertyChanges {

        private final Map<String, String> oldProperties = new LinkedHashMap<>();
        private final Map<String, String> newProperties = new LinkedHashMap<>();

        public PropertyChanges(Map<String, String> oldProperties, Map<String, String> newProperties) {
            if (oldProperties != null) {
                this.oldProperties.putAll(oldProperties);
            }
            if (newProperties != null) {
                this.newProperties.putAll(newProperties);
            }
        }

        public Map<String, String> getAddedProperties() {
            Map<String, String> ret = new HashMap<>(newProperties);
            ret.keySet().removeAll(oldProperties.keySet());
            return ret;
        }

        public Map<String, String> getRemovedProperties() {
            Map<String, String> ret = new HashMap<>(oldProperties);
            ret.keySet().removeAll(newProperties.keySet());
            return ret;
        }

        public Map<String, String> getChangedProperties() {
            Map<String, String> ret = new HashMap<>();
            for (String key : oldProperties.keySet()) {
                String newValue = newProperties.get(key);
                if (newValue != null) {
                    if (!newValue.equals((oldProperties.get(key))) || isUnresolvedVersion(newValue)) {
                        ret.put(key, newProperties.get(key));
                    }
                }
            }
            return ret;
        }

        public boolean isUnresolvedVersion(String version) {
            if (version == null) {
                return false;
            }
            version = version.toLowerCase();
            return version.equals("next") || version.endsWith("-snapshot");
        }

        public boolean hasChanges() {
            return !getAddedProperties().isEmpty() || !getRemovedProperties().isEmpty() || !getChangedProperties().isEmpty();
        }
    }
}
