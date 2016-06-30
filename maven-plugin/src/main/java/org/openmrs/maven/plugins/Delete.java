package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal delete
 * @requiresProject false
 */
public class Delete extends AbstractTask{

    private static final String TEMPLATE_SUCCESS = "Server '%s' removed successfully";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        try {
            Server server = Server.loadServer(serverId);
            FileUtils.deleteDirectory(server.getServerDirectory());

            if (server.isMySqlDb()) {
                String dbName = server.getParam(Server.PROPERTY_DB_NAME);
                String dbUser = server.getParam(Server.PROPERTY_DB_USER);
                String dbPass = server.getParam(Server.PROPERTY_DB_PASS);
                String dbUri = server.getParam(Server.PROPERTY_DB_URI);
                DBConnector connector = new DBConnector(dbUri, dbUser, dbPass, dbName);
                connector.dropDatabase();
                connector.close();
            }
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (SQLException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
