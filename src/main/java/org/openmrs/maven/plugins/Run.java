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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

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
        /*
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "install");
            pb.directory(serverPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                getLog().info(s);
            }
            p.waitFor();
            int exitCode = p.exitValue();
            if (exitCode == 0) {
                File server = new File(serverPath, "server");
                ProcessBuilder pb2 = new ProcessBuilder("mvn", "jetty:run");
                pb2.directory(server);
                pb2.redirectErrorStream(true);
                Process p2 = pb2.start();
                BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
                while ((s = stdInput2.readLine()) != null) {
                    getLog().info(s);
                }
                p2.waitFor();
                int exitCode2 = p2.exitValue();
                if (exitCode2 != 0) {
                    throw new MojoExecutionException("There are error during starting server");
                }
            }
            else {
                throw new MojoExecutionException("There are error during installing server");
            }

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }*/
        File openmrsPath = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        openmrsPath.mkdirs();
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.8")
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"),
                                element("artifactItem",
                                        element("groupId", "org.openmrs.web"),
                                        element("artifactId", "openmrs-webapp"),
                                        element("version", "1.11.3"),
                                        element("type", "war"),
                                        element("outputDirectory", openmrsPath.getAbsolutePath()),
                                        element("destFileName", "openmrs.war")),
                                element("artifactItem",
                                        element("groupId", "com.h2database"),
                                        element("artifactId", "h2"),
                                        element("version", "1.2.135"),
                                        element("type", "jar"),
                                        element("outputDirectory", openmrsPath.getAbsolutePath()),
                                        element("destFileName", "h2-1.2.135.jar")))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );

        File tempDirectory = new File(openmrsPath, "tmp");
        tempDirectory.mkdirs();
        executeMojo(
                plugin(
                        groupId("org.eclipse.jetty"),
                        artifactId("jetty-maven-plugin"),
                        version("9.0.4.v20130625")
                ),
                goal("run-war"),
                configuration(
                        element(name("war"), new File(openmrsPath, "openmrs.war").getAbsolutePath()),
                        element(name("webApp"),
                                element("contextPath", "/openmrs"),
                                element("tempDirectory", tempDirectory.getAbsolutePath()),
                                element("extraClasspath", new File(openmrsPath, "h2-1.2.135.jar").getAbsolutePath())),
                        element(name("systemProperties"),
                                element("systemProperty",
                                        element("name", "OPENMRS_INSTALLATION_SCRIPT"),
                                        element("value", "classpath:installation.h2.properties")),
                                element("systemProperty",
                                        element("name", "OPENMRS_APPLICATION_DATA_DIRECTORY"),
                                        element("value", openmrsPath.getAbsolutePath())))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
