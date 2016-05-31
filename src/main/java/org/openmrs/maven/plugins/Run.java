package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;
import org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal run
 * @requiresProject false
 */
public class Run extends AbstractMojo {

    private static final String NO_SERVER_TEXT = "There no server with given serverId. Please create it using omrs:setup first";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     */
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    /**
     * @required
     * @component
     */
    Wizard wizard;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = wizard.getServerPath(serverId, NO_SERVER_TEXT);
        serverPath.mkdirs();
        File userDir = new File(System.getProperty("user.dir"));
        if (Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);
            if (config.isOpenmrsModule()) {
                String artifactId = config.getArtifactId();
                String groupId = config.getGroupId();
                String version = config.getVersion();
                if ((artifactId != null) && (groupId != null) && version != null) {
                    getLog().info("OpenMRS module detected, installing before run...");
                    ModuleInstall installer = new ModuleInstall(mavenProject, mavenSession, pluginManager);
                    installer.installModule(serverPath.getName(), groupId, artifactId, version);
                }
            }
        }
        File tempDirectory = new File(serverPath, "tmp");
        tempDirectory.mkdirs();
        String warFile = null;
        String h2File = null;
        for(File file: serverPath.listFiles()) {
            if (file.getName().startsWith("openmrs-") && (file.getName().endsWith(".war"))) {
                warFile = file.getName();
            }
            else if (file.getName().startsWith("h2-") && (file.getName().endsWith(".jar"))) {
                h2File = file.getName();
            }
            if ((warFile != null) && (h2File != null)) break;
        };
        // prepare web app configuration
        List<Element> webAppConfiguration = new ArrayList<Element>();
        webAppConfiguration.add(element("contextPath", "/openmrs"));
        webAppConfiguration.add(element("tempDirectory", tempDirectory.getAbsolutePath()));
        if (warFile == null) {
            throw new MojoExecutionException("Error during running server: war file was not found");
        }
        // add h2 file to webapp config if it exists
        if (h2File != null) {
            webAppConfiguration.add(element("extraClasspath", new File(serverPath, h2File).getAbsolutePath()));
        }
        
        List<Element> systemProperties = new ArrayList<Element>();
        systemProperties.add(element("systemProperty",
            element("name", "OPENMRS_INSTALLATION_SCRIPT"),
            element("value", new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath())));
        systemProperties.add(element("systemProperty",
            element("name", "OPENMRS_APPLICATION_DATA_DIRECTORY"),
            element("value", serverPath.getAbsolutePath() + File.separator )));
    
        List<Element> watchedProjects = getWatchedProjectsConfiguration(serverPath);
        if (!watchedProjects.isEmpty()) {
        	systemProperties.addAll(watchedProjects);
        }
        
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_JETTY_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_JETTY_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_JETTY_VERSION)
                ),
                goal("deploy-war"),
                configuration(
                        element(name("war"), new File(serverPath, warFile).getAbsolutePath()),
                        element(name("daemon"), "false"),
                        element(name("webApp"), webAppConfiguration.toArray(new Element[0])),
                        element(name("systemProperties"), systemProperties.toArray(new Element[0]))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

	private List<Element> getWatchedProjectsConfiguration(File serverPath) throws MojoExecutionException {
	    Server serverConfig = Server.loadServer(serverPath);
        List<Element> watched = new ArrayList<Element>();
        Set<Project> watchedProjects = serverConfig.getWatchedProjects();
        if (!watchedProjects.isEmpty()) {
        	getLog().info(" ");
        	getLog().info("Hot redeployment for controllers and gsps enabled.");
        	
        	getLog().info(" ");

	        for (Project project : watchedProjects) {
		        watched.add(element("systemProperty", element("name", "uiFramework.development." + project.getArtifactId()),
		        	element("value", project.getPath())));
	        }
        }
        
        return watched;
    }
}
