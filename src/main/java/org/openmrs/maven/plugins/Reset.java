package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal reset
 * @requiresProject false
 */
public class Reset extends AbstractMojo{

    private static final String TEMPLATE_SUCCESS = "Server '%s' has been reset, user modules were saved";
    private static final String TEMPLATE_SUCCESS_FULL = "Server '%s' has been reset, user modules were removed";

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
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${full}" default-value=false
     */
    private String full;

    /**
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        Wizard helper = new DefaultWizard(prompter);
        if (serverId == null) {
            File currentProperties = helper.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = helper.getServerPath(serverId);
        ServerConfig properties = ServerConfig.loadServerConfig(serverPath);
        DBConnector connector = null;
        try {
            String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverPath.getName());
            String uri = properties.getParam(SDKConstants.PROPERTY_DB_URI);
            uri = uri.substring(0, uri.lastIndexOf("/"));
            connector = new DBConnector(uri, properties.getParam(SDKConstants.PROPERTY_DB_USER),
                                          properties.getParam(SDKConstants.PROPERTY_DB_PASS),
                                          dbName);
            connector.dropDatabase();
            connector.close();
        } catch (SQLException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            if (connector != null) try {
                connector.close();
            } catch (SQLException e) {
                getLog().error(e.getMessage());
            }
        }
        boolean isPlatform = properties.getParam(SDKConstants.PROPERTY_VERSION) == null;
        Server server = new Server.ServerBuilder()
                .setServerId(properties.getParam(SDKConstants.PROPERTY_SERVER_ID))
                .setVersion(properties.getParam(SDKConstants.PROPERTY_PLATFORM))
                .setDbDriver(properties.getParam(SDKConstants.PROPERTY_DB_DRIVER))
                .setDbUri(properties.getParam(SDKConstants.PROPERTY_DB_URI))
                .setDbUser(properties.getParam(SDKConstants.PROPERTY_DB_USER))
                .setDbPassword(properties.getParam(SDKConstants.PROPERTY_DB_PASS))
                .setInteractiveMode("false")
                .build();
        if (helper.checkYes(full)) {
            try {
                SetupPlatform platform = new SetupPlatform(mavenProject, mavenSession, prompter, pluginManager);
                FileUtils.deleteDirectory(serverPath);
                platform.setup(server, isPlatform, true);
                getLog().info(String.format(TEMPLATE_SUCCESS_FULL, server.getServerId()));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        else {
            UpgradePlatform upgradePlatform = new UpgradePlatform(mavenProject, mavenSession, pluginManager, prompter);
            upgradePlatform.upgradeServer(server.getServerId(), server.getVersion(), isPlatform);
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        }
    }
}
