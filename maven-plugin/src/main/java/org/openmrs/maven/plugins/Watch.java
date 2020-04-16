package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;

/**
*
* @goal watch
* @requiresProject false
*
*/
public class Watch extends AbstractTask {

	/**
     * @parameter  property="serverId"
     */
    private String serverId;

	@Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
	    serverId = wizard.promptForExistingServerIdIfMissing(serverId);

	    File userDir = new File(System.getProperty("user.dir"));
	    if (Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);

            Server serverConfig = loadValidatedServer(serverId);
            serverConfig.addWatchedProject(config);
            serverConfig.save();

            getLog().info("Watching " + config.getPath() + " for changes...");
        } else {
        	throw new MojoFailureException("Command must be run from openmrs-core or module's main directory");
        }
    }

}
