package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

/**
 *  @goal clone
 *  @requiresProject false
 */
public class Clone extends AbstractTask {

    public static final String GITHUB_COM = "https://github.com/";
    /**
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${githubUsername}"
     */
    private String githubUsername;

    /**
     * @parameter expression="${githubPassword}"
     */
    private String githubPassword;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        groupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", "org.openmrs.module");
        artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");

        String version = versionsHelper.getLatestReleasedVersion(new Artifact(artifactId, "0", groupId));
        String repoUrl;

        try {
            repoUrl = extractGitHubHttpKeyFromModulePom(artifactId, version, groupId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch scm url from maven repository", e);
        }

        githubUsername = wizard.promptForValueIfMissing(githubUsername, "your GitHub username");
        githubPassword = wizard.promptForPasswordIfMissing(githubPassword, "your GitHub password");

        cloneRepo(repoUrl);
    }

    private String extractGitHubHttpKeyFromModulePom(String artifactId, String version, String groupId) throws MojoExecutionException, IOException {
        downloadModulePom(new Artifact(artifactId, version, groupId, "pom"));
        String pomFileName = this.artifactId+"-"+version+".pom";
        File pomDir = new File("pom/");
        File pom = new File("pom/", pomFileName);
        Model pomProperties = Project.loadProject(pomDir, pomFileName).getModel();
        String url = pomProperties.getScm().getUrl();
        pom.delete();
        pomDir.delete();
        if (!url.endsWith(".git")) {
            return StringUtils.removeEnd(url, "/") + ".git";
        } else {
            return url;
        }
    }

    private void downloadModulePom(Artifact artifact) throws MojoExecutionException {
        MojoExecutor.Element[] artifactItems = new MojoExecutor.Element[1];
        artifactItems[0] = artifact.toElement("pom/");
        List<Element> configuration = new ArrayList<Element>();
        configuration.add(element("artifactItems", artifactItems));
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private void forkRepo(String repoName, String repoOwner) {
        wizard.showMessage("Forking " + repoName + " from " + repoOwner);
        GitHubClient client = new GitHubClient();
        client.setCredentials(githubUsername, githubPassword);
        RepositoryService service = new RepositoryService();
        service.getClient().setCredentials(githubUsername, githubPassword);
        RepositoryId toBeForked = new RepositoryId(repoOwner, repoName);
        try {
            service.forkRepository(toBeForked);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fork repository", e);
        }
    }

    private void cloneRepo(String repoUrl) {
        String repoOwner = repoUrl.substring(repoUrl.indexOf(GITHUB_COM) + GITHUB_COM.length(), repoUrl.lastIndexOf("/"));
        String repoOwnerUrlPart = "/" + repoOwner + "/";
        String originUrl = repoUrl.replace(repoOwnerUrlPart, "/" + githubUsername + "/");

        String repoName = repoUrl.substring(
                repoUrl.indexOf(repoOwnerUrlPart) + repoOwnerUrlPart.length(),
                repoUrl.indexOf(".git")
        );

        if ("false".equals(testMode)) {
            forkRepo(repoName, repoOwner);
        }

        wizard.showMessage("Cloning from " + originUrl + " into " + repoName);

        File localPath = new File(repoName, "");
        if (localPath.exists()) {
            throw new IllegalStateException("Destination path \"" + localPath.getAbsolutePath() + "\" already exists.");
        }

        try {
            Git repository = Git.cloneRepository()
                    .setURI(originUrl)
                    .setDirectory(localPath)
                    .call();
            Git git = new Git(gitHelper.getLocalRepository(localPath.getAbsolutePath()));
            gitHelper.addRemoteUpstream(git, localPath.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clone repository", e);
        }
    }
}
