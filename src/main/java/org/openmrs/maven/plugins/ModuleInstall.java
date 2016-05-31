package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal install
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
     * @required
     * @component
     */
    Wizard wizard;

    public ModuleInstall(MavenProject project, MavenSession session, BuildPluginManager manager) {
        mavenProject = project;
        mavenSession = session;
        pluginManager = manager;
    };

    public ModuleInstall() {}

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
        if (serverId == null) {
            File currentProperties = wizard.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = wizard.getServerPath(serverId);
        Artifact artifact = checkCurrentDirectoryForOpenmrsCoreVersion(serverPath);

        if(artifact != null){
            deployOpenmrsWar(serverPath, artifact);
        } else {
            deployModule(groupId, artifactId, version, serverPath);
        }
    }

    /**
     * Deploy openmrs.war file to server
     * @param serverPath
     * @param artifact
     * @return tru if success
     * @throws MojoExecutionException
     */
    private void deployOpenmrsWar(File serverPath, Artifact artifact) throws MojoExecutionException {
        Server properties = Server.loadServer(serverPath);
        List<Element> artifactItems = new ArrayList<Element>();
        artifactItems.add(artifact.toElement(serverPath.toString()));

        executeMojoPlugin(artifactItems);

        File openmrsCorePath = new File(serverPath, "openmrs-" + properties.getOpenmrsCoreVersion() + ".war");
        openmrsCorePath.delete();

        properties.setParam("openmrs.platform.version", mavenProject.getVersion());
        properties.save();
    }

    /**
     * Deploy Module to server
     * @param groupId
     * @param artifactId
     * @param version
     * @param serverPath
     * @throws MojoExecutionException
     */
    private void deployModule(String groupId, String artifactId, String version, File serverPath) throws MojoExecutionException {
        List<Element> artifactItems = new ArrayList<Element>();
        Server properties = Server.loadServer(serverPath);
        Artifact artifact = getArtifactForSelectedParameters(groupId, artifactId, version);
        artifact.setArtifactId(artifact.getArtifactId() + "-omod");
        File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
        modules.mkdirs();
        artifactItems.add(artifact.toElement(modules.getPath()));

        boolean moduleRemoved = deleteModuleFromServer(artifact, modules);

        executeMojoPlugin(artifactItems);

        String moduleId = artifact.getArtifactId();
        moduleId = moduleId.substring(0, moduleId.indexOf("-omod"));
        String[] params = {artifact.getGroupId(), moduleId, artifact.getVersion()};
        String module = StringUtils.join(params, "/");
        properties.addToValueList(Server.PROPERTY_USER_MODULES, module);
        properties.save();
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
     * @param serverPath
     * @return
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public Artifact checkCurrentDirectoryForOpenmrsCoreVersion(File serverPath) throws MojoExecutionException, MojoFailureException {
        Server serverConfig = Server.loadServer(serverPath);
        String moduleName = mavenProject.getArtifactId();
        if(moduleName.equals("openmrs")){
            if(!new Version(mavenProject.getVersion()).equals(new Version(serverConfig.getOpenmrsCoreVersion()))){
                String message = String.format("The server currently has openmrs.war in version %s. Would you like to change it to %s?", serverConfig.getOpenmrsCoreVersion(), mavenProject.getVersion());
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
}
