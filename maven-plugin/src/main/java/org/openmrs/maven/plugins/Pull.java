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
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * @goal pull
 * @requiresProject false
 */
public class Pull extends AbstractTask {

    private static final String NO_WATCHED_MODULES_MESSAGE = "Server with id %s has no watched modules";
    private static final String PULL_SUCCESS_MESSAGE = "Module %s have been updated successfully";
    private static final String PULL_ERROR_MESSAGE = "Module %s could not be updated automatically due to %s";
    private static final String CONFLICT_MESSAGE = "conflicts";
    private static final String UPDATE_MANUALLY_MESSAGE = "Changes need to be pulled manually\n";
    private static final String UPDATING_MODULE_MESSAGE = "Module %s is being updated to the latest upstream master";
    private static final String CREATING_LOCAL_REPO_ERROR = "problem when initializing local repository";
    private static final String GIT_COMMAND_ERROR = "problem with resolving git command";
    private static final String CREATING_TEMP_REMOTE_ERROR = "problem with creating temporary remote to perform automatic update";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    private String failureReason;

    public Pull() {}

    public Pull(AbstractTask other) {super(other);}

    public Pull(MavenProject project, MavenSession session, BuildPluginManager manager) {
        super.mavenProject = project;
        super.mavenSession = session;
        super.pluginManager = manager;
    }

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);

        if(server.hasWatchedProjects()){
            boolean pullingError = false;
            Set<Project> watchedProjects = server.getWatchedProjects();
            for(Project project: watchedProjects) {
                wizard.showMessage(String.format(UPDATING_MODULE_MESSAGE, project.getArtifactId()));
                if (pullLatestUpstream(project.getPath())) {
                    wizard.showMessage(String.format(PULL_SUCCESS_MESSAGE, project.getArtifactId()));
                } else {
                    getLog().error(String.format(PULL_ERROR_MESSAGE, project.getArtifactId(), failureReason));
                    getLog().error(UPDATE_MANUALLY_MESSAGE);
                    pullingError = true;
                }
            }
            if(pullingError){
                throw new MojoExecutionException("Some modules could not be updated automatically, please see error messages above");
            }
        }else {
            wizard.showMessage(String.format(NO_WATCHED_MODULES_MESSAGE, serverId));
        }
    }

    /**
     * Pull latest changes from upstream master
     *
     * @param path
     * @return true if pull was successful
     * @throws MojoExecutionException
     */
    private boolean pullLatestUpstream(String path) throws MojoExecutionException {
        Repository localRepo;
        Git git = null;
        String previousBranch;
        String newBranchFull;
        String newBranch = "sdk-" + serverId;
        try {
            localRepo = new FileRepository(new File(path, ".git").getAbsolutePath());
            git = new Git(localRepo);

            previousBranch = localRepo.getBranch();

            checkoutAndCreateNewBranch(git, newBranch);
            newBranchFull = localRepo.getFullBranch();
            RevCommit stash = git.stashCreate().call();
            addRemoteTempRepo(git, path, newBranch);
            PullResult pullResult = pullFromRemote(git, newBranch);
            if(!pullResult.isSuccessful()){
                rebaseAbort(git);
                checkoutBranch(git, previousBranch);
                failureReason = CONFLICT_MESSAGE;
                return false;
            }
            if (stash != null) {
                try {
                    git.stashApply().setStashRef(stash.getName()).call();
                    checkoutBranch(git, previousBranch);
                    mergeWithNewBranch(git, newBranchFull);
                    return true;
                } catch (StashApplyFailureException e) {
                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef(previousBranch).call();
                    checkoutBranch(git, previousBranch);
                    git.stashApply().setStashRef(stash.getName()).call();
                    failureReason = CONFLICT_MESSAGE;
                    return false;
                }
            } else {
                checkoutBranch(git, previousBranch);
                mergeWithNewBranch(git, newBranchFull);
                return true;
            }

        } catch (IOException e) {
            failureReason = CREATING_LOCAL_REPO_ERROR;
        } catch (CheckoutConflictException e) {
            failureReason = CONFLICT_MESSAGE;
        } catch (GitAPIException e) {
            failureReason = GIT_COMMAND_ERROR;
        } catch (URISyntaxException e) {
            failureReason = CREATING_TEMP_REMOTE_ERROR;
        } finally {
            cleanUpBranchesAndRemotes(git, newBranch);
        }

        return false;

    }

    /**
     * Checkout to previous branch
     * @param git
     * @param previousBranch
     * @throws GitAPIException
     */
    private void checkoutBranch(Git git, String previousBranch) throws GitAPIException {
        git.checkout()
                .setCreateBranch(false)
                .setName(previousBranch)
                .call();
    }

    /**
     * Pull from upstream master
     * @param git
     * @param newBranch
     * @return PullResult, which contains information if the pull was successful
     * @throws GitAPIException
     */
    private PullResult pullFromRemote(Git git, String newBranch) throws GitAPIException {
        PullCommand pull = git.pull()
                .setRebase(true)
                .setRemote(newBranch)
                .setRemoteBranchName("master");
        return pull.call();
    }

    /**
     * Abort rebasing if there are any conflicts
     * @param git
     * @throws GitAPIException
     */
    private void rebaseAbort(Git git) throws GitAPIException {
        git.rebase()
                .setOperation(RebaseCommand.Operation.ABORT)
                .call();
    }

    /**
     * Delete branch and remote created to perform pull rebase
     * @param git
     * @param newBranch
     */
    private void cleanUpBranchesAndRemotes(Git git, String newBranch){
        try {
            deleteTempBranch(git, newBranch);
            deleteRemoteTempRepo(git, newBranch);
        } catch (GitAPIException e) {
            throw new RuntimeException("Couldn't delete new branch or remote", e);
        }
    }

    /**
     * Delete branch
     * @param git
     * @throws GitAPIException
     */
    private void deleteTempBranch(Git git, String branchName) throws GitAPIException {
        git.branchDelete()
                .setForce(true)
                .setBranchNames(branchName)
                .call();
    }

    /**
     * Merge with temporary branch
     * @param git
     * @param newBranchFull
     * @throws IOException
     * @throws GitAPIException
     */
    private void mergeWithNewBranch(Git git, String newBranchFull) throws IOException, GitAPIException {
        MergeCommand mergeCommand = git.merge();
        mergeCommand.include(git.getRepository().getRef(newBranchFull));
        mergeCommand.call();
    }

    /**
     * Checkout to new branch
     * @param git
     * @param newBranch
     * @throws GitAPIException
     */
    private void checkoutAndCreateNewBranch(Git git, String newBranch) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.setCreateBranch(true);
        checkoutCommand.setName(newBranch);
        checkoutCommand.call();
    }

    /**
     * Add temporary remote with link from pom.xml
     * @param git
     * @param path
     * @param branchName
     * @return
     * @throws URISyntaxException
     * @throws MojoExecutionException
     * @throws GitAPIException
     */
    private boolean addRemoteTempRepo(Git git,String path, String branchName) throws URISyntaxException, MojoExecutionException, GitAPIException {
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setUri(new URIish(getRemoteRepoUrlFromPom(path)));
        remoteAddCommand.setName(branchName);
        remoteAddCommand.call();
        return true;
    }

    /**
     * Delete temporary remote
     * @param git
     * @param branchName
     * @throws GitAPIException
     */
    private void deleteRemoteTempRepo(Git git, String branchName) throws GitAPIException {
        RemoteRemoveCommand remoteRemoveCommand = git.remoteRemove();
        remoteRemoveCommand.setName(branchName);
        remoteRemoveCommand.call();
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


}
