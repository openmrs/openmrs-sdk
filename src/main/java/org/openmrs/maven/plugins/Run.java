package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;

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
        ModuleInstall installer = new ModuleInstall(prompter);
        File serverPath = installer.getServerPath(helper, serverId, NO_SERVER_TEXT);
        serverPath.mkdirs();
        File tempDirectory = new File(serverPath, "tmp");
        tempDirectory.mkdirs();
        String warFile = null;
        for(File file: serverPath.listFiles()) {
            if (file.getName().startsWith("openmrs") && (file.getName().endsWith(".war"))) {
                warFile = file.getName();
                break;
            }
        };
        if (warFile == null) throw new MojoExecutionException("Error during running server: war file was not found");
        executeMojo(
                plugin(
                        groupId("org.eclipse.jetty"),
                        artifactId("jetty-maven-plugin"),
                        version("9.0.4.v20130625")
                ),
                goal("run-war"),
                configuration(
                        element(name("war"), new File(serverPath, warFile).getAbsolutePath()),
                        element(name("webApp"),
                                element("contextPath", "/openmrs"),
                                element("tempDirectory", tempDirectory.getAbsolutePath()),
                                element("extraClasspath", new File(serverPath, "h2-1.2.135.jar").getAbsolutePath())),
                        element(name("systemProperties"),
                                element("systemProperty",
                                        element("name", "OPENMRS_INSTALLATION_SCRIPT"),
                                        element("value", "classpath:installation.h2.properties")),
                                element("systemProperty",
                                        element("name", "OPENMRS_APPLICATION_DATA_DIRECTORY"),
                                        element("value", serverPath.getAbsolutePath())))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
