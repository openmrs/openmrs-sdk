package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Project;

import java.io.File;

/**
 * Add a module from the current directory to the list of watched projects.
 * It enables the live-reloading feature for a module.
 */
@Mojo(name = "watch", requiresProject = false)
public class Watch extends AbstractServerTask {

    /**
     * Unique id of a server.
     */
    @Parameter(property = "serverId")
    private String serverId;

	@Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
	    File userDir = new File(System.getProperty("user.dir"));
	    if (Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);

            Server serverConfig = getServer();
            serverConfig.addWatchedProject(config);
            serverConfig.save();

            getLog().info("Watching " + config.getPath() + " for changes...");
        } else {
        	throw new MojoFailureException("Command must be run from openmrs-core or module's main directory");
        }
    }
}
