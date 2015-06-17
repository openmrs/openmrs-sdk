package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.ConfigurationManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

/**
 * @goal uninstall-module
 * @requiresProject false
 */
public class ModuleUninstall extends AbstractMojo {

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${groupId}" default-value="org.openmrs.module"
     */
    private String groupId;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ModuleInstall installer = new ModuleInstall(prompter);
        AttributeHelper helper = new AttributeHelper(prompter);
        File serverPath = installer.getServerPath(helper, serverId);
        Artifact artifact = installer.getArtifactForSelectedParameters(helper, groupId, artifactId, "default");
        ConfigurationManager manager = new ConfigurationManager(new File(serverPath, SDKConstants.OPENMRS_SERVER_POM).getPath(), getLog());
        Xpp3Dom item = manager.getArtifactItem(artifact);
        if (item == null) getLog().error(String.format("There no module with groupId: '%s', artifactId: '%s' on server.",
                artifact.getGroupId(), artifact.getArtifactId()));
        else {
            boolean removed = manager.removeArtifactItem(artifact);
            if (removed) {
                manager.apply();
                getLog().info(String.format("Module with groupId: '%s', artifactId: '%s' was successfully removed from server.",
                        artifact.getGroupId(), artifact.getArtifactId()));
            }
            else getLog().info(String.format("Error during removing Module with groupId: '%s', artifactId: '%s'.",
                    artifact.getGroupId(), artifact.getArtifactId()));
        }
    }
}
