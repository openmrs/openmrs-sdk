package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.CompositeException;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @goal pull
 * @requiresProject false
 */
public class Pull extends AbstractTask {

    private static final String ENTER_BRANCH_NAME_MESSAGE = "Enter name of the upstream branch you want to pull(default: master)";
    private static final String CLEAN_UP_ERROR_MESSAGE = "problem with resolving git command. " +
                                                            "Please run \"git checkout %s\" and \"git stash apply\" in %s directory to revert changes";
    private static final String NO_WATCHED_MODULES_MESSAGE = "Server with id %s has no watched modules";
    private static final String MODULE_UPDATED_SUCCESSFULLY_MESSAGE = "Module %s updated successfully";
    private static final String CONFLICT_ERROR_MESSAGE = "could not be updated automatically due to %s, changes need to be pulled manually";
    private static final String CREATING_NEW_UPSTREAM_MESSAGE = "Creating new upstream repo due to incorrect url";
    private static final String MODULES_UPDATED_SUCCESSFULLY_MESSAGE = "Modules updated successfully:";
    private static final String MODULES_NOT_UPDATED_MESSAGE = "Modules not updated:";
    private static final String CREATING_LOCAL_REPO_REASON = "problem when initializing local repository";
    private static final String GIT_COMMAND_REASON = "problem with resolving git command";
    private static final String CREATING_REMOTE_UPSTREAM_REASON = "problem with creating remote upstream";
    private static final String DELETING_REMOTE_UPSTREAM_REASON = "problem with deleting remote upstream";
    private static final String CREATING_URL_FROM_POM_REASON = "problem with getting URL from pom.xml";
    private static final String NO_GIT_PROJECT_FOUND_REASON = "no git project found";
    private static final String MERGING_PROBLEM_REASON = "problem with merging";
    private static final String DELETING_BRANCH_REASON = "problem wih deleting branch";
    private static final String PULL_COMMAND_PROBLEM_REASON = "problem with executing pull command";
    private static final String CONFLICT_MESSAGE_REASON = "conflicts";
    private static final String UPSTREAM = "upstream";
    private static final String MASTER = "master";
    private static final String DASH = " - ";
    private static final String SDK_DASH = "sdk-";
    private static final String TEMP_BRANCH = "tempBranch";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${branch}
     */
    private String branch;

    private List<String> updatedModules = new ArrayList<>();

    private List<String> notUpdatedModules = new ArrayList<>();

    private String userBranch;

    private RevCommit stash;


    public Pull() {}

    public Pull(AbstractTask other) {super(other);}

