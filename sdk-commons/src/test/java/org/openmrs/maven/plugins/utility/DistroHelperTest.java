package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

/**
 *
 */
public class DistroHelperTest {

    @Test
    public void parseDistroArtifactShouldInferArtifactIdForRefapp() throws Exception{
        String distro = "referenceapplication:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is(Artifact.GROUP_DISTRO));
        assertThat(artifact.getArtifactId(), is(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID));
    }
    @Test
    public void parseDistroArtifactShouldSetDefaultGroupIdIfNotSpecified() throws Exception{
        String distro = "otherdistro:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is(Artifact.GROUP_DISTRO));
    }
    @Test(expected = MojoExecutionException.class)
    public void parseDistroArtifactShouldReturnNullIfInvalidFormat() throws Exception{
        String distro = "referenceapplication:2.3:fsf:444";
        DistroHelper.parseDistroArtifact(distro, null);
    }
    @Test
    public void parseDistroArtifactShouldCreateProperArtifact() throws Exception{
        String distro = "org.openmrs.distromock:refapp:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is("org.openmrs.distromock"));
        assertThat(artifact.getArtifactId(), is("refapp"));
        assertThat(artifact.getVersion(), is("2.3"));
    }
    @Test
    public void calculateUpdateDifferentialShouldFindArtifactsToAddList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockNewArtifactList());
        assertModuleAdded(upgradeDifferential, "drugs", "0.2-SNAPSHOT");
        assertThat(upgradeDifferential.getModuleChanges().getAddedArtifacts(), hasSize(1));
    }
    @Test
    public void calculateUpdateDifferentialShouldFindArtifactsToDeleteList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockNewArtifactList());
        assertModuleRemoved(upgradeDifferential, "owa", "1.5");
        assertThat(upgradeDifferential.getModuleChanges().getRemovedArtifacts(), hasSize(1));
    }
    @Test
    public void calculateUpdateDifferentialShouldAddSnapshotToUpdateMap() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockNewArtifactList());
        assertModuleUpgraded(upgradeDifferential, "appui", "0.1-SNAPSHOT", "0.1-SNAPSHOT");
        assertModuleUpgraded(upgradeDifferential, "webservices", "1.0", "1.2");
        assertThat(upgradeDifferential.getModuleChanges().getUpgradedArtifacts().keySet(), hasSize(2));
    }

    @Test
    public void calculateUpdateDifferentialShouldAddSnapshotToDowngradeMap() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockNewArtifactList());
        assertModuleDowngraded(upgradeDifferential, "legacyui", "1.5", "1.4");
        assertThat(upgradeDifferential.getModuleChanges().getDowngradedArtifacts().keySet(), hasSize(1));
    }

    @Test
    public void calculateUpdateDifferentialShouldFindPlatformUpdate() throws MojoExecutionException {
        Properties oldPlatform = new Properties();
        oldPlatform.put("war.openmrs", "10.7");
        Properties newPlatform = new Properties();
        newPlatform.put("war.openmrs", "12.0");
        UpgradeDifferential upgradeDifferential = calculateDifferential(oldPlatform, newPlatform);
        assertThat(upgradeDifferential.getWarChanges().hasChanges(), is(true));
        assertWarUpgraded(upgradeDifferential, "10.7", "12.0");
    }

    @Test
    public void calculateUpgradeDifferentialShouldReturnEmptyListIfOlderModules() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockOldestArtifactList());
        assertThat(upgradeDifferential.getWarChanges().hasChanges(), is(false));
        assertThat(upgradeDifferential.getModuleChanges().getAddedArtifacts(), is(empty()));
        assertThat(upgradeDifferential.getModuleChanges().getUpgradedArtifacts().values(), is(empty()));
        assertModuleDowngraded(upgradeDifferential, "webservices", "1.0", "0.7");
    }

    @Test
    public void calculateUpgradeDifferentialShouldReturnOnlyUpdateSnapshotsIfSameList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = calculateDifferential(getMockOldArtifactList(), getMockOldArtifactList());
        assertThat(upgradeDifferential.getWarChanges().hasChanges(), is(false));
        assertThat(upgradeDifferential.getModuleChanges().getAddedArtifacts(), is(empty()));
        assertThat(upgradeDifferential.getModuleChanges().getUpgradedArtifacts().keySet(), hasSize(1));
        assertModuleUpgraded(upgradeDifferential, "appui", "0.1-SNAPSHOT", "0.1-SNAPSHOT");
    }

    public void assertWarUpgraded(UpgradeDifferential differential, String fromVersion, String toVersion) {
        Map<Artifact, Artifact> m = differential.getWarChanges().getUpgradedArtifacts();
        assertThat(m.keySet(), hasSize(1));
        assertThat(m.keySet(), hasItem(new Artifact("openmrs-webapp", fromVersion, "org.openmrs.web")));
        assertThat(m.values(), hasItem(new Artifact("openmrs-webapp", toVersion, "org.openmrs.web")));
    }

    public void assertModuleAdded(UpgradeDifferential differential, String moduleId, String version) {
        assertThat(differential.getModuleChanges().getAddedArtifacts(), hasItem(new Artifact(moduleId + "-omod", version)));
    }

    public void assertModuleRemoved(UpgradeDifferential differential, String moduleId, String version) {
        assertThat(differential.getModuleChanges().getRemovedArtifacts(), hasItem(new Artifact(moduleId + "-omod", version)));
    }

    public void assertModuleUpgraded(UpgradeDifferential differential, String moduleId, String fromVersion, String toVersion) {
        assertThat(differential.getModuleChanges().getUpgradedArtifacts().keySet(), hasItem(new Artifact(moduleId + "-omod", fromVersion)));
        assertThat(differential.getModuleChanges().getUpgradedArtifacts().values(), hasItem(new Artifact(moduleId + "-omod", toVersion)));
    }

    public void assertModuleDowngraded(UpgradeDifferential differential, String moduleId, String fromVersion, String toVersion) {
        assertThat(differential.getModuleChanges().getDowngradedArtifacts().keySet(), hasItem(new Artifact(moduleId + "-omod", fromVersion)));
        assertThat(differential.getModuleChanges().getDowngradedArtifacts().values(), hasItem(new Artifact(moduleId + "-omod", toVersion)));
    }

    private UpgradeDifferential calculateDifferential(Properties serverProperties, Properties distroProperties) throws MojoExecutionException{
        DistroHelper distroHelper = new DistroHelper(null, null, null, null, null);
        return distroHelper.calculateUpdateDifferential(new Server(null, serverProperties), new DistroProperties(distroProperties));
    }

    private Properties getMockOldestArtifactList() {
        Properties properties = new Properties();
        properties.put("webapp.openmrs", "1.7");
        properties.put("omod.webservices", "0.7");
        return properties;
    }

    private Properties getMockOldArtifactList() {
        Properties properties = new Properties();
        properties.put("webapp.openmrs", "1.12");
        properties.put("omod.webservices", "1.0");
        properties.put("omod.owa", "1.5");
        properties.put("omod.legacyui", "1.5");
        properties.put("omod.appui", "0.1-SNAPSHOT");
        return properties;
    }

    private Properties getMockNewArtifactList() {
        Properties properties = new Properties();
        properties.put("webapp.openmrs", "1.12");
        properties.put("omod.webservices", "1.2");
        properties.put("omod.drugs", "0.2-SNAPSHOT");
        properties.put("omod.legacyui", "1.4");
        properties.put("omod.appui", "0.1-SNAPSHOT");
        return properties;
    }
}
