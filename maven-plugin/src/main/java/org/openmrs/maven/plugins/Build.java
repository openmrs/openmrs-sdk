package org.openmrs.maven.plugins;


import com.github.zafarkhaja.semver.UnexpectedCharacterException;
import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @goal build
 * @requiresProject false
 */
public class Build extends AbstractTask {

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${buildOwa}" default-value="true"
     */
    private boolean buildOwa;

    /**
     * @parameter expression="${npmVersion}"
     */
    protected String npmVersion;

    /**
     * @parameter expression="${nodeVersion}"
     */
    protected String nodeVersion;

    protected OwaHelper owaHelper;

    public Build(){}

    public Build(AbstractTask other) { super(other); }

    public Build(AbstractTask other, String serverId){
        super(other);
        this.serverId = serverId;
    }

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        this.owaHelper = new OwaHelper(mavenSession, mavenProject, pluginManager, wizard);
        boolean projectDetected = false;
        boolean buildExecuted = false;

        //if user specified serverId, omit checking directory for projects
        if(StringUtils.isBlank(serverId)){
            //check if there's owa project in current dir
            File configFile = new File("webpack.config.js");
            if (configFile.exists() && buildOwa) {
                projectDetected = true;
                buildOwaProject();
                buildExecuted = true;
            }
            //check if there's maven project in current dir
            File userDir = new File(System.getProperty("user.dir"));
            if(Project.hasProject(userDir)) {
                Project config = Project.loadProject(userDir);
                String artifactId = config.getArtifactId();
                String groupId = config.getGroupId();
                String version = config.getVersion();
                if ((artifactId != null) && (groupId != null) && version != null) {
                    projectDetected = true;
                    boolean buildMavenProject = wizard.promptYesNo(String.format(
                            "Maven artifact %s:%s:%s detected in this directory, would you like to build it?",
                            groupId, artifactId, version)
                    );
                    if(buildMavenProject){
                        try {
                            cleanInstallServerProject(userDir);
                            buildExecuted = true;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to build project");
                        }
                    }
                }
            }
        }