    public Pull(MavenProject project, MavenSession session, BuildPluginManager manager) {
        super.mavenProject = project;
        super.mavenSession = session;
        super.pluginManager = manager;
    }

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        if (isGitProjectPresent() && StringUtils.isBlank(serverId)) {
            pullProjectFromUserDir();
        } else {
            pullWatchedProjects();
        }
    }

    /**
     * Pulls latest changes if the project exists in the user directory
     *
     * @throws MojoExecutionException
     */
    private void pullProjectFromUserDir() throws MojoExecutionException {
        String path = System.getProperty("user.dir");
        Project project = Project.loadProject(new File(path));
        if (project.isOpenmrsCore()) {
            branch = wizard.promptForValueIfMissingWithDefault(ENTER_BRANCH_NAME_MESSAGE, branch, null, MASTER);
        } else if(branch == null){
            branch = MASTER;
        }
        if(serverId == null){ serverId = TEMP_BRANCH; }
        try {
            pullLatestUpstream(path);
            wizard.showMessage(String.format(MODULE_UPDATED_SUCCESSFULLY_MESSAGE, project.getArtifactId()));
        } catch (Exception e) {
            String moduleName = project.getArtifactId();
            try {
                cleanUp(path, moduleName);
                wizard.showError(moduleName + " " + String.format(CONFLICT_ERROR_MESSAGE, e.getMessage()));
            } catch (IllegalStateException e1) {
                wizard.showError(moduleName + " " + String.format(CONFLICT_ERROR_MESSAGE, e1.getMessage()));
            }
        }
    }

    /**
     * Pull latest changes for all watched projects
     *
     * @throws MojoExecutionException
     */
    private void pullWatchedProjects() throws MojoExecutionException {
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);

        if(server.hasWatchedProjects()){
            Set<Project> watchedProjects = server.getWatchedProjects();
            CompositeException allExceptions = new CompositeException("Could not updated latest changes");
            for(Project project: watchedProjects) {
                if(project.isOpenmrsCore()){
                    branch = wizard.promptForValueIfMissingWithDefault(ENTER_BRANCH_NAME_MESSAGE, branch, null, MASTER);
                } else{
                    branch = MASTER;
                }
                try {
                    pullLatestUpstream(project.getPath());
                    updatedModules.add(project.getArtifactId());
                } catch (Exception e) {
                    allExceptions.add(project.getPath(), e);
                    try {
                        cleanUp(project.getPath(), project.getArtifactId());
                        notUpdatedModules.add(project.getArtifactId() + DASH + String.format(CONFLICT_ERROR_MESSAGE, e.getMessage()));
                    } catch (IllegalStateException e1) {
                        allExceptions.add(project.getPath(), e1);
                        notUpdatedModules.add(project.getArtifactId() + DASH + String.format(CONFLICT_ERROR_MESSAGE, e.getMessage()) + ". " + e1.getMessage());
                    }
                }
            }
            displayUpdatedModules();
            displayNotUpdatedModules();
            allExceptions.checkAndThrow();
        }else {
            wizard.showMessage(String.format(NO_WATCHED_MODULES_MESSAGE, serverId));
        }
    }

    /**
     * Check if the Git project exists
     *
     * @return
     */
    private boolean isGitProjectPresent(){
        File file = new File(System.getProperty("user.dir"), ".git");
        if(file.exists()){
            return true;
        }
        return false;
    }

    /**
     * Pull latest changes from upstream master
     *
     * @param path
     * @throws Exception
     */
    private void pullLatestUpstream(String path) throws Exception {
        String newBranch = SDK_DASH + serverId;
        Repository localRepo = getLocalRepository(path);
        Git git = new Git(localRepo);
        
        userBranch = localRepo.getBranch();
        if(isTempBranchCreated(git)){
            deleteTempBranch(git, newBranch);
        }        
        checkoutAndCreateNewBranch(git, newBranch);
        String newBranchFull = localRepo.getFullBranch();
        stash = git.stashCreate().call();
        addRemoteUpstream(git, path);
        pullFromRemoteUpstream(git, stash, newBranch, userBranch);
        if (stash != null) {
            try {
                git.stashApply().setStashRef(stash.getName()).call();
                checkoutBranch(git, userBranch);
                mergeWithNewBranch(git, newBranchFull);
                deleteTempBranch(git, newBranch);
            } catch (StashApplyFailureException e) {
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(userBranch).call();
                checkoutBranch(git, userBranch);
                git.stashApply().setStashRef(stash.getName()).call();
                deleteTempBranch(git, newBranch);
                throw new Exception(CONFLICT_MESSAGE_REASON);
            }
        } else {
            checkoutBranch(git, userBranch);
            mergeWithNewBranch(git, newBranchFull);
            deleteTempBranch(git, newBranch);
        }


    }

    /**
     * Clean up if something went wrong
     *
     * @param path
     * @param moduleName
     * @throws IllegalStateException
     */
    private void cleanUp(String path, String moduleName) throws IllegalStateException {
        try {
            Repository localRepo = getLocalRepository(path);
            Git git = new Git(localRepo);
            if(!localRepo.getBranch().equals(userBranch)){
                checkoutBranch(git, userBranch);
                if(stash != null){ git.stashApply().setStashRef(stash.getName()).call(); }
                deleteTempBranch(git, SDK_DASH + serverId);
            } else if(isTempBranchCreated(git)){
                if(stash != null){ git.stashApply().setStashRef(stash.getName()).call(); }
                deleteTempBranch(git, SDK_DASH + serverId);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format(CLEAN_UP_ERROR_MESSAGE, userBranch, moduleName), e);
        }
    }

    /**
     * Checks if the temp branch is created
     *
     * @param git
     * @return
     * @throws GitAPIException
     */
    private boolean isTempBranchCreated(Git git) throws GitAPIException {
        List<Ref> branchList = git.branchList().call();
        for(Ref ref: branchList){
            if(ref.getName().contains(SDK_DASH + serverId)){
                return true;
            }
        }
        return false;
    }

    /**
     * Return local git repository
     * @param path
     * @return
     * @throws IOException
     */
    private Repository getLocalRepository(String path) throws IOException {
        try {
            return new FileRepository(new File(path, ".git").getAbsolutePath());
        } catch (IOException e) {
            throw new IOException(CREATING_LOCAL_REPO_REASON, e);
        }
    }

    /**
     * Checkout to previous branch
     * @param git
     * @param previousBranch
     * @throws GitAPIException
     */
    private void checkoutBranch(Git git, String previousBranch) throws Exception {
        try {
            git.checkout()
                    .setCreateBranch(false)
                    .setName(previousBranch)
                    .call();
        } catch (GitAPIException e) {
            throw new Exception(GIT_COMMAND_REASON, e);
        }
    }

    /**
     * Pull from upstream
     *
     * @param git
     * @param stash
     * @param newBranch
     * @param previousBranch
     * @throws Exception
     */
    private void pullFromRemoteUpstream(Git git, RevCommit stash, String newBranch, String previousBranch) throws Exception {
        try {
            PullCommand pull = git.pull()
                .setRebase(true)
                .setRemote(UPSTREAM)
                .setRemoteBranchName(branch);

            PullResult pullResult = pull.call();
            if(!pullResult.isSuccessful()){
                rebaseAbort(git);
                checkoutBranch(git, previousBranch);
                if (stash != null) { git.stashApply().setStashRef(stash.getName()).call(); }
                deleteTempBranch(git, newBranch);
                throw new Exception(CONFLICT_MESSAGE_REASON);
            }
        } catch (GitAPIException e) {
            throw new Exception(PULL_COMMAND_PROBLEM_REASON, e);
        }
    }

    /**
     * Abort rebasing if there are any conflicts
     * @param git
     * @throws GitAPIException
     */
    private void rebaseAbort(Git git) throws Exception {
        try {
            git.rebase()
                    .setOperation(RebaseCommand.Operation.ABORT)
                    .call();
        } catch (GitAPIException e) {
            throw new Exception(GIT_COMMAND_REASON, e);
        }
    }

    /**
     * Delete branch
     * @param git
     * @throws GitAPIException
     */
    private void deleteTempBranch(Git git, String branchName) throws Exception {
        try {
            git.branchDelete()
                    .setForce(true)
                    .setBranchNames(branchName)
                    .call();
        } catch (GitAPIException e) {
            throw new Exception(DELETING_BRANCH_REASON, e);
        }
    }

    /**
     * Merge with temporary branch
     * @param git
     * @param newBranchFull
     * @throws IOException
     * @throws GitAPIException
     */
    private void mergeWithNewBranch(Git git, String newBranchFull) throws Exception {
        MergeCommand mergeCommand = git.merge();
        try {
            mergeCommand.include(git.getRepository().getRef(newBranchFull));
            mergeCommand.setFastForward(MergeCommand.FastForwardMode.FF_ONLY);
            mergeCommand.call();
        } catch (GitAPIException e) {
            throw new Exception(MERGING_PROBLEM_REASON, e);
        } catch (IOException e) {
            throw new Exception(GIT_COMMAND_REASON, e);
        }
    }

    /**
     * Checkout to new branch
     * @param git
     * @param newBranch
     * @throws GitAPIException
     */
    private void checkoutAndCreateNewBranch(Git git, String newBranch) throws Exception {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.setCreateBranch(true);
        checkoutCommand.setName(newBranch);
        try {
            checkoutCommand.call();
        } catch (GitAPIException e) {
            throw new Exception(GIT_COMMAND_REASON, e);
        }
    }

    /**
     * Add temporary remote with link from pom.xml
     * @param git
     * @param path
     * @return
     * @throws URISyntaxException
     * @throws MojoExecutionException
     * @throws GitAPIException
     */
    private void addRemoteUpstream(Git git, String path) throws Exception {

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

    /**
     * Deletes upstream remote repo
     * @param git
     * @throws Exception
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
     * @param git
     * @param path
     * @return
     * @throws Exception
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
                            wizard.showMessage(CREATING_NEW_UPSTREAM_MESSAGE);
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
     * get github URL from pom.xml
     * @param path
     * @return github URL
     * @throws MojoExecutionException
     */
    private String getRemoteRepoUrlFromPom(String path) throws MojoExecutionException {
        Project project = Project.loadProject(new File(path));
        Model model = project.getModel();
        String url = model.getScm().getUrl();
        return StringUtils.removeEnd(url, "/") + ".git";
    }

    /**
     * displays all updated modules
     */
    private void displayUpdatedModules(){
        if (updatedModules.size() != 0) {
            wizard.showMessage(MODULES_UPDATED_SUCCESSFULLY_MESSAGE);
            for(String moduleName: updatedModules){
                wizard.showMessage(DASH + moduleName);
            }
        }
    }

    /**
     * Displays all modules that could not be updated
     */
    private void displayNotUpdatedModules(){
        if (notUpdatedModules.size() != 0) {
            wizard.showError(MODULES_NOT_UPDATED_MESSAGE);
            for(String moduleName: notUpdatedModules){
                wizard.showMessage(DASH + moduleName);
            }
        }
    }


}
