package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.util.Set;

/**
*
* @goal info
* @requiresProject false
*
*/
public class Info extends AbstractMojo {
	
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
           
        Server serverConfig = Server.loadServer(serverId);
        Set<Project> watchedProjects = serverConfig.getWatchedProjects();
        
        getLog().info(" ");
        if (watchedProjects.isEmpty()) {
        	getLog().info("No projects watched for changes.");
        } else {
        	getLog().info("Projects watched for changes:");
	        int i = 1;
	        for (Project watchedProject : watchedProjects) {
	            getLog().info(String.format("%d) %s:%s at %s", i, watchedProject.getGroupId(), watchedProject.getArtifactId(), watchedProject.getPath()));
	            i++;
	        }
        }
        getLog().info(" ");
    }
	
}
