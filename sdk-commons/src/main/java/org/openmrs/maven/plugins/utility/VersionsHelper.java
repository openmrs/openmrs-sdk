package org.openmrs.maven.plugins.utility;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.openmrs.maven.plugins.model.Artifact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by user on 27.05.16.
 */
public class VersionsHelper {

    public static final String NO_VERSION_AVAILABLE_MSG = "No version is available in remote repositories!";

    private final MavenEnvironment mavenEnvironment;

    public VersionsHelper(MavenEnvironment mavenEnvironment) {
        this.mavenEnvironment = mavenEnvironment;
    }

    private List<ArtifactVersion> getVersions(Artifact artifact) {
        try {
            return mavenEnvironment.getArtifactMetadataSource().retrieveAvailableVersions(
                    mavenEnvironment.getArtifactFactory().createArtifact(
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion(),
                            "", ""),
                    mavenEnvironment.getMavenSession().getLocalRepository(),
                    mavenEnvironment.getMavenProject().getRemoteArtifactRepositories()
            );
        } catch (ArtifactMetadataRetrievalException e) {
            //TODO
            return Collections.emptyList();
        }
    }

    private void sortDescending(List<ArtifactVersion> versions) {
        versions.sort((v1, v2) -> new ComparableVersion(v2.toString()).compareTo(new ComparableVersion(v1.toString())));
    }

    /**
     * @param artifact the Maven artifact to get the latest released version of
     * @return latest released version. if none is available, return snapshot. If list is empty, returns error message
     */
    public String getLatestReleasedVersion(Artifact artifact) {
        return getLatestReleasedVersion(getVersions(artifact));
    }

    /**
     * @param artifact the Maven artifact to get the latest snapshot version of
     * @return latest snapshot. If list is empty, returns error message
     */
    public String getLatestSnapshotVersion(Artifact artifact) {
        return getLatestSnapshotVersion(getVersions(artifact));
    }

    public String getLatestSnapshotVersion(List<ArtifactVersion> versions) {
        sortDescending(versions);
        for(ArtifactVersion version : versions){
            if (version.toString().contains("SNAPSHOT")) {
                return version.toString();
            }
        }
        return NO_VERSION_AVAILABLE_MSG;
    }

    public String getLatestReleasedVersion(List<ArtifactVersion> versions){
        sortDescending(versions);
        ArtifactVersion lastSnapshot = null;
        for (ArtifactVersion version : versions) {
            if(version.getQualifier() == null)
                return version.toString();
            else if (lastSnapshot == null){
                lastSnapshot = version;
            }
        }

        // no releases, return snapshot
        if (lastSnapshot != null) {
            return lastSnapshot.toString();
        }

        // no releases nor snapshots, return any last version
        return NO_VERSION_AVAILABLE_MSG;
    }

    /**
     * @param artifact the artifact to get the suggested versions for
     * @param maxReleases upper limit of number of releases
     * @return list of suggested versions
     */
    public List<String> getSuggestedVersions(Artifact artifact, Integer maxReleases){
        return getSuggestedVersions(getVersions(artifact), maxReleases);
    }

    public List<String> getSuggestedVersions(List<ArtifactVersion> allVersions, Integer maxSize) {
        if(allVersions.isEmpty()){
            return Collections.emptyList();
        }
        sortDescending(allVersions);
        List<String> advices = new ArrayList<>();
        
        boolean secondSnaphotAdded = false;

        //add first element, presumably last SNAPSHOT version
        advices.add(allVersions.get(0).toString());
        ArtifactVersion previous = allVersions.get(0);
        //start from second element
        for (int i=1; i<allVersions.size(); i++) {
            ArtifactVersion version = allVersions.get(i);
            if (!isSnapshot(version)) {
                //add all releases unless there is already a later release with the same major.minor version
                if (!equalMinorAndMajor(previous, version) || isSnapshot(previous)) {
                    advices.add(version.toString());
                    previous = version;
                }
            }
            else {
                //only add one snapshot prior to the latest snaphost
                if (!equalMinorAndMajor(previous, version) && !secondSnaphotAdded) {
                    advices.add(version.toString());
                    secondSnaphotAdded = true;
                }
            }
        }
        if (maxSize != null && advices.size() > maxSize) {
            advices = advices.subList(0, maxSize);
        }
        return advices;
    }

    private boolean equalMinorAndMajor(ArtifactVersion left, ArtifactVersion right){
        return left.getMajorVersion() == right.getMajorVersion() && left.getMinorVersion() == right.getMinorVersion();
    }

    private boolean isSnapshot(ArtifactVersion version){
        return version.getQualifier() != null && version.getQualifier().contains("SNAPSHOT");
    }
}
