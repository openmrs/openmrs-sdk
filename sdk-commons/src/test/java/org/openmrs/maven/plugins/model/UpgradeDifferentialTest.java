package org.openmrs.maven.plugins.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openmrs.maven.plugins.model.UpgradeDifferential.ArtifactChanges;
import static org.openmrs.maven.plugins.model.UpgradeDifferential.PropertyChanges;

public class UpgradeDifferentialTest {

    ArtifactChanges artifactChanges;
    PropertyChanges propertyChanges;

    @Test
    public void artifactChanges_shouldSupportNullLists() {
        assertArtifactChanges(new ArtifactChanges(null, null), 0, 0, 0, 0, false);
        assertArtifactChanges(new ArtifactChanges(null, new ArrayList<>()), 0, 0, 0, 0, false);
        assertArtifactChanges(new ArtifactChanges(new ArrayList<>(), null), 0, 0, 0, 0, false);
    }

    @Test
    public void artifactChanges_shouldIdentifyNoChanges() {
        Artifact startingModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        artifactChanges = new ArtifactChanges(Collections.singletonList(startingModule), Collections.singletonList(newModule));
        assertArtifactChanges(artifactChanges, 0, 0, 0, 0, false);
    }

    @Test
    public void artifactChanges_shouldIdentifyAdditions() {
        Artifact newModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        artifactChanges = new ArtifactChanges(Collections.emptyList(), Collections.singletonList(newModule));
        assertArtifactChanges(artifactChanges, 1, 0, 0, 0, true);
    }

    @Test
    public void artifactChanges_shouldIdentifyRemovals() {
        Artifact startingModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        artifactChanges = new ArtifactChanges(Collections.singletonList(startingModule), Collections.emptyList());
        assertArtifactChanges(artifactChanges, 0, 1, 0, 0, true);
    }

    @Test
    public void artifactChanges_shouldIdentifyUpgrades() {
        Artifact startingModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        artifactChanges = new ArtifactChanges(Collections.singletonList(startingModule), Collections.singletonList(newModule));
        assertArtifactChanges(artifactChanges, 0, 0, 1, 0, true);
    }

    @Test
    public void artifactChanges_shouldIdentifyDowngrades() {
        Artifact startingModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        artifactChanges = new ArtifactChanges(Collections.singletonList(startingModule), Collections.singletonList(newModule));
        assertArtifactChanges(artifactChanges, 0, 0, 0, 1, true);
    }

    @Test
    public void artifactChanges_shouldIdentifySameArtifact() {
        artifactChanges = new ArtifactChanges(null, null);
        Artifact startingModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        assertTrue(artifactChanges.isSameArtifact(startingModule, newModule));
        newModule = new Artifact("moduleId", "1.2.5", "org.openmrs.module");
        assertTrue(artifactChanges.isSameArtifact(startingModule, newModule));
        newModule = new Artifact("moduleId2", "1.2.4", "org.openmrs.module");
        assertFalse(artifactChanges.isSameArtifact(startingModule, newModule));
    }

    @Test
    public void artifactChanges_shouldIdentifyHigherVersion() {
        artifactChanges = new ArtifactChanges(null, null);
        Artifact startingModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        assertFalse(artifactChanges.isHigherVersion(startingModule, newModule));
        newModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        assertFalse(artifactChanges.isHigherVersion(startingModule, newModule));
        newModule = new Artifact("moduleId", "1.2.5", "org.openmrs.module");
        assertTrue(artifactChanges.isHigherVersion(startingModule, newModule));
        startingModule = new Artifact("moduleId", "1.2.4-SNAPSHOT", "org.openmrs.module");
        newModule = new Artifact("moduleId", "1.2.4-SNAPSHOT", "org.openmrs.module");
        assertTrue(artifactChanges.isHigherVersion(startingModule, newModule));
    }

    @Test
    public void artifactChanges_shouldIdentifyLowerVersion() {
        artifactChanges = new ArtifactChanges(null, null);
        Artifact startingModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        Artifact newModule = new Artifact("moduleId", "1.2.4", "org.openmrs.module");
        assertFalse(artifactChanges.isLowerVersion(startingModule, newModule));
        newModule = new Artifact("moduleId", "1.2.5", "org.openmrs.module");
        assertFalse(artifactChanges.isLowerVersion(startingModule, newModule));
        newModule = new Artifact("moduleId", "1.2.3", "org.openmrs.module");
        assertTrue(artifactChanges.isLowerVersion(startingModule, newModule));
        startingModule = new Artifact("moduleId", "1.2.4-SNAPSHOT", "org.openmrs.module");
        newModule = new Artifact("moduleId", "1.2.4-SNAPSHOT", "org.openmrs.module");
        assertFalse(artifactChanges.isLowerVersion(startingModule, newModule));
    }

