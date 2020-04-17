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

    public static final String GITHUB_COM = "github.com";
    public static final String GITHUB_HTTP_SUFFIX = "https://github.com/";
    /**
     * @parameter  property="groupId"
     */
    private String groupId;

    /**
     * @parameter  property="artifactId"
     */
    private String artifactId;

    /**
     * @parameter  property="githubUsername"
     */
    private String githubUsername;

    /**
     * @parameter  property="githubPassword"
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

        result = StringUtils.removeEnd(result,"/");

        if (!repoUrl.endsWith(".git")) {
            return result + ".git";
        }
        else {
            return result;
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
        String repoOwner = repoUrl.substring(0, repoUrl.indexOf("/"));
        String originUrl = StringUtils.replaceOnce(repoUrl, repoOwner, githubUsername);

        String repoName = repoUrl.substring(repoOwner.length() + 1, repoUrl.lastIndexOf(".git"));

        if ("false".equals(testMode)) {
            forkRepo(repoName, repoOwner);
        }

        File localPath = new File(repoName);
        wizard.showMessage("Cloning from " + originUrl + " into " + localPath.getAbsolutePath());
        if (localPath.exists()) {
            throw new IllegalStateException("Destination path \"" + localPath.getAbsolutePath() + "\" already exists.");
        }

        try {
            Git.cloneRepository()
                    .setURI(GITHUB_HTTP_SUFFIX + originUrl)
                    .setDirectory(localPath)
                    .call();
            Git git = new Git(gitHelper.getLocalRepository(localPath.getAbsolutePath()));
            gitHelper.addRemoteUpstream(git, localPath.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clone repository", e);
        }
    }
}
