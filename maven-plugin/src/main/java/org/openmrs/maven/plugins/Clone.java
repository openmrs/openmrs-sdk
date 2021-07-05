package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "clone", requiresProject = false)
public class Clone extends AbstractTask {

	public static final String GITHUB_COM = "github.com";

	public static final String GITHUB_BASE_URL = "https://github.com/";

	@Parameter(property = "groupId")
	private String groupId;

	@Parameter(property = "artifactId")
	private String artifactId;

	@Parameter(property = "githubUsername")
	private String username;

	@Parameter(property = "githubPassword", alias = "personalAccessToken")
	private String personalAccessToken;

	@Override
	public void executeTask() throws MojoExecutionException {
		groupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", "org.openmrs.module");
		artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");

		String version = versionsHelper.getLatestReleasedVersion(new Artifact(artifactId, "0", groupId));
		String repoUrl;

		repoUrl = extractGitHubHttpKeyFromModulePom(artifactId, version, groupId);

		username = wizard.promptForValueIfMissing(username, "your GitHub username");
		personalAccessToken = wizard
				.promptForPasswordIfMissing(personalAccessToken, "your GitHub personal access token");

		cloneRepo(repoUrl);
	}

	private String extractGitHubHttpKeyFromModulePom(String artifactId, String version, String groupId)
			throws MojoExecutionException {
		downloadModulePom(new Artifact(artifactId, version, groupId, "pom"));
		String pomFileName = this.artifactId + "-" + version + ".pom";
		File pomDir = new File("pom/");
		File pom = new File("pom/", pomFileName);
		Model pomProperties = Project.loadProject(pomDir, pomFileName).getModel();
		String url = pomProperties.getScm().getUrl();
		pom.delete();
		pomDir.delete();

		return extractUniversalRepoUrl(url);
	}

	/**
	 * This method is modifying repoUrl extracted from OpenMRS' project's pom.xml
	 * There are some differences in repoUrl syntax for example:
	 * git@github.com:openmrs/openmrs-contrib-uitestframework.git
	 * or https://github.com/openmrs/openmrs-module-webservices.rest.git
	 * This method ensures that repoUrl will be always the same.
	 */
	private String extractUniversalRepoUrl(String repoUrl) {
		String result;
		result = repoUrl.substring(repoUrl.indexOf(GITHUB_COM) + GITHUB_COM.length() + 1);

		result = StringUtils.removeEnd(result, "/");

		if (!repoUrl.endsWith(".git")) {
			return result + ".git";
		} else {
			return result;
		}
	}

	private void downloadModulePom(Artifact artifact) throws MojoExecutionException {
		MojoExecutor.Element[] artifactItems = new MojoExecutor.Element[1];
		artifactItems[0] = artifact.toElement("pom/");
		List<Element> configuration = new ArrayList<>();
		configuration.add(element("artifactItems", artifactItems));
		executeMojo(
				plugin(
						groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
				),
				goal("copy"),
				configuration(configuration.toArray(new Element[0])),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}

	private void forkRepo(String repoName, String repoOwner) throws MojoExecutionException {
		wizard.showMessage("Forking " + repoName + " from " + repoOwner);
		GitHubClient client = new GitHubClient();
		client.setCredentials(username, personalAccessToken);
		RepositoryService service = new RepositoryService();
		service.getClient().setCredentials(username, personalAccessToken);
		RepositoryId toBeForked = new RepositoryId(repoOwner, repoName);
		try {
			service.forkRepository(toBeForked);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to fork repository", e);
		}
	}

	private void cloneRepo(String repoUrl) throws MojoExecutionException {
		String repoOwner = repoUrl.substring(0, repoUrl.indexOf("/"));
		String originUrl = StringUtils.replaceOnce(repoUrl, repoOwner, username);

		String repoName = repoUrl.substring(repoOwner.length() + 1, repoUrl.lastIndexOf(".git"));

		if (!testMode) {
			forkRepo(repoName, repoOwner);
		}

		File localPath = new File(repoName);
		wizard.showMessage("Cloning from " + originUrl + " into " + localPath.getAbsolutePath());
		if (localPath.exists()) {
			throw new MojoExecutionException("Destination path \"" + localPath.getAbsolutePath() + "\" already exists.");
		}

		try {
			Git.cloneRepository()
					.setURI(GITHUB_BASE_URL + originUrl)
					.setDirectory(localPath)
					.call();
			Git git = new Git(gitHelper.getLocalRepository(localPath.getAbsolutePath()));
			gitHelper.addRemoteUpstream(git, localPath.getAbsolutePath());
		}
		catch (Exception e) {
			throw new MojoExecutionException("Failed to clone repository", e);
		}
	}
}
