package org.openmrs.maven.plugins;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.ServerConfig;

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
     * Component for user prompt
     *
     * @component
     */
    private Prompter prompter;

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	    AttributeHelper attributeHelper = new AttributeHelper(prompter);
	    File serverPath = attributeHelper.getServerPath(serverId);
           
        ServerConfig serverConfig = ServerConfig.loadServerConfig(serverPath);
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
