package org.openmrs.maven.plugins.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
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
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.openmrs.maven.plugins.AbstractTask;
import org.openmrs.maven.plugins.Pull;
import org.openmrs.maven.plugins.utility.CompositeException;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultGitHelper implements GitHelper {

    private static final String CREATING_LOCAL_REPO_REASON = "problem when initializing local repository";
    private final static String OPENMRS_USER = "openmrs";

    /**
     * Return local git repository
     *
     * @param path
     * @return
     * @throws IOException
     */
    @Override
    public Repository getLocalRepository(String path) {
        try {
            return new FileRepository(new File(path, ".git").getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(CREATING_LOCAL_REPO_REASON, e);
        }
    }

    @Override
    public boolean checkIfUncommitedChanges(Git git) {
        try {
            List<DiffEntry> diff = git.diff().call();
            return !diff.isEmpty();
        } catch (GitAPIException e) {
            throw new RuntimeException("could not check if there are any uncommited changes", e);
        }
    }

    @Override
    public void addIssueIdIfMissing(Git git, final String issueId, int rebaseSize) throws MojoExecutionException {
        CompositeException allExceptions = new CompositeException("Failed to add issue id to commits message if missing");
        RebaseCommand.InteractiveHandler renameHandler = new RebaseCommand.InteractiveHandler() {
            @Override
            public void prepareSteps(List<RebaseTodoLine> steps) {
                for (RebaseTodoLine step : steps) {
                    try {
                        step.setAction(RebaseTodoLine.Action.REWORD);
                    } catch (IllegalTodoFileModification e) {
                        throw new RuntimeException("Couldn't rename commit " + step.getCommit().name(), e);
                    }
                }
            }

            @Override
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

    @Override
    public void squashLastCommits(Git git, int numberOfCommits) {
        CompositeException allExceptions = new CompositeException("Failed to squash last commits");
        RebaseCommand.InteractiveHandler squashHandler = new RebaseCommand.InteractiveHandler() {
            @Override
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

            @Override
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

    @Override
    public void pullRebase(AbstractTask parentTask, String branch, Project project) throws MojoExecutionException {
        Pull pull = new Pull(parentTask, branch);
        CompositeException allExceptions = new CompositeException("could not pull upstream automatically - please manually pull latest changes from upstream");
        pull.pullLatestUpstream(allExceptions, project);
        allExceptions.checkAndThrow();
    }

    @Override
    public Iterable<PushResult> push (Git git, String username, String password, String branch) {
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        try {
            Iterable<PushResult> results = git
                    .push()
                    .setCredentialsProvider(credentialsProvider)
                    .add("refs/heads/"+branch)
                    .setRemote("origin")
                    .call();

            for(PushResult result : results){
                RemoteRefUpdate update = result.getRemoteUpdate("refs/heads/"+branch);
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


    @Override
    public Iterable<RevCommit> getCommitDifferential(Git git, String baseRef, String headRef) {
        Ref localRef = git.getRepository().getAllRefs().get(headRef);
        Ref upstreamRef = git.getRepository().getAllRefs().get(baseRef);
        try {
            return git.log().addRange(upstreamRef.getObjectId(), localRef.getObjectId()).call();
        } catch (GitAPIException|MissingObjectException|IncorrectObjectTypeException e) {
            throw new RuntimeException("Failed to get commit differential between"+baseRef+" and "+headRef);
        }
    }


    @Override
    public List<String> getCommitDifferentialMessages(Git git, String baseRef, String headRef) {
        Iterable<RevCommit> commits = getCommitDifferential(git, baseRef, headRef);
        List<String> messages = new ArrayList<>();
        for(RevCommit commit : commits){
            messages.add(commit.getShortMessage());
        }
        return messages;
    }

    @Override
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

    @Override
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
}

