package org.openmrs.maven.plugins.git;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteListCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.openmrs.maven.plugins.AbstractTask;
import org.openmrs.maven.plugins.Pull;
import org.openmrs.maven.plugins.utility.CompositeException;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.maven.plugins.Clone.GITHUB_COM;

public class GitHelper {

    private static final String CREATING_LOCAL_REPO_REASON = "problem when initializing local repository";
    private final static String OPENMRS_USER = "openmrs";
    private static final String CREATING_REMOTE_UPSTREAM_REASON = "problem with creating remote upstream";
    private static final String DELETING_REMOTE_UPSTREAM_REASON = "problem with deleting remote upstream";
    private static final String CREATING_URL_FROM_POM_REASON = "problem with getting URL from pom.xml";
    private static final String NO_GIT_PROJECT_FOUND_REASON = "no git project found";
    private static final String UPSTREAM = "upstream";
    public static final String SCM_GIT_URL = "scm:git:git@github.com:";
    public static final String HTTPS_GIT_URL = "https://github.com/";

    /**
     * @param path to local git repository
     * @return repository object from given path
     */
    public Repository getLocalRepository(String path) {
        try {
            return new FileRepository(new File(path, ".git").getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(CREATING_LOCAL_REPO_REASON, e);
        }
    }

    /**
     * @return if there are any uncommited changes in given git repo
     */
    public boolean checkIfUncommitedChanges(Git git) {
        try {
            List<DiffEntry> diff = git.diff().call();
            return !diff.isEmpty();
        } catch (GitAPIException e) {
            throw new RuntimeException("could not check if there are any uncommited changes", e);
        }
    }

    /**
     * basically calls 'git rebase -i HEAD~{rebaseSize}'
     * @param git
     * @param issueId issueId prefix
     * @param rebaseSize number of last commits to check for issueId in message
     * @throws MojoExecutionException
     */
    public void addIssueIdIfMissing(Git git, final String issueId, int rebaseSize) throws MojoExecutionException {
        CompositeException allExceptions = new CompositeException("Failed to add issue id to commits message if missing");
        RebaseCommand.InteractiveHandler renameHandler = new RebaseCommand.InteractiveHandler() {
            public void prepareSteps(List<RebaseTodoLine> steps) {
                for (RebaseTodoLine step : steps) {
                    try {
                        step.setAction(RebaseTodoLine.Action.REWORD);
                    } catch (IllegalTodoFileModification e) {
                        throw new RuntimeException("Couldn't rename commit " + step.getCommit().name(), e);
                    }
                }
            }

            public String modifyCommitMessage(String commit) {
                if (!commit.startsWith(issueId)) {
                    return issueId + " " + commit;
                } else {
                    return commit;
                }
            }
        };
        try {
            git.rebase().runInteractively(renameHandler).setUpstream("HEAD~" + rebaseSize).call();
        } catch (GitAPIException e) {
            try {
                allExceptions.add("Error during editing commits messages", e);
                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
            } catch (GitAPIException e1) {
                allExceptions.add("Error during aborting rebase", e);
            }
        }
        allExceptions.checkAndThrow();
    }

    /**
     * @param git
     * @param numberOfCommits number of last commits to squash
     */
    public void squashLastCommits(Git git, int numberOfCommits) {
        CompositeException allExceptions = new CompositeException("Failed to squash last commits");
        RebaseCommand.InteractiveHandler squashHandler = new RebaseCommand.InteractiveHandler() {
            public void prepareSteps(List<RebaseTodoLine> steps) {
                for (RebaseTodoLine step : steps.subList(1, steps.size())) {
                    try {
                        step.setAction(RebaseTodoLine.Action.SQUASH);
                        step.setShortMessage("");
                    } catch (IllegalTodoFileModification e) {
                        throw new RuntimeException("Couldn't squash commit " + step.getCommit().name(), e);
                    }
                }
            }

            public String modifyCommitMessage(String commit) {
                return commit;
            }
        };
        try {
            git.rebase().runInteractively(squashHandler).setUpstream("HEAD~" + numberOfCommits).call();
        } catch (GitAPIException e) {
            allExceptions.add("Failed to squash last commits", e);
            try {
                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
            } catch (GitAPIException e1) {
                allExceptions.add("Failed to abort rebase", e1);
            }
        }
    }

    /**
     * basically wrapper for Pull task
     */
    public void pullRebase(AbstractTask parentTask, String branch, Project project) throws MojoExecutionException {
        Pull pull = new Pull(parentTask, branch);
        CompositeException allExceptions = new CompositeException("could not pull upstream automatically - please manually pull latest changes from upstream");
        pull.pullLatestUpstream(allExceptions, project);
        allExceptions.checkAndThrow();
    }

    /**
     * pushes changes callling 'git push'
     * @param git
     * @param username github usename
     * @param password github password
     * @param remote
     *@param force  @return returns JGit resut of push
     */
    public Iterable<PushResult> push(Git git, String username, String password, String reference, String remote, boolean force) {
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        try {
            Iterable<PushResult> results = git
                    .push()
                    .setCredentialsProvider(credentialsProvider)
                    .add(reference)
                    .setRemote(remote)
                    .setForce(force)
                    .call();

            for(PushResult result : results){
                RemoteRefUpdate update = result.getRemoteUpdate(reference);
                RemoteRefUpdate.Status status = update.getStatus();
                if(!(status.equals(RemoteRefUpdate.Status.OK)||status.equals(RemoteRefUpdate.Status.UP_TO_DATE))){
                    throw new RuntimeException("Failed to push changes to origin");
                }
            }
            return results;
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to push to origin", e);
        }
    }

    /**
     * returns commits which differ between given base and head references
     * @param git
     * @param baseRef base reference
     * @param headRef head reference
     * @return
     */
    public Iterable<RevCommit> getCommitDifferential(Git git, String baseRef, String headRef) {
        Ref localRef = git.getRepository().getAllRefs().get(headRef);
        Ref upstreamRef = git.getRepository().getAllRefs().get(baseRef);
        try {
            return git.log().addRange(upstreamRef.getObjectId(), localRef.getObjectId()).call();
        } catch (GitAPIException|MissingObjectException|IncorrectObjectTypeException e) {
            throw new RuntimeException("Failed to get commit differential between"+baseRef+" and "+headRef);
        }
    }

    /**
     * this weird method is neccessary to allow unit testing of PullRequest task,
     * because RevCommit has final methods which cannot be easily mocked
     */
    public List<String> getCommitDifferentialMessages(Git git, String baseRef, String headRef) {
        Iterable<RevCommit> commits = getCommitDifferential(git, baseRef, headRef);
        List<String> messages = new ArrayList<>();
        for(RevCommit commit : commits){
            messages.add(commit.getShortMessage());
        }
        return messages;
    }

    /**
     * opens pull request using parameters from given request
     */
    public PullRequest openPullRequest(GithubPrRequest request) {
        RepositoryService repositoryService = new RepositoryService();
        repositoryService.getClient().setCredentials(request.getUsername(), request.getPassword());
        org.eclipse.egit.github.core.Repository openmrs = null;
        try {
            openmrs = repositoryService.getRepository(OPENMRS_USER, request.getRepository());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get repository data", e);
        }

        PullRequestService pullRequestService = new PullRequestService();
        pullRequestService.getClient().setCredentials(request.getUsername(), request.getPassword());

        PullRequest pr = new org.eclipse.egit.github.core.PullRequest();
        pr.setTitle(request.getTitle());
        pr.setHead(new PullRequestMarker().setLabel(request.getHead()));
        pr.setBase(new PullRequestMarker().setLabel(request.getBase()));
        pr.setBody(request.getDescription());
        try {
            return pullRequestService.createPullRequest(openmrs, pr);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create pull request:"+pr, e);
        }
    }

    /**
     * @param base git reference
     * @param head git reference
     * @param repository name of openmrs repository to check
     * @return
     */
    public PullRequest getPullRequestIfExists(String base, String head, String repository) {
        RepositoryService repositoryService = new RepositoryService();
        org.eclipse.egit.github.core.Repository openmrs = null;
        boolean exists = false;
        try {
            openmrs = repositoryService.getRepository(OPENMRS_USER, repository);
            PullRequestService pullRequestService = new PullRequestService();
            List<PullRequest> openPRs = pullRequestService.getPullRequests(openmrs, "open");
            for(PullRequest openPR : openPRs){
                if(matchPullRequest(base, head, openPR)){
                    return openPR;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to access remote repository data",e);
        }
        return null;
    }

    private boolean matchPullRequest(String base, String head, PullRequest openPR) {
        boolean matchingHead = head.equals(openPR.getHead().getLabel());
        String openmrsBase = base.startsWith(OPENMRS_USER) ? base : OPENMRS_USER+":"+base;
        boolean matchingBase = openmrsBase.equals(openPR.getBase().getLabel());
        return matchingBase && matchingHead;
    }

    /**
     * returns last commit from current branch
     * @param git
     * @return
     */
    public RevCommit getLastCommit(Git git){
        try {
            return git.log().setMaxCount(1).call().iterator().next();
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to access local repository data",e);
        }
    }

    /**
     * calls reset hard on current branch with passed commit as argument
     */
    public void resetHard(Git git, RevCommit commit){
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commit.getName()).call();
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to hard reset to commit "+commit.getShortMessage(), e);
        }
    }

    /**
     * returns tag with passed name in local and remote repository
     * @param git
     * @param tag name of tag
     * @param username github username
     * @param password github password
     * @return
     */
    public boolean deleteTag(Git git, String tag, String username, String password){
        Ref tagRef = git.getRepository().getTags().get(tag);
        if(tagRef != null){
            try {
                //delete remote tag, if
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
                git.push()
                        .setCredentialsProvider(credentialsProvider)
                        .add(":"+tagRef)
                        .setRemote("upstream")
                        .call();
                git.tagDelete().setTags(tagRef.getName()).call();
                return true;
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to delete tag "+tag, e);
            }
        } else {
            return false;
        }
    }

    /**
     * revert passed commits on current branch
     * commits are as iterable, because JGit's LogCommand returns RevWalk which implements iterable
     * @param git
     * @param commits
     */
    public void revertCommits(Git git, Iterable<RevCommit> commits) {
        try {
            RevertCommand revert = git.revert();
            for(RevCommit commit: commits){
                revert.include(commit.getId());
            }
            revert.call();
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to revert commits",e);
        }
    }

    /**
     * Adds 'upstream' reference to remote repositories
     * path has to indicate pom.xml file or directory containing it
     *
     * @param git
     * @param path
     * @throws Exception
     */
    public void addRemoteUpstream(Git git, String path) throws Exception {

        if (!isUpstreamRepoCreated(git, path)) {
            try {
                RemoteAddCommand remoteAddCommand = git.remoteAdd();
                remoteAddCommand.setUri(new URIish(getRemoteRepoUrlFromPom(path)));
                remoteAddCommand.setName(UPSTREAM);
                remoteAddCommand.call();
            } catch (URISyntaxException e) {
                throw new Exception(CREATING_URL_FROM_POM_REASON, e);
            } catch (MojoExecutionException e) {
                throw new MojoExecutionException(NO_GIT_PROJECT_FOUND_REASON, e);
            } catch (GitAPIException e) {
                throw new Exception(CREATING_REMOTE_UPSTREAM_REASON, e);
            }
        }
    }

    public String adjustRemoteOrigin(Git git, String scmUrl, String username) {
        final String originName = "origin";

        String scmOwner = scmUrl.substring(scmUrl.indexOf(GITHUB_COM) + GITHUB_COM.length(), scmUrl.lastIndexOf("/"));
        String scmOwnerUrlPart = "/" + scmOwner + "/";
        String originUrl = scmUrl.replace(scmOwnerUrlPart, "/" + username + "/");

        RemoteConfig origin = getRemote(git, originName);
        if (origin != null) {
            if (!hasUri(origin, originUrl)) {
                removeRemote(git, originName);
                addRemote(git, originName, originUrl);
            }
        } else {
            addRemote(git, originName, originUrl);
        }

        return originUrl;
    }

    private void addRemote(Git git, String name, String uri) {
        RemoteAddCommand remoteAdd = git.remoteAdd();
        remoteAdd.setName(name);
        try {
			remoteAdd.setUri(new URIish(uri));
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
        try {
			remoteAdd.call();
		} catch (GitAPIException e) {
			throw new IllegalStateException(e);
		}
    }

    private void removeRemote(Git git, String name) {
        RemoteRemoveCommand remoteRemove = git.remoteRemove();
        remoteRemove.setName(name);
        try {
			remoteRemove.call();
		} catch (GitAPIException e) {
			throw new IllegalStateException(e);
		}
    }

    private RemoteConfig getRemote(Git git, String name) {
        List<RemoteConfig> remotes;
        try {
            remotes = git.remoteList().call();
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }

        for (RemoteConfig remote: remotes) {
            if (remote.getName().equals(name)) {
                return remote;
            }
        }

        return null;
    }

    private boolean hasUri(RemoteConfig remote, String uri) {
        List<URIish> remoteUris = new ArrayList<>();
        remoteUris.addAll(remote.getURIs());
        remoteUris.addAll(remote.getPushURIs());

        if (remoteUris.isEmpty()) {
            return false;
        }

        for (URIish remoteUri: remoteUris) {
            if (!remoteUri.toString().equals(uri)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Deletes upstream remote repo
     */
    private void deleteUpstreamRepo(Git git) throws Exception {
        RemoteRemoveCommand remoteRemoveCommand = git.remoteRemove();
        remoteRemoveCommand.setName(UPSTREAM);
        try {
            remoteRemoveCommand.call();
        } catch (GitAPIException e) {
            throw new Exception(DELETING_REMOTE_UPSTREAM_REASON, e);
        }

    }

    /**
     * Checks if the upstream repo exist and if there is a proper URL
     */
    private boolean isUpstreamRepoCreated(Git git, String path) throws Exception {
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            for(RemoteConfig remote : remotes){
                if(remote.getName().equals(UPSTREAM)){
                    for(URIish uri : remote.getURIs()){
                        if(uri.toString().equals(getRemoteRepoUrlFromPom(path))){
                            return true;
                        }else {
                            deleteUpstreamRepo(git);
                        }
                    }
                }
            }
        } catch (GitAPIException e) {
            throw new Exception(CREATING_REMOTE_UPSTREAM_REASON, e);
        }

        return false;
    }

    /**
     * get github repository URL from pom.xml
     */
    private String getRemoteRepoUrlFromPom(String path) throws MojoExecutionException {
        Model model = null;
        try {
            File pomFile = new File(path);
            if(pomFile.isDirectory()){
                pomFile = new File(pomFile, "pom.xml");
            }
            model = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to access file "+path,e);
        } catch (XmlPullParserException e) {
            throw new MojoExecutionException("Failed to parse pom.xml",e);
        }
        String url = model.getScm().getUrl();

        if (url.startsWith(SCM_GIT_URL)) {
            url = url.replace(SCM_GIT_URL, HTTPS_GIT_URL);
        } else {
            url = StringUtils.removeEnd(url, "/") + ".git";
        }
        return url;
    }

    public void checkoutToNewBranch(Git git, String branchName) {
        try {
            git.branchCreate().setName(branchName).call();
            git.checkout().setName(branchName).call();
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }
}

