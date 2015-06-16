package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.ConfigurationManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

/**
 * @goal install-module
 * @requiresProject false
 */
public class ModuleInstall extends AbstractMojo {

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
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        String resultServerId;
        try {
            resultServerId = AttributeHelper.makeServerId(prompter, omrsHome.getPath(), serverId);
        } catch (PrompterException e) {
            getLog().error(e.getMessage());
        }
        File pomFile = new File(System.getProperty("user.dir"), "pom.xml");
        String moduleGroupId, moduleArtifactId, moduleVersion;
        if (pomFile.exists()) {
            ConfigurationManager manager = new ConfigurationManager(pomFile.getPath(), getLog());
            if (manager.getParent() != null) {
                moduleGroupId = manager.getParent().getGroupId();
                moduleArtifactId = manager.getParent().getArtifactId();
                moduleVersion = manager.getParent().getVersion();
            }
            else {
                moduleGroupId = manager.getGroupId();
                moduleArtifactId = manager.getArtifactId();
                moduleVersion = manager.getVersion();
            }
        }
    }
}
