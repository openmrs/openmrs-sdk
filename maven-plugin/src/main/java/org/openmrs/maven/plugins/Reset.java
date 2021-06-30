package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.IOException;
import java.sql.SQLException;

@Mojo(name = "reset", requiresProject = false)
public class Reset extends AbstractServerTask {

    private static final String TEMPLATE_SUCCESS_FULL = "Server '%s' has been reset";

    public void executeTask() throws MojoExecutionException {
        Server server = getServer();
        if(StringUtils.isNotBlank(server.getContainerId())){
            new DockerHelper(mavenProject, mavenSession, pluginManager, wizard).runDbContainer(
                    server.getContainerId(),
                    server.getMySqlPort(),
                    server.getDbUser(),
                    server.getDbPassword());
        }
        if(server.getDbDriver().equals(SDKConstants.DRIVER_MYSQL)){
            DBConnector connector = null;
            try {
                String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverId);
                String uri = server.getParam(Server.PROPERTY_DB_URI);
                uri = uri.substring(0, uri.lastIndexOf("/"));
                connector = new DBConnector(uri, server.getParam(Server.PROPERTY_DB_USER),
                        server.getParam(Server.PROPERTY_DB_PASS),
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
        }
        try {
            Server newServer = new Server.ServerBuilder()
                    .setServerId(server.getServerId())
                    .setVersion(server.getVersion())
                    .setPlatformVersion(server.getPlatformVersion())
                    .setDbDriver(server.getDbDriver())
                    .setDbName(server.getDbName())
                    .setDbUri(server.getDbUri())
                    .setDbUser(server.getDbUser())
                    .setDbPassword(server.getDbPassword())
                    .setInteractiveMode(false)
                    .setJavaHome(server.getJavaHome())
                    .setContainerId(server.getContainerId())
                    .build();
            Setup setup = new Setup(this);
            DistroProperties distroProperties = server.getDistroProperties();
            FileUtils.deleteDirectory(server.getServerDirectory());
            setup.setup(newServer, distroProperties);
            getLog().info(String.format(TEMPLATE_SUCCESS_FULL, newServer.getServerId()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    @Override
    protected Server loadServer() throws MojoExecutionException {
        return loadValidatedServer(serverId);
    }
}
