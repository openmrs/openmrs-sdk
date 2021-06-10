package org.openmrs.maven.plugins.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.openmrs.maven.plugins.AbstractTask;
import org.openmrs.maven.plugins.utility.Project;

import java.util.List;

public interface GitHelper {
    /**
     * @param path to local git repository
     * @return repository object from given path
     */
    Repository getLocalRepository(String path);

    /**
     * @return if there are any uncommited changes in given git repo
     */
    boolean checkIfUncommitedChanges(Git git);

    /**
     * basically calls 'git rebase -i HEAD~{rebaseSize}'
     * @param git
     * @param issueId issueId prefix
     * @param rebaseSize number of last commits to check for issueId in message
     * @throws MojoExecutionException
     */
    void addIssueIdIfMissing(Git git, String issueId, int rebaseSize) throws MojoExecutionException;

    /**
     * @param git
     * @param numberOfCommits number of last commits to squash
     */
    void squashLastCommits(Git git, int numberOfCommits);

    /**
     * basically wrapper for Pull task
     */
    void pullRebase(AbstractTask parentTask, String branch, Project project) throws MojoExecutionException;

    /**
     * pushes changes callling 'git push'
     * @param git
     * @param username github usename
     * @param personalAccessToken github personal access token
     * @param remote
     *@param force  @return returns JGit resut of push
     */
    Iterable<PushResult> push(Git git, String username, String personalAccessToken, String ref, String remote, boolean force);

    /**
     * returns commits which differ between given base and head references
     * @param git
     * @param baseRef base reference
     * @param headRef head reference
     * @return
     */
    Iterable<RevCommit> getCommitDifferential(Git git, String baseRef, String headRef);

    /**
     * this weird method is neccessary to allow unit testing of PullRequest task,
     * because RevCommit has final methods which cannot be easily mocked
     */
    List<String> getCommitDifferentialMessages(Git git, String baseRef, String headRef);

    /**
     * opens pull request using parameters from given request
     */
    PullRequest openPullRequest(GithubPrRequest request);

    /**
     * @param base git reference
     * @param head git reference
     * @param repository name of openmrs repository to check
     * @return
     */
    PullRequest getPullRequestIfExists(String base, String head, String repository);

    /**
     * returns last commit from current branch
     * @param git
     * @return
     */
    RevCommit getLastCommit(Git git);

    /**
     * calls reset hard on current branch with passed commit as argument
     */
    void resetHard(Git git, RevCommit commit);

    /**
     * returns tag with passed name in local and remote repository
     * @param git
     * @param tagRef name of tag
     * @param username github username
     * @param personalAccessToken github personal access token
     * @return
     */
    boolean deleteTag(Git git, String tagRef, String username, String personalAccessToken);

    /**
     * revert passed commits on current branch
     * commits are as iterable, because JGit's LogCommand returns RevWalk which implements iterable
     * @param git
     * @param commits
     */
    void revertCommits(Git git, Iterable<RevCommit> commits);

    /**
     * Adds 'upstream' reference to remote repositories
     * path has to indicate pom.xml file or directory containing it
     *
     * @param git
     * @param path
     * @throws Exception
     */
    void addRemoteUpstream(Git git, String path) throws Exception;

    String adjustRemoteOrigin(Git git, String scmUrl, String username);

    void checkoutToNewBranch(Git git, String branchName);
}
