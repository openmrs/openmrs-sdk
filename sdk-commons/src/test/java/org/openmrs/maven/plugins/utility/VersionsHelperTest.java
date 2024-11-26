package org.openmrs.maven.plugins.utility;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class VersionsHelperTest {

    VersionsHelper helper;

    @Before
    public void before() {
        helper = new VersionsHelper(null, null, null, null);
    }

    private List<ArtifactVersion> createTestVersions(String... versions) {
        List<ArtifactVersion> list = new ArrayList<>();
        for (String version : versions) {
            list.add(new DefaultArtifactVersion(version));
        }
        return list;
    }

    @Test
    public void getLatestReleasedVersion_shouldNotIncludeSnapshots() {
        String res = helper.getLatestReleasedVersion(
                createTestVersions("1.8.8", "1.8.3", "1.8.10-SNAPSHOT", "1.8.9-alpha"));
        assertThat(res, is(equalTo("1.8.8")));
    }

    @Test
    public void getLatestSnapshotVersion_shouldReturnLatestSnapshot() {
        String res = helper.getLatestSnapshotVersion(
                createTestVersions("1.8.8", "1.8.3", "1.8.10-SNAPSHOT", "1.8.9-alpha"));
        assertThat(res, is(equalTo("1.8.10-SNAPSHOT")));
    }

    @Test
    public void getSuggestedVersions_containSnapshots() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.8.8", "1.8.3", "1.8.10-SNAPSHOT", "1.8.9-alpha"), 3);
        assertThat(res, hasItem("1.8.10-SNAPSHOT"));
    }

    @Test
    public void getSuggestedVersions_containGivenNumberOfReleases() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.8.8", "1.8.3", "1.8.10-SNAPSHOT", "1.8.9-alpha"), 1);
        assertThat(res, hasItem("1.8.10-SNAPSHOT"));
        assertThat(res, allOf(not(hasItem("1.8.3")), not(hasItem("1.8.9"))));
    }

    @Test
    public void getSuggestedVersions_shouldReturnSortedList() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.1.2", "1.5.6", "1.5.7-SNAPSHOT", "1.1.2-alpha", "1.1.0-SNAPSHOT", "1.3.0-SNAPSHOT", "1.3.0", "1.1.0"), 6);
        assertThat(res, contains("1.5.7-SNAPSHOT", "1.5.6", "1.3.0", "1.1.2"));
    }

    @Test
    public void getSuggestedVersions_shouldContainOnlyLatestTwoSnapshots() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.1.2", "1.5.6", "1.5.7-SNAPSHOT", "1.4.5-SNAPSHOT", "1.1.0-SNAPSHOT", "1.3.0-SNAPSHOT"), 6);
        assertThat(res, contains("1.5.7-SNAPSHOT", "1.5.6", "1.4.5-SNAPSHOT", "1.1.2"));
    }

    @Test
    public void getSuggestedVersions_shouldContainNoMoreThanRequestedQuantity() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.4.0", "1.5.0", "1.8.0"), 2);
        assertThat(res, contains("1.8.0", "1.5.0"));
    }

    @Test
    public void getSuggestedVersions_shouldContainOnlyOneReleaseFromMajorAndMinor() {
        List<String> res = helper.getSuggestedVersions(
                createTestVersions("1.5.2", "1.5.6", "1.5.7-SNAPSHOT"), 6);
        assertThat(res, contains("1.5.7-SNAPSHOT", "1.5.6"));
    }
}
