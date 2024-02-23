package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Project;

import java.util.Set;

/**
 * Display server details including the list of watched modules.
 */
@Mojo(name = "info", requiresProject = false)
public class Info extends AbstractServerTask {

	@Override
	public void executeTask() throws MojoExecutionException {
		Server serverConfig = getServer();
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
