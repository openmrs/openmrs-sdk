package org.openmrs.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.Wizard;
import org.openmrs.maven.plugins.utility.DefaultWizard;
import org.openmrs.maven.plugins.utility.ServerConfig;
import org.openmrs.maven.plugins.utility.SDKConstants;

/**
 *
 * @goal upgrade-platform
 * @requiresProject false
 *
 */
public class UpgradePlatform extends AbstractMojo{
    private static final String TEMPLATE_INVALID_VERSION = "Cannot upgrade to version '%s'";
    private static final String TEMPLATE_INVALID_PROPERTIES = "Server '%s' has invalid properties";
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
    public void upgradeServer(String serverId, final String version, boolean isUpdateToPlatform) throws MojoExecutionException, MojoFailureException {
        Wizard helper = new DefaultWizard(prompter);
        String resultVersion = null;
        String resultServer = null;
        // check if we are inside the some server folder
        if (serverId == null) {
            File currentProperties = helper.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        resultVersion = helper.promptForValueIfMissing(version, "version");

        File serverPath = helper.getServerPath(serverId);
        resultServer = serverPath.getName();
        ServerConfig properties = ServerConfig.loadServerConfig(serverPath);
        String webapp = properties.getParam(SDKConstants.PROPERTY_VERSION);
        String platform = properties.getParam(SDKConstants.PROPERTY_PLATFORM);

        // invalid old version
        if (platform == null) {
            throw new MojoExecutionException(String.format(TEMPLATE_INVALID_PROPERTIES, resultServer));
        }
        Version platformVersion = new Version(platform);
        Version nextVersion = new Version(resultVersion);
        SetupPlatform setupPlatform = new SetupPlatform(mavenProject, mavenSession, prompter, pluginManager);
        // get list modules to remove after
        // for 2.3 and higher, copy dependencies to tmp folder
        File tmpFolder = new File(serverPath, SDKConstants.TMP);
        boolean isPriorVersion = new Version(resultVersion).higher(new Version(Version.PRIOR));
        Version webAppVersionFromTmpFolder = new Version("1");
        if (isPriorVersion) {
            webAppVersionFromTmpFolder = setupPlatform.extractDistroToServer(resultVersion, tmpFolder);
        }
        if (isUpdateToPlatform) {
            if (webapp != null) {
                throw new MojoExecutionException(String.format(TEMPLATE_NONPLATFORM, resultServer));
            }
            else if (platformVersion.higher(nextVersion)) {
                throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
            }
        }
        else {
            String targetWebApp = isPriorVersion ? webAppVersionFromTmpFolder.toString() : SDKConstants.WEBAPP_VERSIONS.get(resultVersion);
            // note, that in future we also must detect web app version also for 2.3 and higher
            if (targetWebApp == null) {
                throw new MojoExecutionException(String.format(TEMPLATE_INVALID_VERSION, resultVersion));
            }
            else {
                if (webapp == null) {
                    if (nextVersion.higher(platformVersion)) {
                        boolean yes = helper.dialogYesNo("Do you want to upgrade platform server?");
                        if (!yes) {
                            return;
                        }
                    }
                    else {
                        throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
                    }
                }
                else if (new Version(webapp).higher(new Version(targetWebApp)) || (platformVersion.higher(nextVersion))) {
                    throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, resultServer));
                }
            }
        }
        // flag if old server is platform server
        boolean isOldPlatformServer = webapp == null;
        // calculate diff
        List<File> listFilesToRemove = getFileListToRemove(serverPath, platform, resultVersion, isOldPlatformServer);
        // for 2.3 and higher, move data from tmp to server
        if (isPriorVersion) {
            try {
                for (File f: tmpFolder.listFiles()) {
                    if (f.isFile()) {
                        File t = new File(serverPath, f.getName());
                        if (!t.exists()) FileUtils.moveFile(f, t);
                    }
                }
                for (File f: new File(tmpFolder, SDKConstants.OPENMRS_SERVER_MODULES).listFiles()) {
                    if (f.isFile()) {
                        File t = new File(new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES), f.getName());
                        if (!t.exists()) FileUtils.moveFile(f, t);
                    }
                }
                FileUtils.deleteDirectory(tmpFolder);
            } catch (IOException e) {
                throw new MojoExecutionException("Error during resolving dependencies: " + e.getMessage());
            }
            // install user modules
            String values = properties.getParam(SDKConstants.PROPERTY_USER_MODULES);
            if (values != null) {
                ModuleInstall installer = new ModuleInstall(mavenProject, mavenSession, pluginManager, prompter);
                String[] modules = values.split(ServerConfig.COMMA);
                for (String mod: modules) {
                    String[] params = mod.split(ServerConfig.SLASH);
                    // check
                    if (params.length == 3) {
                        installer.installModule(resultServer, params[0], params[1], params[2]);
                    }
                    else throw new MojoExecutionException("Properties file parse error - cannot read user modules list");
                }
            }
        }
        // also make a copy of old properties file
        File tempProperties = new File(serverPath, TEMP_PROPERTIES);
        properties.saveTo(tempProperties);
        // also remove copy of old properties file if success
        listFilesToRemove.add(tempProperties);
        boolean addDemoData = (String.valueOf(true).equals(properties.getParam(SDKConstants.PROPERTY_DEMO_DATA)));
        Server server = new Server.ServerBuilder()
                .setServerId(resultServer)
                .setVersion(resultVersion)
                .setDbDriver(properties.getParam(SDKConstants.PROPERTY_DB_DRIVER))
                .setDbUri(properties.getParam(SDKConstants.PROPERTY_DB_URI))
                .setDbUser(properties.getParam(SDKConstants.PROPERTY_DB_USER))
                .setDbPassword(properties.getParam(SDKConstants.PROPERTY_DB_PASS))
                .setInteractiveMode("false")
                .setDemoData(addDemoData)
                .build();
        properties.delete();
        setupPlatform.setup(server, isUpdateToPlatform, false);
        removeFiles(listFilesToRemove);
        // remove temp files after upgrade
        File tmp = new File(serverPath, "${project.basedir}");
        if (tmp.exists()) try {
            FileUtils.deleteDirectory(tmp);
        } catch (IOException e) {
            throw new MojoExecutionException("Error during clean: " + e.getMessage());
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
        boolean isPriorOld = new Version(oldVersion).higher(new Version(Version.PRIOR));
        boolean isPriorNew = new Version(targetVersion).higher(new Version(Version.PRIOR));
        boolean oldPlatformVersion = SDKConstants.WEBAPP_VERSIONS.get(oldVersion) == null;
        File tmpModules = new File(new File(serverPath, SDKConstants.TMP), SDKConstants.OPENMRS_SERVER_MODULES);
        // remove modules first
        if (!isPlatform) {
            File modulePath = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
            if (modulePath.exists() && !oldPlatformVersion) {
                Map<String, Artifact> oldModules = new HashMap<String, Artifact>();
                Map<String, Artifact> targetModules = new HashMap<String, Artifact>();
                // logic for 2.2 and later
                List<Artifact> artifactList = isPriorOld ? getArtifactsFromFolder(modulePath) : SDKConstants.ARTIFACTS.get(oldVersion);
                List<Artifact> targerArtifactList = isPriorNew ? getArtifactsFromFolder(tmpModules) : SDKConstants.ARTIFACTS.get(targetVersion);
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
        List<Artifact> oldModules = isPriorOld ? getArtifactsFromFolder(serverPath) : SDKConstants.getCoreModules(oldVersion, isPlatform);
        for (Artifact artifact: oldModules) {
            oldCoreModules.put(getId(artifact.getArtifactId()), artifact);
        }
        List<Artifact> newModules = isPriorNew ? getArtifactsFromFolder(tmpModules.getParentFile()) : SDKConstants.getCoreModules(targetVersion, isPlatform);
        for (Artifact artifact: newModules) {
            targetCoreModules.put(getId(artifact.getArtifactId()), artifact);
        }
        // also remove core war and h2 module
        for (File coreModule: serverPath.listFiles()) {
            Artifact oldMod = oldCoreModules.get(getId(coreModule.getName()));
            Artifact targetMod = targetCoreModules.get(getId(coreModule.getName()));
            if (oldMod != null) {
                if ((targetMod == null) || (!oldMod.getDestFileName().equals(targetMod.getDestFileName()))) {
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

    /**
     * Convert file names to Artifacts
     * @param folder
     * @return
     */
    private List<Artifact> getArtifactsFromFolder(File folder) {
        final String[] supported = {Artifact.TYPE_WAR, Artifact.TYPE_JAR, Artifact.TYPE_OMOD};
        List<Artifact> artifacts = new ArrayList<Artifact>();
        File[] files = (folder.listFiles() == null) ? new File[0] : folder.listFiles();
        for (File f: files) {
            String type = f.getName().substring(f.getName().lastIndexOf(".") + 1);
            if (type.equals(supported[0]) || type.equals(supported[1]) || type.equals(supported[2])) {
                String id = getId(f.getName());
                String version = Version.parseVersionFromFile(f.getName());
                Artifact a = new Artifact(id, version);
                a.setDestFileName(f.getName());
                artifacts.add(a);
            }
        }
        return artifacts;
    }
}
