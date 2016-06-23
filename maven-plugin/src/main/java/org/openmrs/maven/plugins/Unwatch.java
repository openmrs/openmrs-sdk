package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;


/**
*
* @goal unwatch
* @requiresProject false
*
*/
public class Unwatch extends AbstractMojo {
	
	/**
     * @parameter property="serverId"
     */
    private String serverId;
    
    /**
     * @parameter property="artifactId"
     */
    private String artifactId;

    /**
     * @parameter property="groupId"
     */
    private String groupId;

    /**
     * @required
     * @component
     */
    Wizard wizard;

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	    serverId = wizard.promptForExistingServerIdIfMissing(serverId);
	    Server serverConfig = Server.loadServer(serverId);
	    
	    File userDir = new File(System.getProperty("user.dir"));
	    if (StringUtils.isBlank(artifactId) && Project.hasProject(userDir)) {
	    	Project project = Project.loadProject(userDir);
            if (serverConfig.removeWatchedProjectByExample(new Project(project.getGroupId(), project.getArtifactId(), null, null)) != null) {
            	serverConfig.save();
            	getLog().info("Stopped watching " + project.getPath() + " for changes.");
            } else {
            	getLog().info(project.getArtifactId() + " has not been watched.");
            }
        } else {
            artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");

            if (artifactId.equals("*")) {
        		serverConfig.clearWatchedProjects();
        		serverConfig.save();
        		getLog().info("Stopped watching all projects for changes.");
        	} else {
	        	Project project = new Project(groupId, artifactId, null, null);
	        	project = serverConfig.removeWatchedProjectByExample(project);
	        	if (project != null) {
	        		serverConfig.save();
	        		getLog().info("Stopped watching " + project.getPath() + " for changes.");
	        	} else {
	        		getLog().info((groupId != null) ? (groupId + ":") : "" + artifactId + " has not been watched.");
	        	}
        	}
        }
	   
    }
	
}
