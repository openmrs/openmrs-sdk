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
    private final List<Artifact> modulesToDelete = new ArrayList<Artifact>();
    private final Map<Artifact, Artifact> updateOldToNewMap = new HashMap<Artifact,Artifact>();
    private final Map<Artifact, Artifact> downgradeNewToOldMap = new HashMap<Artifact,Artifact>();
    private Artifact platformArtifact;
    private boolean platformUpgraded;

    public boolean isPlatformUpgraded() {
        return platformUpgraded;
    }

    public void setPlatformUpgraded(boolean platformUpgraded) {
        this.platformUpgraded = platformUpgraded;
    }

    public boolean isEmpty(){
        return platformArtifact ==null&& updateOldToNewMap.isEmpty()&&modulesToAdd.isEmpty()
                &&modulesToDelete.isEmpty()&&downgradeNewToOldMap.isEmpty();
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

    public List<Artifact> getModulesToDelete() {
        return modulesToDelete;
    }

    /**
     * IMPORTANT: convention is: key is old version artifact, value is new version artifact
     */
    public Map<Artifact, Artifact> getUpdateOldToNewMap() {
        return updateOldToNewMap;
    }

    public Map<Artifact, Artifact> getDowngradeNewToOldMap() {
        return downgradeNewToOldMap;
    }

    public boolean addModuleToAdd(Artifact artifact) {
        return modulesToAdd.add(artifact);
    }

    public boolean removeModuleToAdd(Object o) {
        return modulesToAdd.remove(o);
    }

    public boolean addModuleToDelete(Artifact artifact){
        return modulesToDelete.add(artifact);
    }

    public boolean removeModuleToDelete(Object o) {
        return modulesToDelete.remove(o);
    }

    public Artifact putUpdateEntry(Artifact oldVersionArtifact, Artifact newVersionArtifact) {
        return updateOldToNewMap.put(oldVersionArtifact, newVersionArtifact);
    }

    public Artifact removeUpdateEntry(Object oldVersionArtifact) {
        return updateOldToNewMap.remove(oldVersionArtifact);
    }

    public Artifact putDowngradeEntry(Artifact oldVersionArtifact, Artifact newVersionArtifact) {
        return downgradeNewToOldMap.put(oldVersionArtifact, newVersionArtifact);
    }

    public Artifact removeDowngradeEntry(Object oldVersionArtifact) {
        return downgradeNewToOldMap.remove(oldVersionArtifact);
    }
}
