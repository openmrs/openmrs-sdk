package org.openmrs.maven.plugins;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.Wizard;
import org.openmrs.maven.plugins.utility.DefaultWizard;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.ServerConfig;

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
     * Component for user prompt
     *
     * @component
     */
    private Prompter prompter;

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	    Wizard wizard = new DefaultWizard(prompter);
	    File serverPath = wizard.getServerPath(serverId);
	    
	    File userDir = new File(System.getProperty("user.dir"));
	    if (Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);
            
            ServerConfig serverConfig = ServerConfig.loadServerConfig(serverPath);
            serverConfig.addWatchedProject(config);
            serverConfig.save();
            
            getLog().info("Watching " + config.getPath() + " for changes...");
        } else {
        	throw new MojoFailureException("Command must be run from openmrs-core or module's main directory");
        }
    }
	
}
