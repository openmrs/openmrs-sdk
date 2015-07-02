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
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.PropertyManager;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @goal upgrade
 * @requiresProject false
 *
 */
public class Upgrade extends AbstractMojo{

    private static final String TEMPLATE_SAME_VERSION = "Server '%s' already has version '%s'";
    private static final String TEMPLATE_SUCCESS = "Server '%s' was successfully upgraded from version '%s' to '%s'";
    private static final String TEMPLATE_INVALID_PREV_PERSION = "Server '%s' has invalid version '%s'";
    private static final String TEMPLATE_INVALID_PROPERTIES = "Server '%s' hav invalid properties";

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
     * Server id (folder name)
     *
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * Platform version
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * Component for user prompt
     *
     * @component
     */
    private Prompter prompter;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ModuleInstall moduleInstall = new ModuleInstall(prompter);
        AttributeHelper helper = new AttributeHelper(prompter);
        String resultVersion = null;
        String resultServer = null;
        // check if we are inside the some server folder
        File currentPath = new File(System.getProperty("user.dir"));
        File currentPathProperties = new File(currentPath, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File currentParentProperties = new File(currentPath.getParentFile(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        if (currentPathProperties.exists() || currentParentProperties.exists()) {
            if (currentPathProperties.exists()) serverId = currentPath.getName();
            else serverId = currentPath.getParent();
        }
        try {
            resultServer = helper.promptForValueIfMissing(serverId, "serverId");
            resultVersion = helper.promptForValueIfMissing(version, "version");
        } catch (PrompterException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        File serverPath = moduleInstall.getServerPath(helper, resultServer);
        File propertyFile = new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        PropertyManager properties = new PropertyManager(propertyFile.getPath(), getLog());
        String oldVersion = properties.getParam(SDKConstants.PROPERTY_VERSION);
        if ((oldVersion == null) || (oldVersion.equals(""))) {
            throw new MojoExecutionException(String.format(TEMPLATE_INVALID_PROPERTIES, resultServer));
        }
        Server server = new Server.ServerBuilder()
                .setServerId(resultServer)
                .setVersion(resultVersion)
                .setDbDriver(properties.getParam(SDKConstants.PROPERTY_DB_DRIVER))
                .setDbUri(properties.getParam(SDKConstants.PROPERTY_DB_URI))
                .setDbUser(properties.getParam(SDKConstants.PROPERTY_DB_USER))
                .setDbPassword(properties.getParam(SDKConstants.PROPERTY_DB_PASS))
                .setInteractiveMode("false")
                .build();
        if (server.getVersion().equals(oldVersion)) {
            getLog().info(String.format(TEMPLATE_SAME_VERSION, resultServer, resultVersion));
            return;
        }
        String oldPlatformVersion = oldVersion.startsWith("1.") ? "1.x" : oldVersion;
        Map<String, Artifact> oldArtifacts = new HashMap<String, Artifact>();
        List<Artifact> artifactList = SDKConstants.ARTIFACTS.get(oldPlatformVersion);
        if (artifactList != null) {
            for (Artifact artifact: artifactList) {
                oldArtifacts.put(getId(artifact.getArtifactId()), artifact);
            }
            File modulePath = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
            File[] modules = modulePath.listFiles();
            for (File module: modules) {
                if (oldArtifacts.get(getId(module.getName())) != null) {
                    module.delete();
                }
            }
            // also remove core war and h2 module
            for (File coreModule: serverPath.listFiles()) {
                if (oldArtifacts.get(getId(coreModule.getName())) != null) {
                    coreModule.delete();
                }
            }
            // also remove properties file to show that server removed
            propertyFile.delete();
            SetupPlatform platform = new SetupPlatform(mavenProject, mavenSession, prompter, pluginManager);
            platform.setup(server, false);
            getLog().info(String.format(TEMPLATE_SUCCESS, resultServer, oldVersion, resultVersion));
        }
        else {
            throw new MojoExecutionException(String.format(TEMPLATE_INVALID_PREV_PERSION, resultServer, oldVersion));
        }
    }

    /**
     * Get artifact id from module name (without -omod, etc)
     * @param name
     * @return
     */
    private String getId(String name) {
        int index = name.indexOf('-');
        if (index == -1) return name;
        return name.substring(0, index);
    }
}
