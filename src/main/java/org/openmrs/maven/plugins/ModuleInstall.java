package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.ConfigurationManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal install-module
 * @requiresProject false
 */
public class ModuleInstall extends AbstractMojo {

    private static final String DEFAULT_FAIL_MESSAGE = "Server with such serverId is not exists";

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

    public ModuleInstall() {};

    public ModuleInstall(Prompter prompter) {
        this.prompter = prompter;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File serverPath = getServerPath(helper, serverId);
        Artifact artifact = getArtifactForSelectedParameters(helper, groupId, artifactId, version);
        ConfigurationManager manager = new ConfigurationManager(new File(serverPath, SDKConstants.OPENMRS_SERVER_POM).getPath(), getLog());
        Xpp3Dom item = manager.getArtifactItem(artifact);
        if (item == null) {
            List<Artifact> list = new ArrayList<Artifact>();
            list.add(artifact);
            manager.addArtifactListToConfiguration(list);
            getLog().info(String.format("Module with groupId: '%s', artifactId: '%s', version: '%s' was added to server config.",
                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        }
        else {
            String oldVersion = item.getChild("version").getValue();
            manager.updateArtifactItem(artifact);
            getLog().info(String.format("Module version for groupId: '%s', artifactId: '%s' was updated from '%s' to '%s'",
                    artifact.getGroupId(), artifact.getArtifactId(), oldVersion, artifact.getVersion()));
        }
        manager.apply();
    }

    /**
     * Get attribute values and prompt if not selected
     * @param helper
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public Artifact getArtifactForSelectedParameters(AttributeHelper helper, String groupId, String artifactId, String version) {
        File pomFile = new File(System.getProperty("user.dir"), "pom.xml");
        String moduleGroupId = null;
        String moduleArtifactId = null;
        String moduleVersion = null;
        if (pomFile.exists() && (artifactId == null) && (version == null)) {
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
        // prompt all unset values
        else {
            moduleGroupId = groupId;
            try {
                moduleArtifactId = helper.promptForValueIfMissing(artifactId, "artifactId");
                moduleVersion = helper.promptForValueIfMissing(version, "version");
            } catch (PrompterException e) {
                getLog().error(e.getMessage());
            }
        }
        // update server pom
        return new Artifact(moduleArtifactId, moduleVersion, moduleGroupId);
    }

    /**
     * Get path to server by serverId and prompt if missing
     * @param helper
     * @return
     * @throws MojoFailureException
     */
    public File getServerPath(AttributeHelper helper, String serverId, String failureMessage) throws MojoFailureException {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        String resultServerId = null;
        try {
            resultServerId = helper.promptForValueIfMissing(serverId, "serverId");
        } catch (PrompterException e) {
            getLog().error(e.getMessage());
        }
        File serverPath = new File(omrsHome, resultServerId);
        if (!serverPath.exists()) {
            throw new MojoFailureException(failureMessage);
        }
        return serverPath;
    }

    /**
     * Get server with default failure message
     * @param helper
     * @param serverId
     * @return
     * @throws MojoFailureException
     */
    public File getServerPath(AttributeHelper helper, String serverId) throws MojoFailureException {
        return getServerPath(helper, serverId, DEFAULT_FAIL_MESSAGE);
    }
}
