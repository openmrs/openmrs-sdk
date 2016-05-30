package org.openmrs.maven.plugins.utility;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;

import java.util.*;

/**
 * Created by user on 27.05.16.
 */
public class VersionsHelper {

    private static final String RELEASE_VERSION_REGEX = "[0-9\\.]+(-alpha)?(-beta)?";
    private static final String SNAPSHOT_RELEASE_VERSION_REGEX = "[0-9\\.]+(-alpha)?(-beta)?(-SNAPSHOT)?";
    private static final String NONE_VERSION_AVAILABLE_MSG = "None version is available in remote repositories!";

    /**
     * The project currently being build.
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.

     */
    private MavenSession mavenSession;

    /**
     * The artifact metadata source to use.

     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * utility to create maven artifacts
     */
    private ArtifactFactory artifactFactory;

    public VersionsHelper(ArtifactFactory artifactFactory, MavenProject mavenProject, MavenSession mavenSession, ArtifactMetadataSource artifactMetadataSource) {
        this.artifactFactory = artifactFactory;
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.artifactMetadataSource = artifactMetadataSource;
    }

    /**
     * @param artifact
     * @return available versions from remote repositories for given artifact
     */
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

    private void sortDescending(List<ArtifactVersion> versions){
        Collections.sort(versions);
        Collections.reverse(versions);
    }

    /**
     * @param artifact
     * @return inferred version for given artifact
     */
    public String inferVersion(Artifact artifact){
        List<ArtifactVersion> artifactVersions = getVersions(artifact);
        if(artifactVersions.size() == 0){
            return artifact.getVersion();
        }
        return inferVersion(artifact.getVersion(), artifactVersions);
    }

    /**
     *
     * @param currentVersion
     * @param availableVersions
     * @return inferred version based on available versions
     */
    public String inferVersion(String currentVersion, List<ArtifactVersion> availableVersions){
        List<ArtifactVersion> inferredVersions = new ArrayList<ArtifactVersion>();
        //if currentVersion is available
        for(ArtifactVersion version : availableVersions){
            if(version.toString().equals(currentVersion)){
                return currentVersion;
            } else if (version.toString().contains(currentVersion)&&version.toString().matches(RELEASE_VERSION_REGEX)){
                inferredVersions.add(version);
            }
        }
        if(inferredVersions.size()==0){
            return currentVersion;
        }
        Collections.sort(inferredVersions);
        return inferredVersions.get(inferredVersions.size()-1).toString();
    }

    /**
     * @param artifact
     * @return latest released version. if none is available, return snapshot. If list is empty, returns message
     */
    public String getLatestReleasedVersion(Artifact artifact){
        return getLatestReleasedVersion(getVersions(artifact));
    }
    public String getLatestReleasedVersion(List<ArtifactVersion> versions){
        sortDescending(versions);
        ArtifactVersion lastSnapshot = null;
        for(ArtifactVersion version : versions){
            if(version.getQualifier()==null)
                return version.toString();
            else if(lastSnapshot==null){
                lastSnapshot = version;
            }
        }
        //no releases, return snapshot
        if(lastSnapshot != null) return lastSnapshot.toString();
        //no releases nor snapshots, return any last version
        if(versions.size() > 0){
            return versions.get(0).toString();
        }
        else return NONE_VERSION_AVAILABLE_MSG;
    }

    /**
     * @param artifact
     * @param maxReleases upper limit of number of releases
     * @return list of suggested versions
     */
    public List<String> getVersionAdvice(Artifact artifact, int maxReleases){
        return getVersionAdvice(getVersions(artifact), maxReleases);

    }

    public List<String> getVersionAdvice(List<ArtifactVersion> allVersions, int maxSize){
        if(allVersions.size() == 0){
            return Collections.emptyList();
        }
        sortDescending(allVersions);
        List<String> advices = new ArrayList<String>();

        //add first element
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
            if(advices.size() >= maxSize) break;
        }
        return advices;
    }

    private boolean equalIncremental(ArtifactVersion previous, ArtifactVersion version) {
        return version.getIncrementalVersion() == previous.getIncrementalVersion();
    }

    private boolean isNotFeatureVersion(ArtifactVersion version) {
        return version.toString().matches(SNAPSHOT_RELEASE_VERSION_REGEX);
    }

    private boolean isSnapshot(ArtifactVersion version){
        return version.getQualifier()!= null && version.getQualifier().contains("SNAPSHOT");
    }
    private boolean equalMinorAndMajor(ArtifactVersion left, ArtifactVersion right){
        return left.getMajorVersion()==right.getMajorVersion()
                && left.getMinorVersion() == right.getMinorVersion();
    }
}
