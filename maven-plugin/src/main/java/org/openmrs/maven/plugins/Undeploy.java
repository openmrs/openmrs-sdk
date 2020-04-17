package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

/**
 * @goal undeploy
 * @requiresProject false
 */
public class Undeploy extends AbstractTask {

    /**
     * @parameter  property="serverId"
     */
    private String serverId;

    /**
     * @parameter  property="artifactId"
     */
    private String artifactId;

    /**
     * @parameter  property="groupId" default-value="org.openmrs.module"
     */
    private String groupId;

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        Deploy deployer = new Deploy(this);
        if (serverId == null) {
            File currentProperties = Server.checkCurrentDirForServer();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = loadValidatedServer(serverId);
        Artifact artifact = deployer.getModuleArtifactForSelectedParameters(groupId, artifactId, "default");
        artifact.setArtifactId(StringUtils.stripEnd(artifact.getArtifactId(), "-omod"));
        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        File[] listOfModules = modules.listFiles();
        for (File mod : listOfModules) {
            if (mod.getName().startsWith(artifact.getArtifactId())) {
                boolean deleted = mod.delete();
                if (deleted) {
                    Server properties = Server.loadServer(serverId);
                    properties.removeModuleProperties(artifact);
                    properties.saveAndSynchronizeDistro();
                    getLog().info(String.format("Module with groupId: '%s', artifactId: '%s' was successfully removed from server.",
                            artifact.getGroupId(), artifact.getArtifactId()));
                    return;
                }
                else {
                    throw new MojoExecutionException(String.format("Error during removing Module with groupId: '%s', artifactId: '%s'.",
                            artifact.getGroupId(), artifact.getArtifactId()));
                }
            }
        }
        throw new MojoExecutionException(String.format("There no module with groupId: '%s', artifactId: '%s' on server.", artifact.getGroupId(), artifact.getArtifactId()));
    }
}
