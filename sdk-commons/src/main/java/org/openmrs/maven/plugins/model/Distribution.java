package org.openmrs.maven.plugins.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic wrapper for list and map of artifacts
 */
@Data
public class Distribution {
    
    private Artifact oldWar;
    private Artifact newWar;
    private List<Artifact> oldModules = new ArrayList<>();
    private List<Artifact> newModules = new ArrayList<>();
    private List<Artifact> oldOwas = new ArrayList<>();
    private List<Artifact> newOwas = new ArrayList<>();
    private List<Artifact> oldSpaArtifacts = new ArrayList<>();
    private List<Artifact> newSpaArtifacts = new ArrayList<>();
    private Map<String, String> oldSpaProperties = new HashMap<>();
    private Map<String, String> newSpaProperties = new HashMap<>();
    private List<Artifact> oldConfig = new ArrayList<>();
    private List<Artifact> newConfig = new ArrayList<>();
    private List<Artifact> oldContent = new ArrayList<>();
    private List<Artifact> newContent = new ArrayList<>();

    public void addWar(Artifact oldWar, Artifact newWar) {
        this.oldWar = oldWar;
        this.newWar = newWar;
    }
    
    public boolean isWarUpgraded() {
        return oldWar == null || isHigherVersion(oldWar, newWar);
    }
    
    public boolean isWarDowngraded() {
        return oldWar != null && isLowerVersion(oldWar, newWar);
    }

    public void addModules(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        if (oldArtifacts != null) {
            this.oldModules.addAll(oldArtifacts);
        }
        if (newArtifacts != null) {
            this.newModules.addAll(newArtifacts);
        }
    }
    
    public List<Artifact> getAddedModules() {
        return getAddedArtifacts(oldModules, newModules);
    }

    public List<Artifact> getRemovedModules() {
        return getRemovedArtifacts(oldModules, newModules);
    }

    public List<Artifact> getUpgradedModules() {
        return getUpgradedArtifacts(oldModules, newModules);
    }

    public List<Artifact> getDowngradedModules() {
        return getUpgradedArtifacts(oldModules, newModules);
    }

    public void addOwas(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        if (oldArtifacts != null) {
            this.oldOwas.addAll(oldArtifacts);
        }
        if (newArtifacts != null) {
            this.newOwas.addAll(newArtifacts);
        }
    }

    public List<Artifact> getAddedOwas() {
        return getAddedArtifacts(oldOwas, newOwas);
    }

    public List<Artifact> getRemovedOwas() {
        return getRemovedArtifacts(oldOwas, newOwas);
    }

    public List<Artifact> getUpgradedOwas() {
        return getUpgradedArtifacts(oldOwas, newOwas);
    }

    public List<Artifact> getDowngradedOwas() {
        return getUpgradedArtifacts(oldOwas, newOwas);
    }

    public void addSpaArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        if (oldArtifacts != null) {
            this.oldSpaArtifacts.addAll(oldArtifacts);
        }
        if (newArtifacts != null) {
            this.newSpaArtifacts.addAll(newArtifacts);
        }
    }

    public void addSpaProperties(Map<String, String> oldSpaProperties, Map<String, String> newSpaProperties) {
        if (oldSpaProperties != null) {
            this.oldSpaProperties.putAll(oldSpaProperties);
        }
        if (newSpaProperties != null) {
            this.newSpaProperties.putAll(newSpaProperties);
        }
    }

    public void addConfigArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        if (oldArtifacts != null) {
            this.oldConfig.addAll(oldArtifacts);
        }
        if (newArtifacts != null) {
            this.newConfig.addAll(newArtifacts);
        }
    }

    public void addContentArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        if (oldArtifacts != null) {
            this.oldContent.addAll(oldArtifacts);
        }
        if (newArtifacts != null) {
            this.newContent.addAll(newArtifacts);
        }
    }

    public List<Artifact> getAddedArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
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

    public List<Artifact> getRemovedArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
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

    public List<Artifact> getUpgradedArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        List<Artifact> ret = new ArrayList<>();
        for (Artifact newArtifact : newArtifacts) {
            for (Artifact oldArtifact : oldArtifacts) {
                if (isSameArtifact(oldArtifact, newArtifact)) {
                    if (isHigherVersion(oldArtifact, newArtifact)) {
                        ret.add(newArtifact);
                    }
                }
            }
        }
        return ret;
    }

    public List<Artifact> getDowngradedArtifacts(List<Artifact> oldArtifacts, List<Artifact> newArtifacts) {
        List<Artifact> ret = new ArrayList<>();
        for (Artifact newArtifact : newArtifacts) {
            for (Artifact oldArtifact : oldArtifacts) {
                if (isSameArtifact(oldArtifact, newArtifact)) {
                    if (isLowerVersion(oldArtifact, newArtifact)) {
                        ret.add(newArtifact);
                    }
                }
            }
        }
        return ret;
    }

    public Map<String, String> getAddedSpaProperties() {
        Map<String, String> ret = new HashMap<>(newSpaProperties);
        ret.keySet().removeAll(oldSpaProperties.keySet());
        return ret;
    }

    public Map<String, String> getRemovedSpaProperties() {
        Map<String, String> ret = new HashMap<>(oldSpaProperties);
        ret.keySet().removeAll(newSpaProperties.keySet());
        return ret;
    }

    public Map<String, String> getChangedSpaProperties() {
        Map<String, String> ret = new HashMap<>();
        for (String key : oldSpaProperties.keySet()) {
            String newValue = newSpaProperties.get(key);
            if (newValue != null) {
                if (!newValue.equals((oldSpaProperties.get(key)))) {
                    ret.put(key, newSpaProperties.get(key));
                }
            }
        }
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

        if (nextVersion.higher(previousVersion)) {
            return true;
        } else if (nextVersion.equal(previousVersion)) {
            return (previousVersion.isSnapshot() && nextVersion.isSnapshot());
        } else {
            return false;
        }
    }

    public boolean isLowerVersion(Artifact previous, Artifact next) {
        if (artifactsToCompareAreInvalid(previous, next)) {
            return false;
        }

        Version previousVersion = new Version(previous.getVersion());
        Version nextVersion = new Version(next.getVersion());

        if (nextVersion.lower(previousVersion)) {
            return true;
        } else if (nextVersion.equal(previousVersion)) {
            return (previousVersion.isSnapshot() && nextVersion.isSnapshot());
        } else {
            return false;
        }
    }

    public boolean artifactsToCompareAreInvalid(Artifact previous, Artifact next) {
        return previous == null || next == null
                || previous.getArtifactId() == null || next.getArtifactId() == null
                || previous.getVersion() == null || next.getVersion() == null
                || !isSameArtifact(previous, next);
    }
}
