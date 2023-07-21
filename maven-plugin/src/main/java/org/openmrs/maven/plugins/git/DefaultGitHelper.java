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
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
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
import org.openmrs.maven.plugins.model.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openmrs.maven.plugins.Clone.GITHUB_COM;

public class DefaultGitHelper implements GitHelper {

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
	 * @inheritDoc
	 */
	@Override
	public Repository getLocalRepository(String path) throws MojoExecutionException {
		try {
			return new FileRepository(new File(path, ".git").getAbsolutePath());
		}
		catch (IOException e) {
			throw new MojoExecutionException(CREATING_LOCAL_REPO_REASON, e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean checkIfUncommittedChanges(Git git) throws MojoExecutionException {
		try {
			List<DiffEntry> diff = git.diff().call();
			return !diff.isEmpty();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("could not check if there are any uncommited changes", e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void addIssueIdIfMissing(Git git, final String issueId, int rebaseSize) throws MojoExecutionException {
		CompositeException allExceptions = new CompositeException("Failed to add issue id to commits message if missing");
		RebaseCommand.InteractiveHandler renameHandler = new RebaseCommand.InteractiveHandler() {

			@Override
			public void prepareSteps(List<RebaseTodoLine> steps) {
				for (RebaseTodoLine step : steps) {
					try {
						step.setAction(RebaseTodoLine.Action.REWORD);
					}
					catch (IllegalTodoFileModification e) {
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
		}
		catch (GitAPIException e) {
			try {
				allExceptions.add("Error during editing commits messages", e);
				git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
			}
			catch (GitAPIException e1) {
				allExceptions.add("Error during aborting rebase", e);
			}
		}
		allExceptions.checkAndThrow();
	}

	/**
	 * @inheritDoc
	 */
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
					}
					catch (IllegalTodoFileModification e) {
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
		}
		catch (GitAPIException e) {
			allExceptions.add("Failed to squash last commits", e);
			try {
				git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
			}
			catch (GitAPIException e1) {
				allExceptions.add("Failed to abort rebase", e1);
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void pullRebase(AbstractTask parentTask, String branch, Project project) throws MojoExecutionException {
		Pull pull = new Pull(parentTask, branch);
		CompositeException allExceptions = new CompositeException(
				"could not pull upstream automatically - please manually pull latest changes from upstream");
		pull.pullLatestUpstream(allExceptions, project);
		allExceptions.checkAndThrow();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Iterable<PushResult> push(Git git, String username, String password, String reference, String remote,
			boolean force) throws MojoExecutionException {
		UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username,
				password);
		try {
			Iterable<PushResult> results = git
					.push()
					.setCredentialsProvider(credentialsProvider)
					.add(reference)
					.setRemote(remote)
					.setForce(force)
					.call();

			for (PushResult result : results) {
				RemoteRefUpdate update = result.getRemoteUpdate(reference);
				RemoteRefUpdate.Status status = update.getStatus();
				if (!(status.equals(RemoteRefUpdate.Status.OK) || status.equals(RemoteRefUpdate.Status.UP_TO_DATE))) {
					throw new MojoExecutionException("Failed to push changes to origin");
				}
			}
			return results;
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to push to origin", e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Iterable<RevCommit> getCommitDifferential(Git git, String baseRef, String headRef) throws MojoExecutionException {
		Ref localRef = git.getRepository().getAllRefs().get(headRef);
		Ref upstreamRef = git.getRepository().getAllRefs().get(baseRef);
		try {
			return git.log().addRange(upstreamRef.getObjectId(), localRef.getObjectId()).call();
		}
		catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
			throw new MojoExecutionException("Failed to get commit differential between" + baseRef + " and " + headRef + " " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<String> getCommitDifferentialMessages(Git git, String baseRef, String headRef)
			throws MojoExecutionException {
		Iterable<RevCommit> commits = getCommitDifferential(git, baseRef, headRef);
		List<String> messages = new ArrayList<>();
		for (RevCommit commit : commits) {
			messages.add(commit.getShortMessage());
		}
		return messages;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public PullRequest openPullRequest(GithubPrRequest request) throws MojoExecutionException {
		RepositoryService repositoryService = new RepositoryService();
		repositoryService.getClient().setCredentials(request.getUsername(), request.getPassword());
		org.eclipse.egit.github.core.Repository openmrs;
		try {
			openmrs = repositoryService.getRepository(OPENMRS_USER, request.getRepository());
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to get repository data " + e.getMessage(), e);
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
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to create pull request \"" + pr + "\" " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public PullRequest getPullRequestIfExists(String base, String head, String repository) throws MojoExecutionException {
		RepositoryService repositoryService = new RepositoryService();
		org.eclipse.egit.github.core.Repository openmrs;
		try {
			openmrs = repositoryService.getRepository(OPENMRS_USER, repository);
			PullRequestService pullRequestService = new PullRequestService();
			List<PullRequest> openPRs = pullRequestService.getPullRequests(openmrs, "open");
			for (PullRequest openPR : openPRs) {
				if (matchPullRequest(base, head, openPR)) {
					return openPR;
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to access remote repository data " + e.getMessage(), e);
		}
		return null;
	}

	private boolean matchPullRequest(String base, String head, PullRequest openPR) {
		boolean matchingHead = head.equals(openPR.getHead().getLabel());
		String openmrsBase = base.startsWith(OPENMRS_USER) ? base : OPENMRS_USER + ":" + base;
		boolean matchingBase = openmrsBase.equals(openPR.getBase().getLabel());
		return matchingBase && matchingHead;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public RevCommit getLastCommit(Git git) throws MojoExecutionException {
		try {
			return git.log().setMaxCount(1).call().iterator().next();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to access local repository data " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void resetHard(Git git, RevCommit commit) throws MojoExecutionException {
		try {
			git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commit.getName()).call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to hard reset to commit " + commit.getShortMessage() + " " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean deleteTag(Git git, String tag, String username, String password) throws MojoExecutionException {
		Optional<Ref> tagRef = Optional.ofNullable(git.getRepository().getTags().get(tag));
		if(!tagRef.isPresent()) {
			return false;
		}
		try {
			//delete remote tag, if
			UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username,
					password);
			git.push()
					.setCredentialsProvider(credentialsProvider)
					.add(":" + tagRef.get())
					.setRemote("upstream")
					.call();
			git.tagDelete().setTags(tagRef.get().getName()).call();
			return true;
		} catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to delete tag \"" + tag + "\" " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void revertCommits(Git git, Iterable<RevCommit> commits) throws MojoExecutionException {
		try {
			RevertCommand revert = git.revert();
			for (RevCommit commit : commits) {
				revert.include(commit.getId());
			}
			revert.call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to revert commits " + e.getMessage(), e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void addRemoteUpstream(Git git, String path) throws Exception {

		if (!isUpstreamRepoCreated(git, path)) {
			try {
				RemoteAddCommand remoteAddCommand = git.remoteAdd();
				remoteAddCommand.setUri(new URIish(getRemoteRepoUrlFromPom(path)));
				remoteAddCommand.setName(UPSTREAM);
				remoteAddCommand.call();
			}
			catch (URISyntaxException e) {
				throw new Exception(CREATING_URL_FROM_POM_REASON, e);
			}
			catch (MojoExecutionException e) {
				throw new MojoExecutionException(NO_GIT_PROJECT_FOUND_REASON, e);
			}
			catch (GitAPIException e) {
				throw new Exception(CREATING_REMOTE_UPSTREAM_REASON, e);
			}
		}
	}

	@Override
	public String adjustRemoteOrigin(Git git, String scmUrl, String username) throws MojoExecutionException {
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

	private void addRemote(Git git, String name, String uri) throws MojoExecutionException {
		RemoteAddCommand remoteAdd = git.remoteAdd();
		remoteAdd.setName(name);
		try {
			remoteAdd.setUri(new URIish(uri));
		}
		catch (URISyntaxException e) {
			throw new MojoExecutionException("URI '" + uri + "' is not a valid URI", e);
		}
		try {
			remoteAdd.call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("An exception occurred while creating remote '" + name + "' " + e.getMessage(),
					e);
		}
	}

	private void removeRemote(Git git, String name) throws MojoExecutionException {
		RemoteRemoveCommand remoteRemove = git.remoteRemove();
		remoteRemove.setName(name);
		try {
			remoteRemove.call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("An exception occurred while removing remote '" + name + "' " + e.getMessage(),
					e);
		}
	}

	private RemoteConfig getRemote(Git git, String name) throws MojoExecutionException {
		List<RemoteConfig> remotes;
		try {
			remotes = git.remoteList().call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException("Exception occurred while trying to list remotes " + e.getMessage(), e);
		}

		for (RemoteConfig remote : remotes) {
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

		for (URIish remoteUri : remoteUris) {
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
		}
		catch (GitAPIException e) {
			throw new Exception(DELETING_REMOTE_UPSTREAM_REASON, e);
		}

	}

	/**
	 * Checks if the upstream repo exist and if there is a proper URL
	 */
	private boolean isUpstreamRepoCreated(Git git, String path) throws Exception {
		try {
			List<RemoteConfig> remotes = git.remoteList().call();
			for (RemoteConfig remote : remotes) {
				if (remote.getName().equals(UPSTREAM)) {
					for (URIish uri : remote.getURIs()) {
						if (uri.toString().equals(getRemoteRepoUrlFromPom(path))) {
							return true;
						} else {
							deleteUpstreamRepo(git);
						}
					}
				}
			}
		}
		catch (GitAPIException e) {
			throw new Exception(CREATING_REMOTE_UPSTREAM_REASON, e);
		}

		return false;
	}

	/**
	 * get github repository URL from pom.xml
	 */
	private String getRemoteRepoUrlFromPom(String path) throws MojoExecutionException {
		Model model;
		try {
			File pomFile = new File(path);
			if (pomFile.isDirectory()) {
				pomFile = new File(pomFile, "pom.xml");
			}
			model = new MavenXpp3Reader().read(new FileInputStream(pomFile));
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to access file " + path, e);
		}
		catch (XmlPullParserException e) {
			throw new MojoExecutionException("Failed to parse pom.xml", e);
		}
		String url = model.getScm().getUrl();

		if (url.startsWith(SCM_GIT_URL)) {
			url = url.replace(SCM_GIT_URL, HTTPS_GIT_URL);
		} else {
			url = StringUtils.removeEnd(url, "/") + ".git";
		}
		return url;
	}

	@Override
	public void checkoutToNewBranch(Git git, String branchName) throws MojoExecutionException {
		try {
			git.branchCreate().setName(branchName).call();
			git.checkout().setName(branchName).call();
		}
		catch (GitAPIException e) {
			throw new MojoExecutionException(
					"An exception occurred trying to checkout branch '" + branchName + "' " + e.getMessage(), e);
		}
	}
}

