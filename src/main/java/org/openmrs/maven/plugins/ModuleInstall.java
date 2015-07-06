package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.ConfigurationManager;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.model.Version;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal install-module
 * @requiresProject false
 */
public class ModuleInstall extends AbstractMojo {

    private static final String DEFAULT_OK_MESSAGE = "Module '%s' installed successfully";
    private static final String DEFAULT_UPDATE_MESSAGE = "Module '%s' was updated to version '%s'";
    private static final String TEMPLATE_UPDATE = "Module is installed already. Do you want to upgrade it to version '%s'?";
    private static final String TEMPLATE_DOWNGRADE = "Installed version '%s' of module higher than target '%s'";

    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     */
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

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
        installModule(serverId, groupId, artifactId, version);
    }

    /**
     * Install module to selected server
     * @param serverId
     * @param groupId
     * @param artifactId
     * @param version
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void installModule(String serverId, String groupId, String artifactId, String version) throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File serverPath = helper.getServerPath(serverId);
        Artifact artifact = getArtifactForSelectedParameters(helper, groupId, artifactId, version);
        String originalId = artifact.getArtifactId();
        artifact.setArtifactId(artifact.getArtifactId() + "-omod");
        File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
        Element[] artifactItems = new Element[1];
        artifactItems[0] = artifact.toElement(modules.getPath());

        File[] listOfModules = modules.listFiles();
        boolean versionUpdated = false;
        boolean removed = false;
        for (File itemModule : listOfModules) {
            String[] parts = itemModule.getName().split("-");
            if (originalId.equals(parts[0])) {
                try {
                    Version oldVersion = new Version(parts[1].substring(0, parts[1].lastIndexOf('.')));
                    Version newVersion = new Version(artifact.getVersion());
                    if (oldVersion.higher(newVersion)) {
                        throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, oldVersion.toString(), newVersion.toString()));
                    }
                    else if (oldVersion.lower(newVersion)) {
                        boolean agree = helper.dialogYesNo(String.format(TEMPLATE_UPDATE, artifact.getVersion()));
                        if (!agree) {
                            return;
                        }
                    }
                    versionUpdated = true;
                    removed = itemModule.delete();
                    break;

                } catch (PrompterException e) {
                    throw new MojoExecutionException(e.getMessage());
                }

            }
        }
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"), artifactItems)
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
        if (versionUpdated) {
            if (removed) getLog().info(String.format(DEFAULT_UPDATE_MESSAGE, originalId, artifact.getVersion()));
        }
        else getLog().info(String.format(DEFAULT_OK_MESSAGE, originalId));
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
        if (pomFile.exists() && (artifactId == null)) {
            ConfigurationManager manager = new ConfigurationManager(pomFile.getPath(), getLog());
            if (manager.getParent() != null) {
                moduleGroupId = manager.getParent().getGroupId();
                moduleArtifactId = manager.getParent().getArtifactId();
                moduleVersion = (version != null) ? version : manager.getParent().getVersion();
            }
            else {
                moduleGroupId = manager.getGroupId();
                moduleArtifactId = manager.getArtifactId();
                moduleVersion = (version != null) ? version: manager.getVersion();
            }
        }
        else {
            moduleGroupId = groupId;
            try {
                moduleArtifactId = helper.promptForValueIfMissing(artifactId, "artifactId");
                moduleVersion = helper.promptForValueIfMissing(version, "version");
            } catch (PrompterException e) {
                getLog().error(e.getMessage());
            }
        }
        return new Artifact(moduleArtifactId, moduleVersion, moduleGroupId);
    }
}
