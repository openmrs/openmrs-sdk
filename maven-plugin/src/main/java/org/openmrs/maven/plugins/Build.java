package org.openmrs.maven.plugins;


import com.atlassian.util.concurrent.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

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
    private String npmVersion;

    /**
     * @parameter expression="${nodeVersion}"
     */
    private String nodeVersion;

    private final static String FRONTEND_BUILDER_GROUP_ID = "com.github.eirslett";
    private final static String FRONTEND_BUILDER_ARTIFACT_ID = "frontend-maven-plugin";
    private final static String FRONTEND_BUILDER_VERSION = "1.0";

    private final static String MAVEN_EXEC_PLUGIN_GROUP_ID = "org.codehaus.mojo";
    private final static String MAVEN_EXEC_PLUGIN_ARTIFACT_ID = "exec-maven-plugin";
    private final static String MAVEN_EXEC_PLUGIN_VERSION = "1.5.0";

    public static final String PACKAGE_JSON_FILENAME = "package.json";
    public static final String NODE_VERSION_KEY = "node-version";
    public static final String NPM_VERSION_KEY = "npm-version";

    public Build(){}

    public Build(AbstractTask other) { super(other); }

    public Build(AbstractTask other, String serverId){
        super(other);
        this.serverId = serverId;
    }

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
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

        Map<String, String> systemNodeAndNpmVersion = getSystemNodeAndNpmVersion();
        Map<String, String> batchNodeAndNpmVersion = getBatchNodeAndNpmVersion();
        if (checkIfVersionsAreDefined(batchNodeAndNpmVersion)) {
            wizard.showMessage("Using npm and nodejs versions defined in batch mode");
            if (!batchNodeAndNpmVersion.equals(systemNodeAndNpmVersion)) {
                isUsingSystemNpmAndNodejs = false;
                installLocalNodeAndNpm(batchNodeAndNpmVersion.get(NODE_VERSION_KEY), batchNodeAndNpmVersion.get(NPM_VERSION_KEY));
            }
            else {
                isUsingSystemNpmAndNodejs = true;
            }
        }
        else {
            if (!checkIfVersionsAreDefined(systemNodeAndNpmVersion)) {
                isUsingSystemNpmAndNodejs = false;
                Map<String, String> projectNodeAndNpmVersion = getProjectNpmAndNodeVersionFromPackageJson();
                if (checkIfVersionsAreDefined(projectNodeAndNpmVersion)) {
                    wizard.showMessage("Using npm and nodejs versions defined in package.json");
                    installLocalNodeAndNpm(projectNodeAndNpmVersion.get(NODE_VERSION_KEY), projectNodeAndNpmVersion.get(NPM_VERSION_KEY));
                }
                else {
                    wizard.showMessage("Using default npm and nodejs versions");
                    installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
                }
            }
            else {
                isUsingSystemNpmAndNodejs = true;
                wizard.showMessage("Using system npm and nodejs");
                wizard.showMessage("-npm: " + systemNodeAndNpmVersion.get(NPM_VERSION_KEY)
                       + "\n" + "-nodejs: " + systemNodeAndNpmVersion.get(NODE_VERSION_KEY) + "\n");
            }
        }

        installNodeModules(isUsingSystemNpmAndNodejs);
        runOwaBuild(isUsingSystemNpmAndNodejs);

        wizard.showMessage("Build done.");
    }

    private boolean checkIfVersionsAreDefined(Map <String, String> versions) {
        return StringUtils.isNotBlank(versions.get(NPM_VERSION_KEY)) && StringUtils.isNotBlank(versions.get(NODE_VERSION_KEY));
    }

    protected Map<String, String> getBatchNodeAndNpmVersion() {
        if (nodeVersion != null && !nodeVersion.startsWith("v")) {
            nodeVersion = "v" + nodeVersion;
        }
        Map<String, String> result = new HashMap<>();
        result.put(NPM_VERSION_KEY, npmVersion);
        result.put(NODE_VERSION_KEY, nodeVersion);
        return result;
    }

    protected Map<String, String> getSystemNodeAndNpmVersion() {
        Map<String, String> result = new HashMap<>();
        result.put(NPM_VERSION_KEY, getSystemNpmVersion());
        result.put(NODE_VERSION_KEY, getSystemNodeVersion());
        return result;
    }

    protected Map<String, String> getProjectNpmAndNodeVersionFromPackageJson() {
        Map<String, String> result = new HashMap<>();
        result.put(NPM_VERSION_KEY, getProjectNpmVersionFromPackageJson());
        result.put(NODE_VERSION_KEY, getProjectNodeVersionFromPackageJson());
        return result;
    }

    private String getSystemNpmVersion() {
        return runProcessAndGetFirstResponseLine("npm", "-v");
    }
    private String getSystemNodeVersion() {
        return runProcessAndGetFirstResponseLine("node", "-v");
    }

    private String getProjectNpmVersionFromPackageJson() {
        return getPropertyValueFromPropertiesJsonFile(PACKAGE_JSON_FILENAME, NPM_VERSION_KEY);
    }
    private String getProjectNodeVersionFromPackageJson() {
        return getPropertyValueFromPropertiesJsonFile(PACKAGE_JSON_FILENAME, NODE_VERSION_KEY);
    }

    protected static String getPropertyValueFromPropertiesJsonFile(String jsonFilename, String key) {
        JSONParser parser = new JSONParser();
        Object obj;
        try {
            obj = parser.parse(new FileReader(jsonFilename));
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't find " + jsonFilename + " at " + new File(jsonFilename).getAbsolutePath());
        } catch (ParseException e) {
            throw new IllegalStateException("Couldn't parse " + jsonFilename + ":", e);
        }
        JSONObject jsonObject =  (JSONObject) obj;

        return (String) jsonObject.get(key);
    }

    protected void installLocalNodeAndNpm(@Nullable String nodeVersion, @Nullable String npmVersion) throws MojoExecutionException {
        wizard.showMessage("-npm: " + npmVersion + "\n" + "-nodejs: " + nodeVersion + "\n");

        List<MojoExecutor.Element> configuration = new ArrayList<>();
        configuration.add(element("nodeVersion", nodeVersion));
        configuration.add(element("npmVersion", npmVersion));
        executeMojo(
                plugin(
                        groupId(FRONTEND_BUILDER_GROUP_ID),
                        artifactId(FRONTEND_BUILDER_ARTIFACT_ID),
                        version(FRONTEND_BUILDER_VERSION)
                ),
                goal("install-node-and-npm"),
                configuration(configuration.toArray(new MojoExecutor.Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private void runOwaBuild(boolean isUsingSystemNpmAndNodejs) throws MojoExecutionException {
        final String runArg = "run";
        final String buildArg = "build";

        List<String> args = new ArrayList<>();
        args.add(runArg);
        args.add(buildArg);

        if (isUsingSystemNpmAndNodejs) {
            runSystemNpmCommandWithArgs(args);
        }
        else {
            runLocalNpmCommandWithArgs(args);
        }
    }

    private void installNodeModules(boolean isUsingSystemNpmAndNodejs) throws MojoExecutionException {
        final String arg = "install";
        if (isUsingSystemNpmAndNodejs) {
            runSystemNpmCommandWithArgs(arg);
        }
        else {
            runLocalNpmCommandWithArgs(arg);
        }
    }

    private void runLocalNpmCommandWithArgs(String arg) throws MojoExecutionException {
        List<String> args = new ArrayList<>();
        args.add(arg);
        runLocalNpmCommandWithArgs(args);
    }

    protected void runLocalNpmCommandWithArgs(List<String> args) throws MojoExecutionException {
        List<MojoExecutor.Element> configuration = new ArrayList<>();

        StringBuilder argsString = new StringBuilder();

        for (String argument : args) {
            argsString.append(argument);
            argsString.append(" ");
        }

        configuration.add(element("arguments", argsString.toString()));
        executeMojo(
                plugin(
                        groupId(FRONTEND_BUILDER_GROUP_ID),
                        artifactId(FRONTEND_BUILDER_ARTIFACT_ID),
                        version(FRONTEND_BUILDER_VERSION)
                ),
                goal("npm"),
                configuration(configuration.toArray(new MojoExecutor.Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private void runSystemNpmCommandWithArgs(String arg) throws MojoExecutionException {
        List<String> args = new ArrayList<>();
        args.add(arg);
        runSystemNpmCommandWithArgs(args);
    }

    protected void runSystemNpmCommandWithArgs(List<String> args) throws MojoExecutionException {
        List<MojoExecutor.Element> configuration = new ArrayList<>();
        configuration.add(element("executable", "npm"));

        MojoExecutor.Element[] argsele = new MojoExecutor.Element[args.size()];

        for (int i = 0; i < args.size(); i++) {
            argsele[i] = new MojoExecutor.Element("argument", args.get(i));
        }

        MojoExecutor.Element parentArgs = new MojoExecutor.Element("arguments", argsele);

        configuration.add(parentArgs);
        executeMojo(
                plugin(
                        groupId(MAVEN_EXEC_PLUGIN_GROUP_ID),
                        artifactId(MAVEN_EXEC_PLUGIN_ARTIFACT_ID),
                        version(MAVEN_EXEC_PLUGIN_VERSION)
                ),
                goal("exec"),
                configuration(configuration.toArray(new MojoExecutor.Element[0])),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private String runProcessAndGetFirstResponseLine(String command, @Nullable String params) {
        Process process = null;
        List<String> lines = null;
        try {
            if (params != null) {
                process = new ProcessBuilder(command, params).redirectErrorStream(true).start();
            }
            else {
                process = new ProcessBuilder(command).redirectErrorStream(true).start();
            }
            lines = IOUtils.readLines(process.getInputStream());
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            System.out.println("");
        }

        if (process != null && process.exitValue() == 0) {
            if (lines != null) {
                return lines.get(0);
            }
            else {
                return null;
            }
        }
        else {
            return null;
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
