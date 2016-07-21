package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
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
 *
 */

public class Clone extends AbstractTask {

    /**
     * @parameter expression="${groupId}" default-value="org.openmrs.module"
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

    private static final String GOAL_COPY = "copy";
    private static final String OPENMRS_GITHUB_KEY = "openmrs";

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        if (artifactId == null) {
            artifactId = wizard.promptForValueIfMissing(null, "artifactId");
        }

        String version = versionsHelper.getLatestReleasedVersion(new Artifact(artifactId, "0", groupId));
        String gitHubHttpKey;

        try {
            gitHubHttpKey = extractGitHubHttpKeyFromModulePom(artifactId, version, groupId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch github data from module repository", e);
        }

        if (githubUsername == null) {
            githubUsername = wizard.promptForValueIfMissing(null, "GitHub username");
            // Remove eventually mistakes like spaces, tabs etc.
            githubUsername = githubUsername.replaceAll("\\s+", "");
        }

        if (githubPassword == null) {
            githubPassword = wizard.promptForValueIfMissing(null, "GitHub password");
        }

        cloneRepo(gitHubHttpKey, OPENMRS_GITHUB_KEY);

        wizard.showMessage("");
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
        return StringUtils.removeEnd(url, "/") + ".git";
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
                goal(GOAL_COPY),
                configuration(configuration.toArray(new Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private void forkRepo(String repoToForkName) {

        wizard.showMessage("Forking repository:");
        String repoToForkOwner = "openmrs";
        GitHubClient client = new GitHubClient();
        System.out.print("- Client authentication");
        client.setCredentials(githubUsername, githubPassword);
        wizard.showMessage(" - DONE");
        RepositoryService service = new RepositoryService();

        System.out.print("- Service authentication");
        service.getClient().setCredentials(githubUsername, githubPassword);
        wizard.showMessage(" - DONE");
        RepositoryId toBeForked = new RepositoryId(repoToForkOwner, repoToForkName);
        try {
            System.out.print("- Forking");
            service.forkRepository(toBeForked);
            wizard.showMessage(" - DONE");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fork repository \"" + repoToForkOwner + "/" + repoToForkName+ "\"", e);
        }
    }

    private void cloneRepo(String upstreamHttpKey, String repoOwner) {

        repoOwner = "/" + repoOwner + "/";

        String originHttpKey = upstreamHttpKey.replace(repoOwner, "/" + githubUsername + "/");

        String repoName = upstreamHttpKey.substring(
                upstreamHttpKey.indexOf(repoOwner) + repoOwner.length(),
                upstreamHttpKey.indexOf(".git")
        );

        if ("false".equals(testMode)) {
            forkRepo(repoName);
        }

        wizard.showMessage("Cloning repository:");

        System.out.print("- Creating directory " + repoName);
        File localPath = new File(repoName, "");
        if (localPath.exists()) {
            throw new IllegalStateException("Destination path \"" + repoName + "\" allready exists." + localPath.getAbsolutePath());
        }
        wizard.showMessage(" - DONE");

        System.out.print("- Cloning from " + originHttpKey + " to " + localPath);
        try {
            try (Git repository = Git.cloneRepository()
                    .setURI(originHttpKey)
                    .setDirectory(localPath)
                    .call()) {
                wizard.showMessage(" - DONE");
                addUpstream(repository, upstreamHttpKey);
            }
        } catch (GitAPIException e) {
            throw new IllegalStateException("Failed to clone repository from " + originHttpKey + ".", e);
        }
    }

    private void addUpstream(Git repository, String upstreamHttpKey) {
        System.out.print("- Adding \"upstream\" remote repository");
        StoredConfig config = repository.getRepository().getConfig();
        config.setString("remote", "upstream", "url", upstreamHttpKey);
        try {
            config.save();
            wizard.showMessage(" - DONE");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to add \"upstream\" remote repository (" + upstreamHttpKey + ")", e);
        }
    }
}
