package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal deploy
 * @requiresProject false
 */
public class Deploy extends AbstractTask {

    private static final String DEFAULT_OK_MESSAGE = "Module '%s' installed successfully";
    private static final String DEFAULT_UPDATE_MESSAGE = "Module '%s' was updated to version '%s'";
    private static final String TEMPLATE_UPDATE = "Module is installed already. Do you want to upgrade it to version '%s'?";
    private static final String TEMPLATE_DOWNGRADE = "Installed version '%s' of module higher than target '%s'";
    private static final String TEMPLATE_CURRENT_VERSION = "The server currently has the OpenMRS %s in version %s installed.";

    private static final String INSTALL_MODULE_OPTION = "Install module";
    private static final String INSTALL_DISTRO_OPTION = "Install OpenMRS distribution";
    private static final String INSTALL_PLATFORM_OPTION = "Install OpenMRS platform";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * @parameter expression="${version}"
     */
    private String version;
    /**
     * @parameter expression="${distro}"
     */
    private String distro;

    /**
     * @parameter expression="${platform}"
     */
    private String platform;

    public Deploy() {}

    public Deploy(AbstractTask other) {super(other);}

    public Deploy(MavenProject project, MavenSession session, BuildPluginManager manager) {
        super.mavenProject = project;
        super.mavenSession = session;
        super.pluginManager = manager;
    }

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);
        /**
         * workflow:
         * -if user specified both distro and platform, ignore them and enter interactive mode
         * -if user specified distro or platform, always upgrade distro/platform
         * -if user don't specify any:
         * -- if in module project dir/ core dir, install it
         * -- if in directory containing distro-properties file, use it to upgrade
         * -- if those conditions are not met or user doesn't agree, start interactive mode
         */
        ServerUpgrader serverUpgrader = new ServerUpgrader(this);

        if(platform!=null&&distro!=null) {
            if(platform!=null&&distro!=null) getLog().error("platform and distro both specified, starting interactive mode");
            platform = null;
            distro = null;
        }
        if((platform == null && distro == null)){
            Artifact artifact = checkCurrentDirectoryForOpenmrsWebappUpdate(server);
            DistroProperties distroProperties = checkCurrentDirectoryForDistroProperties(server);
            if(artifact != null){
                deployOpenmrsWar(server, artifact);
            } else if(checkCurrentDirForModuleProject()) {
                deployModule(groupId, artifactId, version, server);
            } else if(distroProperties!=null){
                serverUpgrader.upgradeToDistro(server, distroProperties);
            } else {
                runInteractiveMode(server, serverUpgrader);
            }
        } else if(distro != null) {
            DistroProperties distroProperties = distroHelper.retrieveDistroProperties(distro);
            serverUpgrader.upgradeToDistro(server, distroProperties);
        } else {
            serverUpgrader.upgradePlatform(server, platform);
        }
    }

    private void runInteractiveMode(Server server, ServerUpgrader upgrader) throws MojoExecutionException, MojoFailureException {
        DistroProperties distroProperties;List<String> options = new ArrayList<>(Arrays.asList(
                INSTALL_MODULE_OPTION,
                INSTALL_DISTRO_OPTION,
                INSTALL_PLATFORM_OPTION));
        String choice = wizard.promptForMissingValueWithOptions("What would You like to do?%s", null, "", options, false);
        switch(choice){
            case(INSTALL_MODULE_OPTION):{
                deployModule(serverId, groupId, artifactId, version);
                break;
            }
            case(INSTALL_DISTRO_OPTION):{
                wizard.showMessage(String.format(
                        TEMPLATE_CURRENT_VERSION,
                        "Reference Application distribution",
                        server.getVersion()));

                distro = wizard.promptForDistroVersion();
                distroProperties = distroHelper.retrieveDistroProperties(distro);
                upgrader.upgradeToDistro(server, distroProperties);
                break;
            }
            case(INSTALL_PLATFORM_OPTION):{
                wizard.showMessage(String.format(
                        TEMPLATE_CURRENT_VERSION,
                        "platform",
                        server.getPlatformVersion()));
                Artifact webapp = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_WEB);
                platform = wizard.promptForPlatformVersion(versionsHelper.getVersionAdvice(webapp, 3));
                upgrader.upgradePlatform(server, platform);
                break;
            }
            default: {
                throw new MojoExecutionException(String.format("Invalid installation option only '%s', '%s', '%s' are available",
                        INSTALL_MODULE_OPTION, INSTALL_DISTRO_OPTION, INSTALL_PLATFORM_OPTION));
            }
        }
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

    public void deployModule(String serverId, String groupId, String artifactId, String version) throws MojoExecutionException, MojoFailureException {
        initTask();
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }

        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);
        deployModule(groupId, artifactId, version, server);
    }

    /**
     * Deploy openmrs.war file to server
     * @param server
     * @param artifact
     * @return tru if success
     * @throws MojoExecutionException
     */
    private void deployOpenmrsWar(Server server, Artifact artifact) throws MojoExecutionException {
        List<Element> artifactItems = new ArrayList<Element>();
        artifactItems.add(artifact.toElement(server.getServerDirectory().getPath()));

        executeMojoPlugin(artifactItems);

        File openmrsCorePath = new File(server.getServerDirectory(), "openmrs-" + server.getPlatformVersion() + ".war");
        openmrsCorePath.delete();
        server.deleteServerTmpDirectory();

        server.setPlatformVersion(mavenProject.getVersion());
        server.save();
        getLog().info("OpenMRS war has been successfully deployed");
    }

    /**
     * Deploy Module to server
     * @param groupId
     * @param artifactId
     * @param version
     * @param server
     * @throws MojoExecutionException
     */
    private void deployModule(String groupId, String artifactId, String version, Server server) throws MojoExecutionException {
        List<Element> artifactItems = new ArrayList<Element>();
        Artifact artifact = getModuleArtifactForSelectedParameters(groupId, artifactId, version);

        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        artifactItems.add(artifact.toElement(modules.getPath()));

        boolean moduleRemoved = deleteModuleFromServer(artifact, modules, server);

        executeMojoPlugin(artifactItems);

        server.saveUserModule(artifact);
        server.save();
        if (moduleRemoved) {
            getLog().info(String.format(DEFAULT_UPDATE_MESSAGE, artifact.getArtifactId(), artifact.getVersion()));
        }
        else getLog().info(String.format(DEFAULT_OK_MESSAGE, artifact.getArtifactId()));
    }

    /**
     * Install modules form artifactItems
     * @param artifactItems
     * @throws MojoExecutionException
     */
    private void executeMojoPlugin(List<Element> artifactItems) throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"), artifactItems.toArray(new Element[0]))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    /**
     * Deletes old module from the server after updating
     * @param artifact
     * @param serverModules
     * @param server
     * @return true if module has been removed
     * @throws MojoExecutionException
     */
    private boolean deleteModuleFromServer(Artifact artifact, File serverModules, Server server) throws MojoExecutionException {
        File[] listOfModules = serverModules.listFiles();
        String moduleId = StringUtils.removeEnd(artifact.getArtifactId(), "-omod");
        for (File itemModule : listOfModules) {
            String[] parts = itemModule.getName().split("-");
            String oldV = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "-");
            if (moduleId.equals(parts[0])) {
                Version oldVersion = new Version(oldV.substring(0, oldV.lastIndexOf('.')));
                Version newVersion = new Version(artifact.getVersion());
                if (oldVersion.higher(newVersion)) {
                    throw new MojoExecutionException(String.format(TEMPLATE_DOWNGRADE, oldVersion.toString(), newVersion.toString()));
                }
                else if (oldVersion.lower(newVersion)) {
                    boolean agree = wizard.promptYesNo(String.format(TEMPLATE_UPDATE, artifact.getVersion()));
                    if (!agree) {
                        return false;
                    }
                }
                server.removeUserModule(new Artifact(moduleId, oldVersion.toString(), artifact.getGroupId()));
                return itemModule.delete();
            }
        }
        return false;
    }

    /**
     * Check if the command openmrs-sdk:install was invoked from the openmrs-core directory and then check the version
     * @param server
     * @return artifact to update, if update requested or null
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public Artifact checkCurrentDirectoryForOpenmrsWebappUpdate(Server server) throws MojoExecutionException, MojoFailureException {
        String moduleName = mavenProject.getArtifactId();
        if(moduleName.equals("openmrs")){
            if(!new Version(mavenProject.getVersion()).equals(new Version(server.getPlatformVersion()))){
                String message = String.format("The server currently has openmrs.war in version %s. Would you like to change it to %s?", server.getPlatformVersion(), mavenProject.getVersion());
                boolean agree = wizard.promptYesNo(message);
                if(agree){
                    return new Artifact("openmrs-webapp", mavenProject.getVersion(), Artifact.GROUP_WEB, Artifact.TYPE_WAR);
                }
            }
        }
        return null;
    }
    private DistroProperties checkCurrentDirectoryForDistroProperties(Server server) {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromDir();
        if (distroProperties!=null) {
            String message = String.format(
                    "Would you like to update %s to %s %s specified in distro properties file in current directory?",
                    server.getServerId(),
                    distroProperties.getName(),
                    distroProperties.getServerVersion());

            boolean agree = wizard.promptYesNo(message);
            if(agree){
                return distroProperties;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get attribute values and prompt if not selected
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public Artifact getModuleArtifactForSelectedParameters(String groupId, String artifactId, String version) throws MojoExecutionException {
        String moduleGroupId = null;
        String moduleArtifactId = null;
        String moduleVersion = null;
        
        File userDir = new File(System.getProperty("user.dir"));
        Project project = null;
        if (Project.hasProject(userDir)) {
            project = Project.loadProject(userDir);
        }
        if (artifactId == null && project != null && project.isOpenmrsModule()) {
            if (project.getParent() != null) {
                moduleGroupId = project.getParent().getGroupId();
                moduleArtifactId = project.getParent().getArtifactId();
                moduleVersion = (version != null) ? version : project.getParent().getVersion();

            } else {
                moduleGroupId = project.getGroupId();
                moduleArtifactId = project.getArtifactId();
                moduleVersion = (version != null) ? version : project.getVersion();
            }
        } else {
            moduleGroupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", Artifact.GROUP_MODULE);
            moduleArtifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
            if(!moduleArtifactId.endsWith("-omod")){
                moduleArtifactId += "-omod";
            }
            List<String> availableVersions = versionsHelper.getVersionAdvice(new Artifact(moduleArtifactId, "1.0",moduleGroupId), 5);
            moduleVersion = wizard.promptForMissingValueWithOptions(
                    "You can install following versions of module", version, "version", availableVersions, true);
        }
        return new Artifact(moduleArtifactId, moduleVersion, moduleGroupId, Artifact.TYPE_JAR, Artifact.TYPE_OMOD);
    }

    public boolean checkCurrentDirForModuleProject() throws MojoExecutionException {
        File dir = new File(System.getProperty("user.dir"));
        Project project = null;
        if (Project.hasProject(dir)) {
            project = Project.loadProject(dir);
        }
        boolean hasProject = (project != null && project.isOpenmrsModule());
        if(hasProject){
            hasProject = wizard.promptYesNo(String.format("Would you like to install %s %s from this directory?",
                    project.getArtifactId(), project.getVersion()));
        }
        return hasProject;
    }
}
