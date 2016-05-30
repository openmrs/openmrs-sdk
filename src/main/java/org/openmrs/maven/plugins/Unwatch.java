package org.openmrs.maven.plugins;

import java.io.File;

import org.apache.commons.lang.StringUtils;
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
* @goal unwatch
* @requiresProject false
*
*/
public class Unwatch extends AbstractMojo {
	
	/**
     * @parameter expression="${serverId}"
     */
    private String serverId;
    
    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${groupId}"
     */
    private String groupId;
    
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
	    ServerConfig serverConfig = ServerConfig.loadServerConfig(serverPath);
	    
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