        //no project found, start default workflow
        if(!projectDetected){
            buildWatchedProjects();
        }
        //found owa or maven project, but didn't build
        else if(!buildExecuted) {
            boolean buildWatched = wizard.promptYesNo("Do you want to build all watched projects instead?");
            if(buildWatched){
                buildWatchedProjects();
            } else {
                wizard.showMessage("Task aborted");
            }
        }
        //otherwise just finish
    }

    private void buildWatchedProjects() throws MojoExecutionException, MojoFailureException {
        serverId = wizard.promptForExistingServerIdIfMissing(serverId);
        Server server = loadValidatedServer(serverId);

        if (server.getWatchedProjects().isEmpty()) {
            wizard.showMessage("There are no watched projects for " + serverId + " server.");
            return;
        }

        File tempFolder = createTempReactorProject(server);
        try {
            buildCoreIfWatched(server);
            cleanInstallServerProject(tempFolder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build project in "+tempFolder.getAbsolutePath(), e);
        } finally {
            deleteTempReactorProject(tempFolder);
        }

        try {
            deployWatchedProjects(server);
        } catch (MavenInvocationException e) {
            throw new MojoFailureException("Failed to deploy watched modules", e);
        }
    }

    private boolean buildCoreIfWatched(Server server) throws MojoFailureException {
        for (Project project : server.getWatchedProjects()) {
            if(project.isOpenmrsCore()){
                cleanInstallServerProject(new File(project.getPath()));
                return true;
            }
        }
        return false;
    }

    protected void buildOwaProject() throws MojoExecutionException {
        wizard.showMessage("Building OWA project...");

        boolean isUsingSystemNpmAndNodejs;

        String systemNpmVersionString = owaHelper.getSystemNpmVersion();
        String systemNodeVersionString = owaHelper.getSystemNodeVersion();
        Version systemNpmVersion = systemNpmVersionString != null ? Version.valueOf(systemNpmVersionString) : null;
        Version systemNodeVersion = systemNodeVersionString != null ? Version.valueOf(systemNodeVersionString) : null;

        Version batchNpmVersion = npmVersion != null ? Version.valueOf(npmVersion) : null;
        Version batchNodeVersion = nodeVersion != null ? Version.valueOf(nodeVersion.replaceFirst("v","")) : null;

        if (checkIfVersionsAreDefined(batchNpmVersion, batchNodeVersion)) {
            wizard.showMessage("Using npm and nodejs versions defined in batch mode");
            if (!(batchNpmVersion.equals(systemNpmVersion) && batchNodeVersion.equals(systemNodeVersion))) {
                isUsingSystemNpmAndNodejs = false;
                owaHelper.installLocalNodeAndNpm(batchNodeVersion.toString(), batchNpmVersion.toString());
            }
            else {
                isUsingSystemNpmAndNodejs = true;
            }
        }
        else {
            String projectNpmVersionString = owaHelper.getProjectNpmVersionFromPackageJson();
            String projectNodeVersionString = owaHelper.getProjectNodeVersionFromPackageJson();

            Version projectNpmVersion;
            Version projectNodeVersion;

            try {
                projectNpmVersion = projectNpmVersionString != null ? Version.valueOf(projectNpmVersionString) : null;
                projectNodeVersion = projectNodeVersionString != null ? Version.valueOf(projectNodeVersionString) : null;
            }
            catch (UnexpectedCharacterException e) {
                Map<String, Version> mostSatisfyingVersions = owaHelper.getMostSatisfyingNodeAndNpmVersion(projectNpmVersionString, projectNodeVersionString);
                projectNpmVersion = mostSatisfyingVersions.get("npm");
                projectNodeVersion = mostSatisfyingVersions.get("node");
            }

            if (!checkIfVersionsAreDefined(systemNpmVersion, systemNodeVersion)) {
                isUsingSystemNpmAndNodejs = false;

                if (checkIfVersionsAreDefined(projectNpmVersion, projectNodeVersion)) {
                    wizard.showMessage("Using npm and nodejs versions defined in package.json");
                    owaHelper.installLocalNodeAndNpm(projectNodeVersion.toString(), projectNpmVersion.toString());
                }
                else {
                    wizard.showMessage("Using default npm and nodejs versions");
                    owaHelper.installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
                }
            }
            else {
                if (checkIfVersionsAreDefined(projectNpmVersion, projectNodeVersion)) {
                    if (systemNpmVersion.satisfies(projectNpmVersionString) && systemNodeVersion.satisfies(projectNodeVersionString)) {
                        isUsingSystemNpmAndNodejs = true;
                        wizard.showMessage("Using system npm and nodejs");
                        wizard.showMessage("-npm: " + systemNpmVersion
                                + "\n" + "-nodejs: " + systemNodeVersion + "\n");
                    }
                    else {
                        isUsingSystemNpmAndNodejs = false;
                        wizard.showMessage("Using npm and nodejs versions defined in package.json");
                        owaHelper.installLocalNodeAndNpm(projectNodeVersion.toString(), projectNpmVersion.toString());
                    }
                }
                else {
                    isUsingSystemNpmAndNodejs = true;
                    wizard.showMessage("Using system npm and nodejs");
                    wizard.showMessage("-npm: " + systemNpmVersion
                            + "\n" + "-nodejs: " + systemNodeVersion + "\n");
                }
            }
        }

        installNodeModules(isUsingSystemNpmAndNodejs);
        runOwaBuild(isUsingSystemNpmAndNodejs);

        wizard.showMessage("Build done.");
    }

    private boolean checkIfVersionsAreDefined(Version npmVersion, Version nodeVersion) {
        return npmVersion != null && nodeVersion != null &&
                StringUtils.isNotBlank(npmVersion.toString()) && StringUtils.isNotBlank(nodeVersion.toString());
    }

    private void runOwaBuild(boolean isUsingSystemNpmAndNodejs) throws MojoExecutionException {
        final String runArg = "run";
        final String buildArg = "build";

        List<String> args = new ArrayList<>();
        args.add(runArg);
        args.add(buildArg);

        if (isUsingSystemNpmAndNodejs) {
            owaHelper.runSystemNpmCommandWithArgs(args);
        }
        else {
            owaHelper.runLocalNpmCommandWithArgs(args);
        }
    }

    private void installNodeModules(boolean isUsingSystemNpmAndNodejs) throws MojoExecutionException {
        final String arg = "install";
        if (isUsingSystemNpmAndNodejs) {
            owaHelper.runSystemNpmCommandWithArgs(arg);
        }
        else {
            owaHelper.runLocalNpmCommandWithArgs(arg);
        }
    }

    /**
     * Creates temporary Maven reactor project to build dependencies in the correct order
     *
     * @param server
     * @return
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private File createTempReactorProject(Server server) throws MojoFailureException, MojoExecutionException {
        File tempFolder = new File(server.getServerDirectory(), "temp-project");
        if (tempFolder.exists()) {
            deleteTempReactorProject(tempFolder);
        }
        tempFolder.mkdir();

        Model tempModel = createModel();
        Set<Project> watchedModules = server.getWatchedProjects();
        for(Project module: watchedModules){
            if (!module.getModel().getPomFile().exists()) {
                throw new IllegalStateException("Module " + module.getArtifactId() + " could not be found at " + module.getModel().getPomFile().getAbsolutePath() + ". " +
                        "Unwatch this module by running mvn openmrs-sdk:unwatch -DartifactId="+module.getArtifactId()+" -DserverId=" + serverId);
            }

            //core is built before, its plugins fail when it is built as submodule
            if(!module.isOpenmrsCore()){
                Path newLink = Paths.get(new File(tempFolder, module.getArtifactId()).getAbsolutePath());
                Path existingfile = Paths.get(module.getPath());
                try {
                    Files.createSymbolicLink(newLink, existingfile);
                } catch (IOException e) {
                    copyModuleToTempServer(module.getPath(), newLink.toString());
                }  finally {
                    tempModel.addModule(module.getArtifactId());
                }
            }

        }

        try {
            Writer writer = new FileWriter(new File(tempFolder, "pom.xml"));
            new MavenXpp3Writer().write(writer, tempModel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write pom.xml", e);
        }

        return tempFolder;
    }

    private void copyModuleToTempServer(String orginalPath, String newPath){
        File module = new File(orginalPath);
        File copiedModule = new File(newPath);

        try {
            FileUtils.copyDirectory(module, copiedModule);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy modules", e);
        }
    }

    /**
     * Deletes temporary Maven reactor project
     *
     * @param tempFolder
     */
    private void deleteTempReactorProject(File tempFolder) {
        FileUtils.deleteQuietly(tempFolder);
    }

    /**
     * Creates Model to generate pom.xml for temporary project
     *
     * @return
     */
    private Model createModel(){
        Model model = new Model();
        model.setArtifactId(String.format("openmrs-sdk-server-%s", serverId));
        model.setVersion("1.0.0-SNAPSHOT");
        model.setGroupId("org.openmrs");
        model.setPackaging("pom");
        model.setModelVersion("4.0.0");

        return model;
    }

    /**
     * Deploy all watched modules to server
     *
     * @param server
     * @throws MojoFailureException
     * @throws MojoExecutionException
     * @throws MavenInvocationException
     */
    private void deployWatchedProjects(Server server) throws MojoFailureException, MojoExecutionException, MavenInvocationException {
        Set<Project> watchedProject = server.getWatchedProjects();
        for (Project module: watchedProject) {
            Project project = Project.loadProject(new File(module.getPath()));
            if(project.isOpenmrsModule()){
                new Deploy(this).deployModule(project.getGroupId(), project.getArtifactId(), project.getVersion(), server);
            } else if(project.isOpenmrsCore()){
                new ServerUpgrader(this).upgradePlatform(server, project.getVersion());
            }
        }
    }

    /**
     * Run "mvn clean install -DskipTests" command in the given directory
     * @param tempProject
     * @throws MojoFailureException
     */
    public void cleanInstallServerProject(File tempProject) throws MojoFailureException {
        Properties properties = new Properties();
        properties.put("skipTests", "true");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList("clean install"))
                .setProperties(properties)
                .setShowErrors(mavenSession.getRequest().isShowErrors())
                .setOffline(mavenSession.getRequest().isOffline())
                .setLocalRepositoryDirectory(mavenSession.getRequest().getLocalRepositoryPath())
                .setUpdateSnapshots(mavenSession.getRequest().isUpdateSnapshots())
                .setShowVersion(true)
                .setBaseDirectory(tempProject);


        Invoker invoker = new DefaultInvoker();
        InvocationResult result = null;
        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            throw new RuntimeException("Failed to build project in directory: "+tempProject);
        }
        if (result.getExitCode() != 0 ) {
            throw new IllegalStateException("Failed building project in "+tempProject.getAbsolutePath(), result.getExecutionException());
        }
    }
}
