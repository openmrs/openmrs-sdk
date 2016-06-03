package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal reset
 * @requiresProject false
 */
public class Reset extends AbstractTask {

    private static final String TEMPLATE_SUCCESS = "Server '%s' has been reset, user modules were saved";
    private static final String TEMPLATE_SUCCESS_FULL = "Server '%s' has been reset, user modules were removed";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${full}" default-value=false
     */
    private String full;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server properties = Server.loadServer(serverId);
        DBConnector connector = null;
        try {
            String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverId);
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
                Setup platform = new Setup(this);
                FileUtils.deleteDirectory(server.getServerDirectory());
                platform.setup(server, isPlatform, true);
                getLog().info(String.format(TEMPLATE_SUCCESS_FULL, server.getServerId()));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        else {
            UpgradePlatform upgradePlatform = new UpgradePlatform(this);
            upgradePlatform.upgradeServer(server.getServerId(), server.getVersion(), isPlatform);
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        }
    }
}
