package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

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
     * @required
     * @component
     */
    Wizard wizard;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = wizard.getServerPath(serverId);
        Server properties = Server.loadServer(serverPath);
        DBConnector connector = null;
        try {
            String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverPath.getName());
            String uri = properties.getParam(Server.PROPERTY_DB_URI);
            uri = uri.substring(0, uri.lastIndexOf("/"));
            connector = new DBConnector(uri, properties.getParam(Server.PROPERTY_DB_USER),
                                          properties.getParam(Server.PROPERTY_DB_PASS),
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
        boolean isPlatform = properties.getParam(Server.PROPERTY_VERSION) == null;
        Server server = new Server.ServerBuilder()
                .setServerId(properties.getParam(Server.PROPERTY_SERVER_ID))
                .setVersion(properties.getParam(Server.PROPERTY_PLATFORM))
                .setDbDriver(properties.getParam(Server.PROPERTY_DB_DRIVER))
                .setDbUri(properties.getParam(Server.PROPERTY_DB_URI))
                .setDbUser(properties.getParam(Server.PROPERTY_DB_USER))
                .setDbPassword(properties.getParam(Server.PROPERTY_DB_PASS))
                .setInteractiveMode("false")
                .build();
        if (wizard.checkYes(full)) {
            try {
                Setup platform = new Setup(mavenProject, mavenSession, pluginManager);
                FileUtils.deleteDirectory(serverPath);
                platform.setup(server, isPlatform, true);
                getLog().info(String.format(TEMPLATE_SUCCESS_FULL, server.getServerId()));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        else {
            UpgradePlatform upgradePlatform = new UpgradePlatform(mavenProject, mavenSession, pluginManager);
            upgradePlatform.upgradeServer(server.getServerId(), server.getVersion(), isPlatform);
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        }
    }
}
