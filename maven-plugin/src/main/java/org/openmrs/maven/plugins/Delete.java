package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DockerHelper;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Used to delete an SDK server and the server database
 */
@Mojo(name = "delete", requiresProject = false)
public class Delete extends AbstractServerTask {

	private static final String TEMPLATE_SUCCESS = "Server '%s' removed successfully";

	public void executeTask() throws MojoExecutionException {
		Server server = getServer();
		try {
			FileUtils.deleteDirectory(server.getServerDirectory());

			if (StringUtils.isNotBlank(server.getContainerId())) {
				new DockerHelper(getMavenEnvironment()).runDbContainer(
						server.getContainerId(),
						server.getMySqlPort(),
						server.getDbUser(),
						server.getDbPassword());
			}

			if (server.isMySqlDb() || server.isPostgreSqlDb()) {
				String dbName = server.getDbName();
				String dbUser = server.getDbUser();
				String dbPass = server.getDbPassword();
				String dbUri = server.getDbUri();
				DBConnector connector = new DBConnector(dbUri, dbUser, dbPass, dbName);
				connector.dropDatabase();
				connector.close();
			}
			getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		catch (SQLException e) {
			throw new MojoExecutionException("Failed to drop database", e);
		}
	}

    @Override
    protected Server loadServer() throws MojoExecutionException {
        return loadValidatedServer(serverId);
    }
}
