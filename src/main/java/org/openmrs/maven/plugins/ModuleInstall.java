package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal install
 * @requiresProject false
 */
public class ModuleInstall extends AbstractTask {

    private static final String DEFAULT_OK_MESSAGE = "Module '%s' installed successfully";
    private static final String DEFAULT_UPDATE_MESSAGE = "Module '%s' was updated to version '%s'";
    private static final String TEMPLATE_UPDATE = "Module is installed already. Do you want to upgrade it to version '%s'?";
    private static final String TEMPLATE_DOWNGRADE = "Installed version '%s' of module higher than target '%s'";
    private static final String TEMPLATE_CURRENT_VERSION = "The server currently has the OpenMRS %s in version %s installed.";

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
     * @parameter expression="${distro}"
     */
    private String distro;

    /**
     * @parameter expression="${platform}"
     */
    private String platform;

    public ModuleInstall() {}

    public ModuleInstall(AbstractTask other) {super(other);}

    public ModuleInstall(MavenProject project, MavenSession session, BuildPluginManager manager) {
        super.mavenProject = project;
        super.mavenSession = session;
        super.pluginManager = manager;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        initUtilities();
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);
        /**
         * asssumptions:
         * -if user specified both distro and platform, clean it and enter interactive mode
         * -if user specified distro or platform, always upgrade distro/platform
         * -if user don't specify any:
         * -- if in module project dir/ core dir, install it
         * -- if not, start interactive mode for upgrading platform/distro
         */
        Upgrader upgrader = new Upgrader(this);
        if(platform!=null&&distro!=null) {
            if(platform!=null&&distro!=null) getLog().error("platform and distro both specified, starting interactive mode");
            platform = null;
            distro = null;
        }
        if((platform == null && distro == null)){
            Artifact artifact = checkCurrentDirectoryForOpenmrsCoreVersion(server);
            if(artifact != null){
                deployOpenmrsWar(server, artifact);
            } else if(checkCurrentDirForModuleProject()) {
                deployModule(groupId, artifactId, version, server);
            } else {
                boolean installDistro = wizard.promptForInstallDistro();
                if(installDistro){
                    wizard.showMessage(String.format(
                            TEMPLATE_CURRENT_VERSION,
                            "Reference Application distribution",
                            server.getVersion()));

                    distro = wizard.promptForDistroVersion();
                    upgrader.upgradeDistro(server, distro);
                } else {
                    wizard.showMessage(String.format(
                            TEMPLATE_CURRENT_VERSION,
                            "platform",
                            server.getPlatformVersion()));
                    Artifact webapp = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_WEB);
                    platform = wizard.promptForPlatformVersion(versionsHelper.getVersionAdvice(webapp, 3));
                    upgrader.upgradePlatform(server, platform);
                }
            }
        } else if(distro != null) {
            upgrader.upgradeDistro(server, distro);
        } else {
            upgrader.upgradePlatform(server, platform);
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
    public void installModule(String serverId, String groupId, String artifactId, String version) throws MojoExecutionException, MojoFailureException {
        initUtilities();
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }

        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = Server.loadServer(serverId);
        Artifact artifact = checkCurrentDirectoryForOpenmrsCoreVersion(server);

        if(artifact != null){
            deployOpenmrsWar(server, artifact);
        } else {
            deployModule(groupId, artifactId, version, server);
        }
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

        File openmrsCorePath = new File(server.getServerDirectory(), "openmrs-" + server.getOpenmrsCoreVersion() + ".war");
        openmrsCorePath.delete();

        if(server.getVersion()!=null){
            server.setVersion(mavenProject.getVersion());
        } else {
            server.setPlatformVersion(mavenProject.getVersion());
        }
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
        Artifact artifact = getArtifactForSelectedParameters(groupId, artifactId, version);

        artifact.setArtifactId(artifact.getArtifactId() + "-omod");
        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        artifactItems.add(artifact.toElement(modules.getPath()));

        boolean moduleRemoved = deleteModuleFromServer(artifact, modules);

        executeMojoPlugin(artifactItems);

        String moduleId = artifact.getArtifactId();
        moduleId = moduleId.substring(0, moduleId.indexOf("-omod"));
        String[] params = {artifact.getGroupId(), moduleId, artifact.getVersion()};
        String module = StringUtils.join(params, "/");
        server.addToValueList(Server.PROPERTY_USER_MODULES, module);
        server.save();
        if (moduleRemoved) {
            getLog().info(String.format(DEFAULT_UPDATE_MESSAGE, moduleId, artifact.getVersion()));
        }
        else getLog().info(String.format(DEFAULT_OK_MESSAGE, moduleId));
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
     * @return true if module has been removed
     * @throws MojoExecutionException
     */
    private boolean deleteModuleFromServer(Artifact artifact, File serverModules) throws MojoExecutionException {
        File[] listOfModules = serverModules.listFiles();
        String moduleId = artifact.getArtifactId();
        moduleId = moduleId.substring(0, moduleId.indexOf("-omod"));
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
                return itemModule.delete();
            }
        }
        return false;
    }

    /**
     * Check if the command openmrs-sdk:install was invoked from the openmrs-core directory and then check the version
     * @param server
     * @return
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public Artifact checkCurrentDirectoryForOpenmrsCoreVersion(Server server) throws MojoExecutionException, MojoFailureException {
        String moduleName = mavenProject.getArtifactId();
        if(moduleName.equals("openmrs")){
            if(!new Version(mavenProject.getVersion()).equals(new Version(server.getOpenmrsCoreVersion()))){
                String message = String.format("The server currently has openmrs.war in version %s. Would you like to change it to %s?", server.getOpenmrsCoreVersion(), mavenProject.getVersion());
                boolean agree = wizard.promptYesNo(message);
                if(agree){
                    return new Artifact("openmrs-webapp", mavenProject.getVersion(), Artifact.GROUP_WEB, Artifact.TYPE_WAR);
                }
            }
        }
        return null;
    }

    /**
     * Get attribute values and prompt if not selected
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public Artifact getArtifactForSelectedParameters(String groupId, String artifactId, String version) throws MojoExecutionException {
        String moduleGroupId = null;
        String moduleArtifactId = null;
        String moduleVersion = null;
        
        File userDir = new File(System.getProperty("user.dir"));
        Project project = null;
        if (Project.hasProject(userDir)) {
        	project = Project.loadProject(userDir);
        }
        
        if (artifactId == null && project.isOpenmrsModule()) {
            if (project.getParent() != null) {
                moduleGroupId = project.getParent().getGroupId();
                moduleArtifactId = project.getParent().getArtifactId();
                moduleVersion = (version != null) ? version : project.getParent().getVersion();
            }
            else {
                moduleGroupId = project.getGroupId();
                moduleArtifactId = project.getArtifactId();
                moduleVersion = (version != null) ? version: project.getVersion();
            }
        }
        else {
            moduleGroupId = groupId;
            moduleArtifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
            moduleVersion = wizard.promptForValueIfMissing(version, "version");
        }
        return new Artifact(moduleArtifactId, moduleVersion, moduleGroupId);
    }

    public boolean checkCurrentDirForModuleProject() throws MojoExecutionException {
        File dir = new File(System.getProperty("user.dir"));
        Project project = null;
        if (Project.hasProject(dir)) {
            project = Project.loadProject(dir);
        }
        return (project != null && project.isOpenmrsModule());
    }
}
