package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
        initUtilities();
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);
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
        boolean isPlatform = server.getParam(Server.PROPERTY_VERSION) == null;
        if (wizard.checkYes(full)) {
            try {
                Server newServer = new Server.ServerBuilder()
                        .setServerId(server.getParam(Server.PROPERTY_SERVER_ID))
                        .setVersion(server.getParam(Server.PROPERTY_PLATFORM))
                        .setDbDriver(server.getParam(Server.PROPERTY_DB_DRIVER))
                        .setDbUri(server.getParam(Server.PROPERTY_DB_URI))
                        .setDbUser(server.getParam(Server.PROPERTY_DB_USER))
                        .setDbPassword(server.getParam(Server.PROPERTY_DB_PASS))
                        .setInteractiveMode("false")
                        .build();
                Setup setup = new Setup(this);
                FileUtils.deleteDirectory(newServer.getServerDirectory());
                setup.setup(newServer, isPlatform, true, null);
                getLog().info(String.format(TEMPLATE_SUCCESS_FULL, newServer.getServerId()));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        else {
	        if(isPlatform){
                ServerUpgrader serverUpgrader = new ServerUpgrader(this);
		        serverUpgrader.upgradePlatform(server, server.getVersion());
	        } else {
		        resetDistro(server);
	        }
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getServerId()));
        }
    }

    public void resetDistro(Server server) throws MojoExecutionException, MojoFailureException {
        Setup setup = new Setup(this);
        //delete webapp and modules of existing server version, leave out modules installed by user
        List<Artifact> userModules = server.getUserModules();
        if(server.getDbDriver().equals(SDKConstants.DRIVER_H2)){
            userModules.add(SDKConstants.H2_ARTIFACT);
        }
        File modulesFolder = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        deleteNonUserModulesFromDir(userModules, server.getServerDirectory());
        deleteNonUserModulesFromDir(userModules, modulesFolder);
        //delete installation.properties file on server to avoid Setup task errors with already existing server
        server.saveBackupProperties();
        server.delete();
        setup.setup(server, false, true, null);
        server.deleteBackupProperties();
        getLog().info("Server reset successful");
    }

    private void deleteNonUserModulesFromDir(List<Artifact> userModules, File dir) {
        File[] files = dir.listFiles();
        if(files != null){
            for (File f: files) {
                String type = f.getName().substring(f.getName().lastIndexOf(".") + 1);
                if (SDKConstants.isExtensionSupported(type)) {
                    boolean toDelete = true;
                    String id = getId(f.getName());
                    for(Artifact userModule : userModules){
                        if(userModule.getArtifactId().equals(id)){
                            toDelete = false;
                            break;
                        }
                    }
                    if(toDelete){
                        f.delete();
                    }
                }
            }
        }
    }
    /**
     * Get artifact id from module name (without -omod, etc)
     * @param name of file which id will be obtained
     * @return
     */
    public String getId(String name) {
        int index = name.indexOf('-');
        if (index == -1) return name;
        return name.substring(0, index);
    }
}
