package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.util.Set;

/**
 *  @goal pull
 *
 */
public class Pull extends AbstractTask {

    private static final String NO_WATCHED_MODULES_MESSAGE = "Server with id %s has no watched modules";
    private static final String PULL_SUCCESS_MESSAGE = "Module %s have been updated successfully";
    private static final String PULL_ERROR_MESSAGE = "Module %s could not be updated";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

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
            Set<Project> watchedProjects = server.getWatchedProjects();
            for(Project project: watchedProjects) {
                File module = new File(project.getPath());
                if (pullLatestUpstream(module)) {
                    wizard.showMessage(String.format(PULL_SUCCESS_MESSAGE, project.getArtifactId()));
                } else {
                    wizard.showMessage(String.format(PULL_ERROR_MESSAGE, project.getArtifactId()));
                }
            }
        }else {
            wizard.showMessage(String.format(NO_WATCHED_MODULES_MESSAGE, serverId));
        }
    }

    private boolean pullLatestUpstream(File module){
        return false;
    }


}
