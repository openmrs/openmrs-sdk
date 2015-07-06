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
import org.openmrs.maven.plugins.model.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @goal upgrade-platform
 * @requiresProject false
 *
 */
public class UpgradePlatform extends AbstractMojo{
    private static final String TEMPLATE_SAME_VERSION = "Server '%s' already has version '%s'";
    private static final String TEMPLATE_SUCCESS = "Server '%s' was successfully upgraded from version '%s' to '%s'";
    private static final String TEMPLATE_INVALID_VERSION = "Cannot upgrade to version '%s'";
    private static final String TEMPLATE_INVALID_PROPERTIES = "Server '%s' hav invalid properties";
    private static final String TEMPLATE_DOWNGRADE = "Server '%s' has higher version, than you tried to upgrade";
    private static final String TEMPLATE_NONPLATFORM = "Cannot upgrade server '%s' from non-platform to platform";

    private static final String TEMP_PROPERTIES = "old.properties";

    public UpgradePlatform() {};

    public UpgradePlatform(MavenProject mavenProject,
                           MavenSession mavenSession,
                           BuildPluginManager pluginManager,
                           Prompter prompter) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.prompter = prompter;
    }

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
        upgradeServer(serverId, version, true);
    }

    /**
     * Upgrade server to selected version
     * @param serverId
     * @param version
     * @param isUpdateToPlatform
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void upgradeServer(String serverId, String version, boolean isUpdateToPlatform) throws MojoExecutionException, MojoFailureException {
        ModuleInstall moduleInstall = new ModuleInstall(prompter);
        AttributeHelper helper = new AttributeHelper(prompter);
        String resultVersion = null;
        String resultServer = null;
        // check if we are inside the some server folder
        if (serverId == null) {
            File currentPath = new File(System.getProperty("user.dir"));
            File currentPathProperties = new File(currentPath, SDKConstants.OPENMRS_SERVER_PROPERTIES);
            File currentParentProperties = new File(currentPath.getParentFile(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
            if (currentPathProperties.exists() || currentParentProperties.exists()) {
                if (currentPathProperties.exists()) serverId = currentPath.getName();
                else serverId = currentPath.getParent();
            }
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
        String webapp = properties.getParam(SDKConstants.PROPERTY_VERSION);
        String platform = properties.getParam(SDKConstants.PROPERTY_PLATFORM);

        // invalid old version
        if (platform == null) {
            throw new MojoExecutionException(String.format(TEMPLATE_INVALID_PROPERTIES, resultServer));
        }
        Version platformVersion = new Version(platform);
        Version nextVersion = new Version(resultVersion);
        if (isUpdateToPlatform) {
            if (webapp != null) {
                throw new MojoExecutionException(String.format(TEMPLATE_NONPLATFORM, resultServer));
            }
            else if (platformVersion.higher(nextVersion)) {
                throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
            }
            else if (platformVersion.equal(nextVersion)) {
                getLog().info(String.format(TEMPLATE_SAME_VERSION, resultServer, resultVersion));
                return;
            }
        }
        else {
            String targetWebApp = SDKConstants.WEBAPP_VERSIONS.get(resultVersion);
            if (targetWebApp == null) {
                throw new MojoExecutionException(String.format(TEMPLATE_INVALID_VERSION, resultVersion));
            }
            else {
                if (webapp == null) {
                    if (nextVersion.higher(platformVersion)) {
                        try {
                            boolean yes = helper.dialogYesNo("Do you want to upgrade platform server?");
                            if (!yes) {
                                return;
                            }
                        } catch (PrompterException e) {
                            throw new MojoExecutionException(e.getMessage());
                        }
                    }
                    else {
                        throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
                    }
                }
                else if (new Version(webapp).higher(new Version(targetWebApp)) || (platformVersion.higher(nextVersion))) {
                    throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
                }
                else if (new Version(webapp).equal(new Version(targetWebApp)) || (platformVersion.equal(nextVersion))) {
                    getLog().info(String.format(TEMPLATE_SAME_VERSION, resultServer, resultVersion));
                    return;
                }
            }
        }
        // flag if old server is platform server
        boolean isOldPlatformServer = webapp == null;
        // get list modules to remove after
        List<File> listFilesToRemove = getFileListToRemove(serverPath, platform, resultVersion, isOldPlatformServer);
        // also make a copy of old properties file
        File tempProperties = new File(serverPath, TEMP_PROPERTIES);
        properties.apply(tempProperties.getPath());
        // also remove copy of old properties file if success
        listFilesToRemove.add(tempProperties);
        propertyFile.delete();
        SetupPlatform setupPlatform = new SetupPlatform(mavenProject, mavenSession, prompter, pluginManager);
        Server server = new Server.ServerBuilder()
                .setServerId(resultServer)
                .setVersion(resultVersion)
                .setDbDriver(properties.getParam(SDKConstants.PROPERTY_DB_DRIVER))
                .setDbUri(properties.getParam(SDKConstants.PROPERTY_DB_URI))
                .setDbUser(properties.getParam(SDKConstants.PROPERTY_DB_USER))
                .setDbPassword(properties.getParam(SDKConstants.PROPERTY_DB_PASS))
                .setInteractiveMode("false")
                .build();
        setupPlatform.setup(server, isUpdateToPlatform);
        removeFiles(listFilesToRemove);
        getLog().info(String.format(TEMPLATE_SUCCESS, resultServer, platform, resultVersion));
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

    /**
     * Calculate list of files to remove
     * @param serverPath
     * @param oldVersion
     * @param targetVersion
     * @param isPlatform
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private List<File> getFileListToRemove(File serverPath, String oldVersion, String targetVersion, boolean isPlatform) throws MojoExecutionException, MojoFailureException {
        List<File> list = new ArrayList<File>();
        boolean oldPlatformVersion = SDKConstants.WEBAPP_VERSIONS.get(targetVersion) == null;
        // remove modules first
        if (!isPlatform) {
            File modulePath = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
            if (modulePath.exists() && !oldPlatformVersion) {
                Map<String, Artifact> oldModules = new HashMap<String, Artifact>();
                Map<String, Artifact> targetModules = new HashMap<String, Artifact>();
                List<Artifact> artifactList = SDKConstants.ARTIFACTS.get(oldVersion);
                List<Artifact> targerArtifactList = SDKConstants.ARTIFACTS.get(targetVersion);
                if (artifactList != null) {
                    for (Artifact artifact: artifactList) {
                        oldModules.put(getId(artifact.getArtifactId()), artifact);
                    }
                }
                if (targerArtifactList != null) {
                    for (Artifact artifact: targerArtifactList) {
                        targetModules.put(getId(artifact.getArtifactId()), artifact);
                    }
                }
                File[] modules = modulePath.listFiles();
                for (File module: modules) {
                    Artifact oldMod = oldModules.get(getId(module.getName()));
                    Artifact targetMod = targetModules.get(getId(module.getName()));
                    // exist and not equal to knew module
                    if (oldMod != null) {
                        if ((targetMod == null) || (!oldMod.getDestFileName().equals(targetMod.getDestFileName()))) {
                            list.add(module);
                        }
                    }
                }
            }
        }

        Map<String, Artifact> oldCoreModules = new HashMap<String, Artifact>();
        Map<String, Artifact> targetCoreModules = new HashMap<String, Artifact>();
        for (Artifact artifact: SDKConstants.getCoreModules(oldVersion, isPlatform)) {
            oldCoreModules.put(getId(artifact.getArtifactId()), artifact);
        }
        for (Artifact artifact: SDKConstants.getCoreModules(targetVersion, isPlatform)) {
            targetCoreModules.put(getId(artifact.getArtifactId()), artifact);
        }
        // also remove core war and h2 module
        for (File coreModule: serverPath.listFiles()) {
            Artifact oldMod = oldCoreModules.get(getId(coreModule.getName()));
            Artifact targetMod = targetCoreModules.get(getId(coreModule.getName()));
            if (oldMod != null) {
                if ((targetMod == null) || (!oldMod.getDestFileName().equals(targetMod.getDestFileName()))) {
                    getLog().info("ADD " + oldMod.getArtifactId());
                    list.add(coreModule);
                }
            }
        }
        return list;
    }

    /**
     * Remove selected files
     * @param files
     */
    private void removeFiles(List<File> files) {
        for (File file: files) {
            file.delete();
        }
    }
}
