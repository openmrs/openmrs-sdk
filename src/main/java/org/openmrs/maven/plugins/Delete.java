package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;

import java.io.File;
import java.io.IOException;

/**
 * @goal delete
 * @requiresProject false
 */
public class Delete extends AbstractMojo{

    private static final String TEMPLATE_SUCCESS = "Server '%s' removed successfully";
    private static final String TEMPLATE_ERROR = "Unable to remove server '%s'";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File server = helper.getServerPath(serverId);
        try {
            FileUtils.deleteDirectory(server);
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getName()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
