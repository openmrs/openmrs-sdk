package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

@Mojo(name = "undeploy", requiresProject = false)
public class Undeploy extends AbstractServerTask {

    @Parameter(property = "artifactId")
    private String artifactId;

    @Parameter(defaultValue = "org.openmrs.module", property = "groupId")
    private String groupId;

    public void executeTask() throws MojoExecutionException {
        Server server = getServer();

        Deploy deployer = new Deploy(this);
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

    @Override
    protected Server loadServer() throws MojoExecutionException {
        return loadValidatedServer(serverId);
    }
}
