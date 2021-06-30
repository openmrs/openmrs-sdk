package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

@Mojo(name = "fetch", requiresProject = false)
public class Fetch extends AbstractTask {

	private static final String FETCH_MODULE_OPTION = "Module";

	private static final String FETCH_OWA_OPTION = "Open Web App";

	private static final String ERROR_INVALID_VERSION = "Invalid version number";

	private static final String DEFAULT_GROUP_ID = "org.openmrs.module";

	@Parameter(property = "artifactId")
	private String artifactId;

	@Parameter(property = "groupId")
	private String groupId;

	@Parameter(property = "version")
	private String version;

	@Parameter(property = "owa")
	private String owa;

	@Parameter(property = "dir")
	private String dir;

	private String projectName;

	private File downloadDirectory;

	private enum ProjectType {
		OWA, MODULE
	}

	private ProjectType projectType;

	public void executeTask() throws MojoExecutionException {
		if (StringUtils.isBlank(groupId)) {
			groupId = DEFAULT_GROUP_ID;
		}
		getData();
		switch (projectType) {
			case MODULE:
				fetchModule(projectName, groupId, version, downloadDirectory);
				break;
			case OWA:
				fetchOwa(projectName, version, downloadDirectory);
				break;
		}
	}

	private void getData() {
		downloadDirectory = resolveDownloadDirectory(dir);

		projectType = resolveProjectType(artifactId, owa);

		projectName = resolveProjectName(artifactId, owa);

		if (StringUtils.isBlank(version)) {
			version = resolveVersion(projectName);
			if (StringUtils.isBlank(version)) {
				switch (projectType) {
					case MODULE:
						version = askForMavenRepoProjectVersion(projectName, groupId);
						break;
					case OWA:
						version = askForMavenRepoProjectVersion(projectName, "org.openmrs.owa");
						break;
				}
			}
		}

	}

	private File resolveDownloadDirectory(String dirPath) {
		//Resolve project directory if undefined
		if (StringUtils.isBlank(dirPath)) {
			dirPath = System.getProperty("user.dir");
		}
		return new File(dirPath);
	}

	private ProjectType resolveProjectType(String artifactId, String owa) {
		ProjectType result = null;

		//Resolve project type if defined
		if (StringUtils.isNotBlank(artifactId)) {
			result = ProjectType.MODULE;
		} else if (StringUtils.isNotBlank(owa)) {
			result = ProjectType.OWA;
		}

		//Resolve project type if undefined
		if (result == null) {
			List<String> options = new ArrayList<>(Arrays.asList(
					FETCH_MODULE_OPTION,
					FETCH_OWA_OPTION
			));

			String choice = wizard.promptForMissingValueWithOptions("What would you like to fetch?%s", null, "", options);
			switch (choice) {
				case FETCH_MODULE_OPTION:
					result = ProjectType.MODULE;
					break;
				case FETCH_OWA_OPTION:
					result = ProjectType.OWA;
					break;
			}
		}
		return result;
	}

	private String resolveProjectName(String artifactId, String owa) {
		//Resolve project type and name if defined
		if (StringUtils.isNotBlank(artifactId)) {
			return artifactId;
		} else if (ProjectType.OWA.equals(projectType)) {
			return wizard.promptForValueIfMissing(owa, "owa");
		} else {
			return wizard.promptForValueIfMissing(artifactId, "artifactId");
		}
	}

	private String resolveVersion(String projectName) {
		//Check if there is version number in project name
		if (projectName.contains(":")) {
			String substringName = (projectName.split(":"))[0];
			String substringVersion = (projectName.split(":"))[1];
			if (!substringVersion.isEmpty()) {
				this.projectName = substringName;
				return substringVersion;
			} else {
				throw new IllegalArgumentException(ERROR_INVALID_VERSION);
			}
		}
		return null;
	}

	private String askForMavenRepoProjectVersion(String projectName, String groupId) {
		List<String> availableVersions = versionsHelper.getSuggestedVersions(new Artifact(projectName, "1.0", groupId), 5);
		return wizard.promptForMissingValueWithOptions(
				"You can fetch the following versions of the module", version, "version", availableVersions,
				"Please specify module version", null);
	}

	private void fetchOwa(String projectName, String version, File downloadDirectory) throws MojoExecutionException {
		List<MojoExecutor.Element> owaItems = new ArrayList<>();
		Artifact artifact = new Artifact(projectName, version, "org.openmrs.owa", Artifact.TYPE_ZIP);
		owaItems.add(artifact.toElement(downloadDirectory.getPath()));
		executeMojoPlugin(owaItems);
	}

	private void fetchModule(String artifactId, String groupId, String version, File downloadDirectory)
			throws MojoExecutionException {
		if (!artifactId.endsWith("-" + Artifact.TYPE_OMOD)) {
			artifactId = artifactId + "-" + Artifact.TYPE_OMOD;
		}
		List<MojoExecutor.Element> artifactItems = new ArrayList<>();
		Artifact artifact = new Artifact(artifactId, version, groupId, Artifact.TYPE_JAR, Artifact.TYPE_OMOD);
		artifactItems.add(artifact.toElement(downloadDirectory.getPath()));
		executeMojoPlugin(artifactItems);
	}

	private void executeMojoPlugin(List<MojoExecutor.Element> artifactItems) throws MojoExecutionException {
		executeMojo(
				plugin(
						groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
						artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
						version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
				),
				goal("copy"),
				configuration(
						element(name("artifactItems"), artifactItems.toArray(new MojoExecutor.Element[0]))
				),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}
}
