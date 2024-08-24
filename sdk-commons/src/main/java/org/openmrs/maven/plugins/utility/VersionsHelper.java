package org.openmrs.maven.plugins.utility;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource;
import org.openmrs.maven.plugins.model.Artifact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by user on 27.05.16.
 */
public class VersionsHelper {

    private static final String RELEASE_VERSION_REGEX = "[0-9.]+(-alpha)?(-beta)?";
    private static final String NO_VERSION_AVAILABLE_MSG = "No version is available in remote repositories!";

    /**
     * The project currently being build.
     */
    private final MavenProject mavenProject;

    /**
     * The current Maven session.

     */
    private final MavenSession mavenSession;

    /**
     * The artifact metadata source to use.

     */
    private final ArtifactMetadataSource artifactMetadataSource;

    /**
     * utility to create maven artifacts
     */
    private final ArtifactFactory artifactFactory;

    public VersionsHelper(ArtifactFactory artifactFactory, MavenProject mavenProject, MavenSession mavenSession, ArtifactMetadataSource artifactMetadataSource) {
        this.artifactFactory = artifactFactory;
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.artifactMetadataSource = artifactMetadataSource;
    }

    private List<ArtifactVersion> getVersions(Artifact artifact){
        try {
            return artifactMetadataSource.retrieveAvailableVersions(
                    artifactFactory.createArtifact(
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion(),
                            "", ""),
                    mavenSession.getLocalRepository(),
                    mavenProject.getRemoteArtifactRepositories()
            );
        } catch (ArtifactMetadataRetrievalException e) {
            //TODO
            return Collections.emptyList();
        }
    }

    public List<ArtifactVersion> getAllVersions(Artifact artifact, int maxSize) {
        List<ArtifactVersion> versions = getVersions(artifact);
        sortDescending(versions);
        System.out.println("Initial Versions: " + versions);
        Optional<ArtifactVersion> firstNonSnapshotVersion = versions.stream()
                .filter(version -> !version.toString().endsWith("-SNAPSHOT"))
                .findFirst();

        if (firstNonSnapshotVersion.isPresent()) {
            ArtifactVersion firstNonSnapshot = firstNonSnapshotVersion.get();
            versions.removeIf(version -> version.toString().endsWith("-SNAPSHOT")
                    && new ComparableVersion(version.toString()).compareTo(new ComparableVersion(firstNonSnapshot.toString())) < 0);
        }

        List<ArtifactVersion> result = versions.subList(0, Math.min(versions.size(), maxSize));
        System.out.println("Filtered Versions: " + result);

        return result;
    }

    private void sortDescending(List<ArtifactVersion> versions){
        Collections.sort(versions, (v1, v2) -> new ComparableVersion(v2.toString()).compareTo(new ComparableVersion(v1.toString())));
    }

    /**
     * @param artifact the Maven artifact to get the latest released version of
     * @return latest released version. if none is available, return snapshot. If list is empty, returns error message
     */
    public String getLatestReleasedVersion(Artifact artifact){
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
    public List<String> getSuggestedVersions(Artifact artifact, int maxReleases){
        return getSuggestedVersions(getVersions(artifact), maxReleases);
    }

    public List<String> getSuggestedVersions(List<ArtifactVersion> allVersions, int maxSize){
        if(allVersions.size() == 0){
            return Collections.emptyList();
        }
        sortDescending(allVersions);
        List<String> advices = new ArrayList<>();
        
        boolean secondSnaphotAdded = false;

        //add first element, presumably last SNAPSHOT version
        advices.add(allVersions.get(0).toString());
        ArtifactVersion previous = allVersions.get(0);
        //start from second element
        for(ArtifactVersion version : allVersions.subList(1, allVersions.size())){
            if(isNotFeatureVersion(version)){
                if(equalMinorAndMajor(previous, version)) {
                    if(isSnapshot(previous) && !isSnapshot(version)){
                        if(equalIncremental(previous, version)){
                            advices.remove(advices.size()-1);
                            advices.add(version.toString());
                        } else {
                            advices.add(version.toString());
                        }
                    } else if(isSnapshot(version) && !equalIncremental(previous, version)){
                        advices.add(version.toString());
                    }
                } else {
                    advices.add(version.toString());
                }
                previous = version;
            }
            else if(!secondSnaphotAdded && isSnapshot(version)) {
            	secondSnaphotAdded = true;
            	advices.add(version.toString());
            }
            if(advices.size() >= maxSize) break;
        }
        return advices;
    }

    private boolean equalIncremental(ArtifactVersion previous, ArtifactVersion version) {
        return version.getIncrementalVersion() == previous.getIncrementalVersion();
    }

    private boolean isNotFeatureVersion(ArtifactVersion version) {
        return version.toString().matches(RELEASE_VERSION_REGEX);
    }

    private boolean isSnapshot(ArtifactVersion version){
        return version.getQualifier()!= null && version.getQualifier().contains("SNAPSHOT");
    }
    private boolean equalMinorAndMajor(ArtifactVersion left, ArtifactVersion right){
        return left.getMajorVersion()==right.getMajorVersion()
                && left.getMinorVersion() == right.getMinorVersion();
    }
}
