package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.StringUtils;
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
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
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

	public File downloadArtifact(Artifact artifact, File directory) throws MojoExecutionException {
		return downloadArtifact(artifact, directory, null);
	}

	/**
	 * Downloads the given artifact to the given directory with the given fileName.  If fileName is null, it will use the maven default.
	 * @param artifact the artifact to download
	 * @param directory the directory into which to download the artifact
	 * @param fileName the name of the file to save the artifact to (optional, if null will use the maven default)
	 * @return the downloaded File
	 * @throws MojoExecutionException
	 */
	public File downloadArtifact(Artifact artifact, File directory, String fileName) throws MojoExecutionException {
		if (StringUtils.isBlank(fileName)) {
			fileName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType();
		}
		artifact.setDestFileName(fileName);
		List<MojoExecutor.Element> artifactItems = new ArrayList<>();
		MojoExecutor.Element element = artifact.toElement(directory.getAbsolutePath());
		artifactItems.add(element);
		executeMojo(
				plugin(
						groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
				),
				goal("copy"),
				configuration(
						element(name("artifactItems"), artifactItems.toArray(new MojoExecutor.Element[0]))
				),
				executionEnvironment(
						mavenEnvironment.getMavenProject(),
						mavenEnvironment.getMavenSession(),
						mavenEnvironment.getPluginManager()
				)
		);
		return new File(directory, artifact.getDestFileName());
	}
}
