package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * The purpose of this class is to handle all interactions with Maven that require retrieving artifacts from the Maven repository
 */
public class ArtifactHelper {

	private static final Logger log = LoggerFactory.getLogger(ArtifactHelper.class);

	final MavenEnvironment mavenEnvironment;

	public ArtifactHelper(MavenEnvironment mavenEnvironment) {
		this.mavenEnvironment = mavenEnvironment;
	}

	/**
	 * Downloads the given artifact to the given directory with the given fileName.  If fileName is null, it will use the maven default.
	 * @param artifact the artifact to download
	 * @param directory the directory into which to download the artifact
	 * @param unpack if true will unzip the artifact in the given directory, otherwise will not unpack it
	 * @throws MojoExecutionException if there are errors
	 */
	public void downloadArtifact(Artifact artifact, File directory, boolean unpack) throws MojoExecutionException {
		try (TempDirectory markersDirectory = TempDirectory.create("markers")) {

			String goal = "copy";
			List<MojoExecutor.Element> configuration = new ArrayList<>();
			configuration.add(element("artifactItems", artifact.toElement(directory.getAbsolutePath())));
			configuration.add(element("overWriteSnapshots", "true"));
			configuration.add(element("overWriteReleases", "true"));
			if (unpack) {
				goal = "unpack";
				configuration.add(element("markersDirectory", markersDirectory.getAbsolutePath()));
			}

			executeMojo(
					plugin(
							groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
							artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
							version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
					),
					goal(goal),
					configuration(configuration.toArray(new MojoExecutor.Element[0])),
					executionEnvironment(
							mavenEnvironment.getMavenProject(),
							mavenEnvironment.getMavenSession(),
							mavenEnvironment.getPluginManager()
					)
			);

			if (!unpack && mavenEnvironment.isVerifySignatures()) {
				File artifactFile = new File(directory, artifact.getDestFileName());
				verifyAlreadyDownloaded(artifact, artifactFile, directory);
			}
		}
	}

	public void verifyAlreadyDownloaded(Artifact artifact, File artifactFile, File directory) throws MojoExecutionException {
		Artifact ascArtifact = makeAscArtifact(artifact);
		File ascFile = new File(directory, ascArtifact.getDestFileName());

		boolean signatureFetched = tryFetchSignature(ascArtifact, directory);

		if (!signatureFetched || !ascFile.exists()) {
			log.warn("Artifact {} is not signed (pre-backfill release). Skipping verification.", artifact);
			return;
		}

		try {
			SignatureVerifier.verify(artifactFile, ascFile);
			log.info("Verified signature for {}", artifact);
		}
		catch (SecurityException e) {
			throw new MojoExecutionException("Signature verification failed for " + artifact + ": " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Signature verification error for " + artifact + ": " + e.getMessage(), e);
		}
		finally {
			if (!ascFile.delete()) {
				ascFile.deleteOnExit();
			}
		}
	}

	private boolean tryFetchSignature(Artifact ascArtifact, File directory) {
		try {
			List<MojoExecutor.Element> configuration = new ArrayList<>();
			configuration.add(element("artifactItems", ascArtifact.toElement(directory.getAbsolutePath())));
			configuration.add(element("overWriteSnapshots", "true"));
			configuration.add(element("overWriteReleases", "true"));

			executeMojo(
					plugin(
							groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
							artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
							version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
					),
					goal("copy"),
					configuration(configuration.toArray(new MojoExecutor.Element[0])),
					executionEnvironment(
							mavenEnvironment.getMavenProject(),
							mavenEnvironment.getMavenSession(),
							mavenEnvironment.getPluginManager()
					)
			);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static Artifact makeAscArtifact(Artifact artifact) {
		Artifact asc = new Artifact();
		asc.setGroupId(artifact.getGroupId());
		asc.setArtifactId(artifact.getArtifactId());
		asc.setVersion(artifact.getVersion());
		asc.setClassifier(artifact.getClassifier());
		asc.setType(artifact.getType() + ".asc");
		asc.setFileExtension(artifact.getFileExtension() + ".asc");
		asc.setDestFileName(artifact.getDestFileName() + ".asc");
		return asc;
	}
}
