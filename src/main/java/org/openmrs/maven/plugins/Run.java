package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

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
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        if (serverId == null) {
            File currentProperties = helper.getCurrentServerPath(getLog());
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = helper.getServerPath(serverId, NO_SERVER_TEXT);
        serverPath.mkdirs();
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
        if (warFile == null) {
            throw new MojoExecutionException("Error during running server: war file was not found");
        }
        else if (h2File == null) {
            throw new MojoExecutionException("Error during running server: h2 file was not found");
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_JETTY_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_JETTY_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_JETTY_VERSION)
                ),
                goal("run-war"),
                configuration(
                        element(name("war"), new File(serverPath, warFile).getAbsolutePath()),
                        element(name("webApp"),
                                element("contextPath", "/openmrs"),
                                element("tempDirectory", tempDirectory.getAbsolutePath()),
                                element("extraClasspath", new File(serverPath, h2File).getAbsolutePath())),
                        element(name("systemProperties"),
                                element("systemProperty",
                                        element("name", "OPENMRS_INSTALLATION_SCRIPT"),
                                        element("value", new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath())),
                                element("systemProperty",
                                        element("name", "OPENMRS_APPLICATION_DATA_DIRECTORY"),
                                        element("value", serverPath.getAbsolutePath())))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
