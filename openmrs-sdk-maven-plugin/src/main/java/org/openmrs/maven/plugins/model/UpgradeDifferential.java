package org.openmrs.maven.plugins.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic wrapper for list and map of artifacts
 */
public class UpgradeDifferential {
    private final List<Artifact> modulesToAdd = new ArrayList<Artifact>();
    private final Map<Artifact, Artifact> updateOldToNewMap = new HashMap<Artifact,Artifact>();
    private Artifact platformArtifact;

    public boolean isEmpty(){
        return platformArtifact ==null&& updateOldToNewMap.isEmpty()&&modulesToAdd.isEmpty();
    }

    public Artifact getPlatformArtifact() {
        return platformArtifact;
    }

    public void setPlatformArtifact(Artifact platformArtifact) {
        this.platformArtifact = platformArtifact;
    }

    public List<Artifact> getModulesToAdd() {
        return modulesToAdd;
    }

    /**
     * IMPORTANT: convention is: key is old version artifact, value is new version artifact
     */
    public Map<Artifact, Artifact> getUpdateOldToNewMap() {
        return updateOldToNewMap;
    }

    public boolean addModuleToAdd(Artifact artifact) {
        return modulesToAdd.add(artifact);
    }

    public boolean removeModuleToAdd(Object o) {
        return modulesToAdd.remove(o);
    }

    public Artifact putUpdateEntry(Artifact oldVersionArtifact, Artifact newVersionArtifact) {
        return updateOldToNewMap.put(oldVersionArtifact, newVersionArtifact);
    }

    public Artifact removeUpdateEntry(Object oldVersionArtifact) {
        return updateOldToNewMap.remove(oldVersionArtifact);
    }
}
