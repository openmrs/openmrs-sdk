package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;

/**
*
* @goal watch
* @requiresProject false
*
*/
public class Watch extends AbstractMojo {
	
	/**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @required
     * @component
     */
    Wizard wizard;

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	    serverId = wizard.promptForExistingServerIdIfMissing(serverId);
	    
	    File userDir = new File(System.getProperty("user.dir"));
	    if (Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);
            
            Server serverConfig = Server.loadServer(serverId);
            serverConfig.addWatchedProject(config);
            serverConfig.save();
            
            getLog().info("Watching " + config.getPath() + " for changes...");
        } else {
        	throw new MojoFailureException("Command must be run from openmrs-core or module's main directory");
        }
    }
	
}
