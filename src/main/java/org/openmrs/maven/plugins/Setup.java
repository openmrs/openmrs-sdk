package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.ConfigurationManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.util.List;

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
     * @parameter expression="${interactiveMode}" default-value="true"
     */
    private String interactiveMode;

    /**
     * Server id (folder name)
     *
     * @parameter expression="${serverId}"
     * @required
     *
     */
    private String serverId;

    /**
     * Platform version
     *
     * @parameter expression="${version} default-value="2.2"
     */
    private String version;

    /**
     * DB Driver type
     *
     * @parameter expression="${dbDriver} default-value="mysql"
     */
    private String dbDriver;

    /**
     * DB Uri
     *
     * @parameter expression="${dbUri}
     * @required
     */
    private String dbUri;

    /**
     * DB User
     *
     * @parameter expression="${dbUser}
     * @required
     */
    private String dbUser;

    /**
     * DB Pass
     *
     * @parameter expression="${dbPassword}
     * @required
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
                        .setNestedServerId(serverId)
                        .setNestedVersion(version)
                        .setNestedDbDriver(dbDriver)
                        .setNestedDbUri(dbUri)
                        .setNestedDbUser(dbUser)
                        .setNestedDbPassword(dbPassword)
                        .setNestedInteractiveMode(interactiveMode)
                        .build();
        String serverPath = configurator.setup(server);
        ConfigurationManager manager = new ConfigurationManager(serverPath);
        List<Artifact> artifacts = SDKConstants.ARTIFACTS.get(version);
        if (artifacts != null) {
            manager.addArtifactsToConfiguration(artifacts);
            manager.apply();
            getLog().info("Modules for version " + version + " configured successfully");
        }
        else getLog().info("There are no modules for selected OpenMRS version");
        getLog().info("Server configured successfully");
    }
}
