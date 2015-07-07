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
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal reset
 * @requiresProject false
 */
public class Reset extends AbstractMojo{

    private static final String TEMPLATE_SUCCESS = "Server '%s' was reset successfully, user modules were saved";
    private static final String TEMPLATE_SUCCESS_FULL = "Server '%s' was reset successfully, user modules were removed";

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
        AttributeHelper helper = new AttributeHelper(prompter);
        if (serverId == null) {
            File currentProperties = helper.getCurrentServerPath(getLog());
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = helper.getServerPath(serverId);
        PropertyManager properties = new PropertyManager(new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getPath(), getLog());
        DBConnector connector = null;
        try {
            String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverPath.getName());
            connector = new DBConnector(properties.getParam(SDKConstants.PROPERTY_DB_URI),
                                                    properties.getParam(SDKConstants.PROPERTY_DB_USER),
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
                platform.setup(server, isPlatform);
                getLog().info(String.format(TEMPLATE_SUCCESS_FULL, server.getServerId()));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        else {
            UpgradePlatform upgradePlatform = new UpgradePlatform(mavenProject, mavenSession, pluginManager, prompter);
            final boolean allowEqualVersion = true;
            upgradePlatform.upgradeServer(server.getServerId(), server.getVersion(), isPlatform, allowEqualVersion);
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        }
    }
}
