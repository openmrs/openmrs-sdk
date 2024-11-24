package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
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
		}
	}
}
