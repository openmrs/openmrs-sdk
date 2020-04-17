package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DockerHelper;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal delete
 * @requiresProject false
 */
public class Delete extends AbstractTask{

    private static final String TEMPLATE_SUCCESS = "Server '%s' removed successfully";

    /**
     * @parameter  property="serverId"
     */
    private String serverId;

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = loadValidatedServer(serverId);
        try {
            FileUtils.deleteDirectory(server.getServerDirectory());

            if(StringUtils.isNotBlank(server.getContainerId())){
                new DockerHelper(mavenProject, mavenSession, pluginManager, wizard).runDbContainer(
                        server.getContainerId(),
                        server.getMySqlPort(),
                        server.getDbUser(),
                        server.getDbPassword());
            }
            if (server.isMySqlDb()) {
                String dbName = server.getDbName();
                String dbUser = server.getDbUser();
                String dbPass = server.getDbPassword();
                String dbUri = server.getDbUri();
                DBConnector connector = new DBConnector(dbUri, dbUser, dbPass, dbName);
                connector.dropDatabase();
                connector.close();
            }
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (SQLException e) {
            throw new MojoExecutionException("Failed to drop database", e);
        }
    }
}
