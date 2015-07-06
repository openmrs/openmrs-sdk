package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.model.Server;

/**
 *
 * @goal setup
 * @requiresProject false
 *
 */
public class Setup extends AbstractMojo {

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
     * Interactive mode param
     *
     * @parameter expression="${interactiveMode}" default-value="false"
     */
    private String interactiveMode;

    /**
     * Server id (folder name)
     *
     * @parameter expression="${serverId}"
     *
     */
    private String serverId;

    /**
     * Platform version
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * DB Driver type
     *
     * @parameter expression="${dbDriver}" default-value="mysql"
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter expression="${dbUri}"
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter expression="${dbUser}"
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter expression="${dbPassword}"
     */
    private String dbPassword;

    /**
     * Component for user prompt
     *
     * @component
     */
    private Prompter prompter;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        SetupPlatform configurator = new SetupPlatform(mavenProject, mavenSession, prompter, pluginManager);
        Server server = new Server.ServerBuilder()
                        .setServerId(serverId)
                        .setVersion(version)
                        .setDbDriver(dbDriver)
                        .setDbUri(dbUri)
                        .setDbUser(dbUser)
                        .setDbPassword(dbPassword)
                        .setInteractiveMode(interactiveMode)
                        .build();
        // setup non-platform server
        String serverPath = configurator.setup(server, false);
        getLog().info("Server configured successfully, path: " + serverPath);
    }
}