    @Test
    public void propertyChanges_shouldSupportNullLists() {
        propertyChanges = new PropertyChanges(null, null);
        assertTrue(propertyChanges.getAddedProperties().isEmpty());
        assertTrue(propertyChanges.getRemovedProperties().isEmpty());
        assertTrue(propertyChanges.getChangedProperties().isEmpty());
        assertFalse(propertyChanges.hasChanges());
    }

    @Test
    public void propertyChanges_shouldIdentifyNoChanges() {
        Map<String, String> starting = map("prop1", "val1", "prop2", "val2");
        Map<String, String> ending = map("prop1", "val1", "prop2", "val2");
        propertyChanges = new PropertyChanges(starting, ending);
        assertTrue(propertyChanges.getAddedProperties().isEmpty());
        assertTrue(propertyChanges.getRemovedProperties().isEmpty());
        assertTrue(propertyChanges.getChangedProperties().isEmpty());
        assertFalse(propertyChanges.hasChanges());
    }

    @Test
    public void propertyChanges_shouldIdentifyAdditions() {
        Map<String, String> starting = map("prop1", "val1");
        Map<String, String> ending = map("prop1", "val1", "prop2", "val2");
        propertyChanges = new PropertyChanges(starting, ending);
        assertThat(propertyChanges.getAddedProperties().size(), equalTo(1));
        assertThat(propertyChanges.getAddedProperties().get("prop2"), equalTo("val2"));
        assertTrue(propertyChanges.getRemovedProperties().isEmpty());
        assertTrue(propertyChanges.getChangedProperties().isEmpty());
        assertTrue(propertyChanges.hasChanges());
    }

    @Test
    public void propertyChanges_shouldIdentifyRemovals() {
        Map<String, String> starting = map("prop1", "val1", "prop2", "val2");
        Map<String, String> ending = map("prop1", "val1");
        propertyChanges = new PropertyChanges(starting, ending);
        assertTrue(propertyChanges.getAddedProperties().isEmpty());
        assertThat(propertyChanges.getRemovedProperties().size(), equalTo(1));
        assertThat(propertyChanges.getRemovedProperties().get("prop2"), equalTo("val2"));
        assertTrue(propertyChanges.getChangedProperties().isEmpty());
        assertTrue(propertyChanges.hasChanges());
    }

    @Test
    public void propertyChanges_shouldIdentifyChanges() {
        Map<String, String> starting = map("prop1", "val1", "prop2", "val2");
        Map<String, String> ending = map("prop1", "val1", "prop2", "val3");
        propertyChanges = new PropertyChanges(starting, ending);
        assertTrue(propertyChanges.getAddedProperties().isEmpty());
        assertTrue(propertyChanges.getRemovedProperties().isEmpty());
        assertThat(propertyChanges.getChangedProperties().size(), equalTo(1));
        assertThat(propertyChanges.getChangedProperties().get("prop2"), equalTo("val3"));
        assertTrue(propertyChanges.hasChanges());
    }

    @Test
    public void propertyChanges_shouldIdentifySnapshotChanges() {
        Map<String, String> starting = map("prop1", "next");
        Map<String, String> ending = map("prop1", "next");
        propertyChanges = new PropertyChanges(starting, ending);
        assertTrue(propertyChanges.getAddedProperties().isEmpty());
        assertTrue(propertyChanges.getRemovedProperties().isEmpty());
        assertThat(propertyChanges.getChangedProperties().size(), equalTo(1));
        assertThat(propertyChanges.getChangedProperties().get("prop1"), equalTo("next"));
        assertTrue(propertyChanges.hasChanges());
    }

    void assertArtifactChanges(UpgradeDifferential.ArtifactChanges artifactChanges, int added, int removed, int upgraded, int downgraded, boolean hasChanges) {
        assertThat(artifactChanges.getAddedArtifacts().size(), equalTo(added));
        assertThat(artifactChanges.getRemovedArtifacts().size(), equalTo(removed));
        assertThat(artifactChanges.getUpgradedArtifacts().size(), equalTo(upgraded));
        assertThat(artifactChanges.getDowngradedArtifacts().size(), equalTo(downgraded));
        assertThat(artifactChanges.getArtifactsToAdd().size(), equalTo(added + upgraded + downgraded));
        assertThat(artifactChanges.getArtifactsToRemove().size(), equalTo(removed + upgraded + downgraded));
        assertThat(artifactChanges.hasChanges(), equalTo(hasChanges));
    }

    Map<String, String> map(String... keysAndValues) {
        Map<String, String> properties = new HashMap<>();
        for (int i=0; i<keysAndValues.length; i+=2) {
            properties.put(keysAndValues[i], keysAndValues[i+1]);
        }
        return properties;
    }
}
