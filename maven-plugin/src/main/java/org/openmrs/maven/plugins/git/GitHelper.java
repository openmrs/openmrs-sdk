package org.openmrs.maven.plugins.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.openmrs.maven.plugins.AbstractTask;
import org.openmrs.maven.plugins.utility.Project;

import java.util.Iterator;

public interface GitHelper {
    Repository getLocalRepository(String path);

    boolean checkIfUncommitedChanges(Git git);

    void addIssueIdIfMissing(Git git, String issueId, int rebaseSize) throws MojoExecutionException;

    void squashLastCommits(Git git, int numberOfCommits);

    void pullRebase(AbstractTask task, String branch, Project project) throws MojoExecutionException;

    Iterable<PushResult> push(Git git, String username, String password);

    public Iterator<RevCommit> getCommitDifferential(Git git, Repository repository, String baseRef, String headRef);

    PullRequest openPullRequest(GithubPrRequest request);

    PullRequest getPullRequestIfExists(String ref, String head, String repository);
}
