package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.bintray.BintrayId;
import org.openmrs.maven.plugins.bintray.BintrayPackage;
import org.openmrs.maven.plugins.bintray.OpenmrsBintray;
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

    private static final String DEFAULT_ABORT_MESSAGE = "Deploying module '%s' aborted";
    private static final String DEFAULT_UPDATE_MESSAGE = "Module '%s' was updated to version '%s'";
    private static final String TEMPLATE_UPDATE = "Do you want to update module '%s' in version '%s' to version '%s'?";
    private static final String TEMPLATE_DOWNGRADE = "Please note that downgrades are not recommended";
    private static final String TEMPLATE_CURRENT_VERSION = "The server currently has the OpenMRS %s in version %s installed.";

    private static final String DEPLOY_MODULE_OPTION = "Module";
    private static final String DEPLOY_OWA_OPTION = "Open Web App";
    private static final String DEPLOY_DISTRO_OPTION = "Distribution";
    private static final String DEPLOY_PLATFORM_OPTION = "Platform";

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

    /**
     * @parameter expression="${owa}"
     */
    private String owa;

    public Deploy() {}

    public Deploy(AbstractTask other) {super(other);}

    public Deploy(MavenProject project, MavenSession session, BuildPluginManager manager) {
        super.mavenProject = project;
        super.mavenSession = session;
        super.pluginManager = manager;
    }

    public void executeTask() throws MojoExecutionException, MojoFailureException {
        if (serverId == null) {
            File currentProperties = Server.checkCurrentDirForServer();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = loadValidatedServer(serverId);
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

        if((platform == null && distro == null && owa == null) && artifactId == null){
            Artifact artifact = checkCurrentDirectoryForOpenmrsWebappUpdate(server);
            DistroProperties distroProperties = checkCurrentDirectoryForDistroProperties(server);
            if(artifact != null){
                deployOpenmrsFromDir(server, artifact);
            } else if(distroProperties!=null){
                serverUpgrader.upgradeToDistro(server, distroProperties);
            } else if(checkCurrentDirForModuleProject()) {
                deployModule(groupId, artifactId, version, server);
            } else {
                runInteractiveMode(server, serverUpgrader);
            }
        } else if(distro != null) {
            DistroProperties distroProperties = distroHelper.retrieveDistroProperties(distro);
            serverUpgrader.upgradeToDistro(server, distroProperties);
        } else if(platform != null) {
            deployOpenmrs(server, platform);
        } else if(owa != null){
            BintrayId id = OpenmrsBintray.parseOwa(owa);
            deployOwa(server, id.getName(), id.getVersion());
        } else if(artifactId != null){
            deployModule(serverId, groupId, artifactId, version);
        } else throw new MojoExecutionException("Invalid installation option");
    }

    private void runInteractiveMode(Server server, ServerUpgrader upgrader) throws MojoExecutionException, MojoFailureException {
        DistroProperties distroProperties;
        List<String> options = new ArrayList<>(Arrays.asList(
                DEPLOY_MODULE_OPTION,
                DEPLOY_OWA_OPTION,
                DEPLOY_DISTRO_OPTION,
                DEPLOY_PLATFORM_OPTION
                ));
        String choice = wizard.promptForMissingValueWithOptions("What would you like to deploy?%s", null, "", options);

        switch(choice){
            case(DEPLOY_MODULE_OPTION):{
                deployModule(serverId, groupId, artifactId, version);
                break;
            }
            case(DEPLOY_DISTRO_OPTION):{
                wizard.showMessage(String.format(
                        TEMPLATE_CURRENT_VERSION,
                        server.getName(),
                        server.getVersion()));

                distro = wizard.promptForRefAppVersion(versionsHelper);
                distroProperties = distroHelper.retrieveDistroProperties(distro);
                upgrader.upgradeToDistro(server, distroProperties);
                break;
            }
            case(DEPLOY_PLATFORM_OPTION):{
                wizard.showMessage(String.format(
                        TEMPLATE_CURRENT_VERSION,
                        "platform",
                        server.getPlatformVersion()));
                Artifact webapp = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID, SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION, Artifact.GROUP_DISTRO);
                platform = wizard.promptForPlatformVersion(versionsHelper.getVersionAdvice(webapp, 3));
                deployOpenmrs(server, platform);
                break;
            }
            case(DEPLOY_OWA_OPTION):{
                deployOwa(server, null, null);
                break;
            }
            default: {
                throw new MojoExecutionException(String.format("Invalid installation option only '%s', '%s', '%s' are available",
                        DEPLOY_MODULE_OPTION, DEPLOY_DISTRO_OPTION, DEPLOY_PLATFORM_OPTION));
            }
        }
    }

    private void deployOwa(Server server, String name, String version) throws MojoExecutionException {
        OpenmrsBintray bintray = new OpenmrsBintray();
        if(name == null){
            List<String> owas = new ArrayList<>();
            for(BintrayId id : bintray.getAvailableOWA()){
                owas.add(id.getName());
            }
            name = wizard.promptForMissingValueWithOptions("Which OWA would you like to deploy?%s", name, "", owas, "Please specify OWA id", null);
        }
        if(version == null){
            BintrayPackage owaMetadata = bintray.getOwaMetadata(name);
            if(owaMetadata == null){
                throw new RuntimeException("there is no package with given name");
            }
            List<String> versions = owaMetadata.getVersions();
            version = wizard.promptForMissingValueWithOptions("Which version would you like to deploy?%s", version, "", versions, "Please specify OWA version", null);
        }

        boolean installOwaModule = true;
        List<Artifact> serverModules = server.getServerModules();
        Artifact owaModule = new Artifact("owa-omod", "");
        for(Artifact module : serverModules){
            if(owaModule.getArtifactId().equals(module.getArtifactId())){
                installOwaModule = false;
                break;
            }
        }
        if(installOwaModule){
            wizard.showMessage("No installation of OWA module found on this server, will install latest version");
            owaModule.setVersion(versionsHelper.getLatestReleasedVersion(owaModule));
            deployModule(
                    owaModule.getGroupId(),
                    owaModule.getArtifactId(),
                    owaModule.getVersion(),
                    server
            );
        }

        File owaDir = new File(server.getServerDirectory(), "owa");
        if(!owaDir.exists()){
            //OWA module has option to set custom app folder
            boolean useDefaultDir = wizard.promptYesNo(String.format(
                    "\nThere is no default directory '%s' on server %s, would you like to create it? (if not, you will be asked for path to custom directory)",
                    Server.OWA_DIRECTORY,
                    server.getServerId()));
            if(useDefaultDir){
                owaDir.mkdir();
            } else {
                String path = wizard.promptForValueIfMissing(null, "owa directory path");
                owaDir = new File(path);
            }
        }
        bintray.downloadOWA(owaDir, name, version);
        server.saveUserOWA(new BintrayId(name, version));
        server.save();
        getLog().info(String.format("OWA %s %s was successfully deployed on server %s", name, version, server.getServerId()));
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
        if (serverId == null) {
            File currentProperties = Server.checkCurrentDirForServer();
            if (currentProperties != null) serverId = currentProperties.getName();
        }

        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = loadValidatedServer(serverId);
        groupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", Artifact.GROUP_MODULE);
        artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
        deployModule(groupId, artifactId, version, server);
    }

    public void deployOpenmrs(Server server, String version) throws MojoFailureException, MojoExecutionException {
        Artifact artifact = new Artifact(SDKConstants.PLATFORM_ARTIFACT_ID, version, Artifact.GROUP_DISTRO);
        try {
            deployOpenmrsPlatform(server, artifact);
        } catch (MojoExecutionException e) {
            ServerUpgrader serverUpgrader = new ServerUpgrader(this);
            serverUpgrader.upgradePlatform(server, platform);
        }
    }
    
    public void deployOpenmrsFromDir(Server server, Artifact artifact) throws MojoExecutionException, MojoFailureException {
        String artifactId = artifact.getArtifactId();
        if(artifactId.equals("openmrs-webapp")){
            deployOpenmrsWar(server, artifact);
        } else if(artifactId.equals("openmrs-distro-platform")){
            artifact.setArtifactId("platform");
            deployOpenmrsPlatform(server, artifact);
        }
    }

    private void deployOpenmrsPlatform(Server server, Artifact artifact) throws MojoExecutionException, MojoFailureException {
        DistroProperties platformDistroProperties = distroHelper.downloadDistroProperties(server.getServerDirectory(), artifact);
        DistroProperties serverDistroProperties = server.getDistroProperties();
        serverDistroProperties.setArtifacts(platformDistroProperties);
        serverDistroProperties.saveTo(server.getServerDirectory());
        ServerUpgrader serverUpgrader = new ServerUpgrader(this);
        serverUpgrader.upgradeToDistro(server, serverDistroProperties);
    }

    /**
     * Deploy openmrs.war file to server
     * @param server
     * @param artifact
     * @return tru if success
     * @throws MojoExecutionException
     */
    public void deployOpenmrsWar(Server server, Artifact artifact) throws MojoExecutionException {
        File openmrsCorePath = new File(server.getServerDirectory(), "openmrs-" + server.getPlatformVersion() + ".war");
        openmrsCorePath.delete();
        server.deleteServerTmpDirectory();

        List<Element> artifactItems = new ArrayList<Element>();
        artifactItems.add(artifact.toElement(server.getServerDirectory().getPath()));

        executeMojoPlugin(artifactItems);

        server.setPlatformVersion(mavenProject.getVersion());
        server.saveAndSynchronizeDistro();
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
    public void deployModule(String groupId, String artifactId, String version, Server server) throws MojoExecutionException {
        List<Element> artifactItems = new ArrayList<Element>();
        Artifact artifact = getModuleArtifactForSelectedParameters(groupId, artifactId, version);

        File modules = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        artifactItems.add(artifact.toElement(modules.getPath()));

        boolean moduleRemoved = deleteModuleFromServer(artifact, modules, server);

        if (moduleRemoved) {
            executeMojoPlugin(artifactItems);

            server.setModuleProperties(artifact);
            server.saveAndSynchronizeDistro();
            getLog().info(String.format(DEFAULT_UPDATE_MESSAGE, artifact.getArtifactId(), artifact.getVersion()));
        }
        else getLog().info(String.format(DEFAULT_ABORT_MESSAGE, artifact.getArtifactId()));
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
     * @return true if module has been removed or module does not exist
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

                if (!oldVersion.equals(newVersion)) {
                    if (oldVersion.higher(newVersion)) {
                        wizard.showMessage(String.format(TEMPLATE_DOWNGRADE, oldVersion.toString(), newVersion.toString()));
                    }

                    boolean agree = wizard.promptYesNo(String.format(TEMPLATE_UPDATE, moduleId, oldVersion, artifact.getVersion()));
                    if (!agree) {
                        return false;
                    }
                }

                server.removeModuleProperties(new Artifact(moduleId, oldVersion.toString(), artifact.getGroupId()));
                server.saveAndSynchronizeDistro();
                return itemModule.delete();
            }
        }
        return true;
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
            if(!new Version(mavenProject.getVersion()).equals(new Version(server.getPlatformVersion())) || new Version(mavenProject.getVersion()).isSnapshot()){
                String message = String.format("The server currently has openmrs.war in version %s. Would you like to update it to %s from the current directory?", server.getPlatformVersion(), mavenProject.getVersion());
                boolean agree = wizard.promptYesNo(message);
                if(agree){
                    return new Artifact("openmrs-webapp", mavenProject.getVersion(), Artifact.GROUP_WEB, Artifact.TYPE_WAR);
                }
            }
        } else if("platform".equals(moduleName)){
            if(!new Version(mavenProject.getVersion()).equals(new Version(server.getPlatformVersion())) || new Version(mavenProject.getVersion()).isSnapshot()){
                String message = String.format("The server currently has openmrs platform in version %s. Would you like to update it to %s from the current directory?", server.getPlatformVersion(), mavenProject.getVersion());
                boolean agree = wizard.promptYesNo(message);
                if(agree){
                    return new Artifact("openmrs-distro-platform", mavenProject.getVersion(), Artifact.GROUP_DISTRO);
                }
            }
        }
        return null;
    }
    private DistroProperties checkCurrentDirectoryForDistroProperties(Server server) {
        DistroProperties distroProperties = DistroHelper.getDistroPropertiesFromDir();
        if (distroProperties!=null) {
            String message = String.format(
                    "Would you like to deploy %s %s from the current directory?",
                    distroProperties.getName(),
                    distroProperties.getVersion());

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
                moduleArtifactId = project.getParent().getArtifactId() + "-omod";
                moduleVersion = (version != null) ? version : project.getParent().getVersion();

            } else {
                moduleGroupId = project.getGroupId();
                moduleArtifactId = project.getArtifactId() + "-omod";
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
                    "You can deploy the following versions of the module", version, "version", availableVersions, "Please specify module version", null);
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
            hasProject = wizard.promptYesNo(String.format("Would you like to deploy %s %s from the current directory?",
                    project.getArtifactId(), project.getVersion()));
        }
        return hasProject;
    }
}
