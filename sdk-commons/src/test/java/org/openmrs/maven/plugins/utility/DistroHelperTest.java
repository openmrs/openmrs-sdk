package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DistroHelperTest {

    @Mock
    VersionsHelper versionsHelper;

    @Before
    public void setupMocks() {
        when(versionsHelper.getLatestSnapshotVersion((Artifact) any())).thenReturn("RESOLVED-LATEST-SNAPSHOT");
        when(versionsHelper.getLatestReleasedVersion((Artifact) any())).thenReturn("RESOLVED-LATEST-RELEASE");
    }

    @Test
    public void parseDistroArtifactShouldInferArtifactIdForRefapp() throws Exception{
        String distro = "referenceapplication:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is(Artifact.GROUP_DISTRO));
        assertThat(artifact.getArtifactId(), is(SDKConstants.REFAPP_2X_ARTIFACT_ID));
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
    public void parseDistroArtifact_shouldNormalizeArtifactBeforeReturning() throws MojoExecutionException {
        Artifact artifact = DistroHelper.parseDistroArtifact("org.openmrs.module:htmlformentry:2.0.0", versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry-omod"));
    }

    @Test
    public void calculateUpdateDifferentialShouldFindArtifactsToAddList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockNewArtifactList());

        assertThat(upgradeDifferential.getModulesToAdd(), hasItem(new Artifact("drugs", "0.2-SNAPSHOT")));
        assertThat(upgradeDifferential.getModulesToAdd(), hasSize(1));
    }

    @Test
    public void calculateUpdateDifferentialShouldFindArtifactsToDeleteList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockNewArtifactList());

        assertThat(upgradeDifferential.getModulesToDelete(), hasItem(new Artifact("owa", "1.5")));
        assertThat(upgradeDifferential.getModulesToAdd(), hasSize(1));
    }

    @Test
    public void calculateUpdateDifferentialShouldAddSnapshotToUpdateMap() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockNewArtifactList());

        assertThat(upgradeDifferential.getUpdateOldToNewMap().keySet(), hasItem(new Artifact("appui", "0.1-SNAPSHOT")));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().keySet(), hasItem(new Artifact("webservices","1.0")));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().values(), hasItem(new Artifact("webservices","1.2")));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().keySet(), hasSize(2));
    }

    @Test
    public void calculateUpdateDifferentialShouldAddSnapshotToDowngradeMap() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockNewArtifactList());

        assertThat(upgradeDifferential.getDowngradeNewToOldMap().keySet(), hasItem(new Artifact("legacyui","1.5")));
        assertThat(upgradeDifferential.getDowngradeNewToOldMap().values(), hasItem(new Artifact("legacyui","1.4")));
        assertThat(upgradeDifferential.getDowngradeNewToOldMap().keySet(), hasSize(1));
    }

    @Test
    public void calculateUpdateDifferentialShouldFindPlatformUpdate() throws MojoExecutionException {
        Artifact oldPlatform = new Artifact("openmrs-webapp", "10.7", Artifact.GROUP_WEB, Artifact.TYPE_WAR);
        Artifact newPlatform = new Artifact("openmrs-webapp", "12.0", Artifact.GROUP_WEB, Artifact.TYPE_WAR);

        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(
                Collections.singletonList(oldPlatform),
                Collections.singletonList(newPlatform));
        assertThat(upgradeDifferential.getPlatformArtifact(), is(newPlatform));
    }

    @Test(expected = MojoExecutionException.class)
    public void calculateUpdateDifferentialShouldNotAllowToDeletePlatform() throws MojoExecutionException {
        Artifact oldPlatform = new Artifact("openmrs-webapp", "10.7", Artifact.GROUP_WEB, Artifact.TYPE_WAR);
        Artifact artifact = new Artifact("owa", "12.0");

        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(
                Collections.singletonList(oldPlatform),
                Collections.singletonList(artifact));
    }

    @Test
    public void calculateUpgradeDifferentialShouldReturnEmptyListIfOlderModules() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockOldestArtifactList());

        assertThat(upgradeDifferential.getPlatformArtifact(), is(nullValue()));
        assertThat(upgradeDifferential.getModulesToAdd(), is(empty()));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().values(), is(empty()));
    }

    @Test
    public void calculateUpgradeDifferentialShouldReturnOnlyUpdateSnapshotsIfSameList() throws MojoExecutionException {
        UpgradeDifferential upgradeDifferential = DistroHelper.calculateUpdateDifferential(getMockOldArtifactList(), getMockOldArtifactList());

        assertThat(upgradeDifferential.getPlatformArtifact(), is(nullValue()));
        assertThat(upgradeDifferential.getModulesToAdd(), is(empty()));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().keySet(), hasSize(1));
        assertThat(upgradeDifferential.getUpdateOldToNewMap().keySet(), hasItem(new Artifact("appui", "0.1-SNAPSHOT")));
    }

    @Test
    public void normalizeArtifact_shouldAppendOmodToModuleArtifactIds() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("htmlformentry", "2.0.0", "org.openmrs.module"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry-omod"));
    }

    @Test
    public void normalizeArtifact_shouldNotAppendOmodToNonModuleArtifactIds() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("htmlformentry", "2.0.0", "org.pih.module"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp2() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "2.6.1", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("referenceapplication-package"));
        assertThat(artifact.getType(), equalTo("jar"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp3Beta() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "3.0.0-alpha", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("referenceapplication-distro"));
        assertThat(artifact.getType(), equalTo("zip"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp3() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "3.0.0", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("distro-emr-configuration"));
        assertThat(artifact.getGroupId(), equalTo("org.openmrs"));
        assertThat(artifact.getType(), equalTo("zip"));
    }

    @Test
    public void normalizeArtifact_shouldResolveLatestSnapshotVersion() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("a", "LATEST", "b"), versionsHelper);
        assertThat(artifact.getVersion(), equalTo("RESOLVED-LATEST-RELEASE"));
    }

    @Test
    public void normalizeArtifact_shouldResolveLatestReleaseVersion() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("a", "LATEST-SNAPSHOT", "b"), versionsHelper);
        assertThat(artifact.getVersion(), equalTo("RESOLVED-LATEST-SNAPSHOT"));
    }

    @Test
    public void normalizeArtifact_shouldNotChangeByDefault() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("my-artifact", "my-version", "my-group", "my-type"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("my-artifact"));
        assertThat(artifact.getGroupId(), equalTo("my-group"));
        assertThat(artifact.getVersion(), equalTo("my-version"));
        assertThat(artifact.getType(), equalTo("my-type"));
    }

    private List<Artifact> getMockOldestArtifactList(){
        List<Artifact> oldList = new ArrayList<>(Arrays.asList(
                new Artifact("webservices", "0.7"),
                new Artifact("webapp", "1.7"),
                new Artifact("openmrs-webapp", "10.7", Artifact.GROUP_WEB, Artifact.TYPE_WAR)
        ));

        return oldList;
    }

    private List<Artifact> getMockOldArtifactList(){
        List<Artifact> oldList = new ArrayList<>(Arrays.asList(
                new Artifact("webservices", "1.0"),
                new Artifact("webapp", "1.12"),
                new Artifact("owa", "1.5"),
                new Artifact("legacyui", "1.5"),
                new Artifact("appui", "0.1-SNAPSHOT"),
                new Artifact("openmrs-webapp", "10.7", Artifact.GROUP_WEB, Artifact.TYPE_WAR)
        ));

        return oldList;
    }

    private List<Artifact> getMockNewArtifactList(){
        List<Artifact> oldList = new ArrayList<>(Arrays.asList(
                new Artifact("webservices", "1.2"),
                new Artifact("webapp", "1.12"),
                new Artifact("appui", "0.1-SNAPSHOT"),
                new Artifact("legacyui", "1.4"),
                new Artifact("drugs", "0.2-SNAPSHOT"),
                new Artifact("openmrs-webapp", "12.0", Artifact.GROUP_WEB, Artifact.TYPE_WAR)
        ));
        return oldList;
    }
}
