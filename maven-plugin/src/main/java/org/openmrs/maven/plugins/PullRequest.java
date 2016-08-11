package org.openmrs.maven.plugins;


import com.atlassian.jira.rest.client.domain.Issue;
import com.google.common.collect.Iterables;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.openmrs.maven.plugins.git.GithubPrRequest;
import org.openmrs.maven.plugins.utility.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal pr
 * @requiresProject false
 */
public class PullRequest extends AbstractTask {

    /**
     * Interactive mode flag, set for 'false' allows automatic testing in batch mode,
     * as it makes all 'yes/no' prompts return 'yes'
     *
     * @parameter expression="${branch}" default-value="master"
     */
    String branch;

    /**
     * id of issue against which code is commited
     *
     * @parameter expression="${issueId}"
     */
    String issueId;

    /**
     * github username, required to push and open pull request
     *
     * @parameter expression="${username}"
     */
    String username;

    /**
     * github password, required to push and open pull request
     *
     * @parameter expression="${password}"
     */
    String password;

    /**
     * name of github repository to open PR
     */
    String repoName;

    private static final String COMMIT_NUM_INFO_TMPL = "There are %d commits, which will be included in your pull request. " +
            "It is recommended to squash them into one. Would you like to squash them?";

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        issueId = wizard.promptForValueIfMissing(issueId, "issue id");
        Issue issue = jira.getIssue(issueId);
        if(issue == null){
            throw new MojoExecutionException("invalid issue id");
        }
        String path = mavenProject.getBasedir().getAbsolutePath();
        Repository localRepository = gitHelper.getLocalRepository(path);
        String localBranch = null;
        try {
            localBranch = localRepository.getBranch();
        } catch (IOException e) {
            throw new MojoFailureException("Error during accessing local repository", e);
        }

        Git git = new Git(localRepository);

        if(gitHelper.checkIfUncommitedChanges(git)){
            wizard.showMessage("There are uncommitted changes. Please commit before proceeding.");
            throw new MojoExecutionException("There are uncommitted changes. Please commit before proceeding.");
        }

        if (localBranch.equals("master")) {
            boolean yes = wizard.promptYesNo("Creating pull request from the master branch is not recommended. Would you like to create a feature branch '" + issueId + "'?");
            if (yes) {
                gitHelper.checkoutToNewBranch(git, issueId);
                localBranch = issueId;
            }
        }

        gitHelper.pullRebase(this, branch, new Project(mavenProject.getModel()));

        String localRef = "refs/heads/"+localBranch;
        String upstreamRef = "refs/remotes/upstream/"+branch;

        squashCommitsIfAccepted(git, localRef, upstreamRef);

        boolean correctMessages = fixCommitMessagesIfAccepted(git, localRef, upstreamRef);
        if(!correctMessages){
            return;
        }

        String scmUrl = mavenProject.getScm().getUrl();
        repoName = scmUrl.substring(scmUrl.lastIndexOf("/")+1);
        username = wizard.promptForValueIfMissing(username, "your GitHub username");

        String originUrl = gitHelper.adjustRemoteOrigin(git, scmUrl, username);
        wizard.showMessage("Changes will be pushed to " + localBranch + " branch at " + originUrl);

        password = wizard.promptForPasswordIfMissing(password, "your GitHub password");

        gitHelper.push(git, username, password, "refs/heads/"+localBranch, "origin", true);
        createUpdatePullRequest(issue, localBranch, repoName);
    }

    /**
     *
     * Checks if there is more than one commit to potential PR, and prompts if user wants them automatically squashed
     * if so, squashes them
     *
     * @param git JGit object handling localRepository
     * @param localRef reference to local branch
     * @param upstreamRef reference to upstream branch
     */
    private void squashCommitsIfAccepted(Git git, String localRef, String upstreamRef) {
        Iterable<RevCommit> commits = gitHelper.getCommitDifferential(git, upstreamRef, localRef);

        int size = Iterables.size(commits);
        if(size > 1){
            boolean hasToSquash = wizard.promptYesNo(String.format(COMMIT_NUM_INFO_TMPL, size));
            if(hasToSquash){
                gitHelper.squashLastCommits(git, size);
            }
        }
    }

    /**
     *
     * Checks if commits in local branch, which are ahead of upstream branch have commit messages starting with issueId,
     * and fixes them if user accepts
     *
     * @param git JGit object handling localRepository
     * @param localRef reference to local branch
     * @param upstreamRef reference to upstream branch
     * @return if commits in potential PR have messages starting with issueId
     * @throws MojoExecutionException
     */
    private boolean fixCommitMessagesIfAccepted(Git git, String localRef, String upstreamRef) throws MojoExecutionException {
        List<String> commitMessages = gitHelper.getCommitDifferentialMessages(git, upstreamRef, localRef);
        List<String> messagesToModify = new ArrayList<>();

        int size = Iterables.size(commitMessages);
        for(String commit : commitMessages){
            if(!commit.startsWith(issueId)){
                messagesToModify.add(commit);
            }
        }

        if(messagesToModify.size() > 0) {
            String messageText = createRenamePrompt(issueId, messagesToModify);
            wizard.showMessage(messageText);
            boolean correctMessages = wizard.promptYesNo("Would you like them to be corrected automatically?");
            if (correctMessages) {
                gitHelper.addIssueIdIfMissing(git, issueId, size);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     *
     * opens PR on github, refering to passed jira issue, from given origin branch to selected repo in OpenMRS user account
     *
     * @param issue jira issue which PR refers to
     * @param originBranch name of branch to be head of PR
     * @param repoName name of openmrs repository to be base of PR
     */
    private void createUpdatePullRequest(Issue issue, String originBranch, String repoName) {
        org.eclipse.egit.github.core.PullRequest pr = gitHelper.getPullRequestIfExists(branch, username +":"+originBranch, repoName);
        if(pr == null){
            wizard.showMessage("Creating new pull request...");
            String description = wizard.promptForValueIfMissingWithDefault("You can include a short %s (optional)", null, "description", "");
            description = "https://issues.openmrs.org/browse/"+ issueId +"\n\n"+description;
            GithubPrRequest request = new GithubPrRequest.Builder()
                    .setBase(branch)
                    .setHead(username +":"+originBranch)
                    .setUsername(username)
                    .setPassword(password)
                    .setDescription(description)
                    .setTitle(issue.getKey()+" "+issue.getSummary())
                    .setRepository(repoName)
                    .build();
            pr = gitHelper.openPullRequest(request);
            wizard.showMessage("Pull request created at "+pr.getHtmlUrl());
        } else {
            wizard.showMessage("Pull request updated at " + pr.getHtmlUrl());
        }
    }


    /**
     * creates multiline message showing commit messages which need to be fixed,
     * in format {message} -> {issueId}{message}
     *
     * @param issueId
     * @param messagesToModify
     * @return
     */
    private String createRenamePrompt(String issueId, List<String> messagesToModify) {
        StringBuilder message = new StringBuilder("Some of your commits do not start from issue id. they should be corrected as following:\n");
        for(String messageToModify : messagesToModify) {
            message.append(messageToModify);
            message.append(" -> ");
            message.append(issueId);
            message.append(" ");
            message.append(messageToModify);
            message.append("\n");
        }
        return message.toString();
    }
}
