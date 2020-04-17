package org.openmrs.maven.plugins;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.openmrs.maven.plugins.bintray.BintrayPackage;
import org.openmrs.maven.plugins.bintray.OpenmrsBintray;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.CompositeException;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;


/**
 *
 * @goal release
 * @requiresProject false
 *
 */
public class Release extends AbstractTask {

    private static final String SCM_TMPL_CONNECTION = "scm:git:https://github.com/%s/%s.git";
    private static final String SCM_TMPL_DEV_CONNECTION = "scm:https://github.com/%s/%s.git";
    private static final String SCM_TMPL_URL = "https://github.com/%s/%s";
    private static final String SCM_TAG = "HEAD";

    private static final String BINTRAY_SERVER_ID = "bintray";

    private static final String BINTRAY_MAVEN_REPO_URL_TMPL = "https://api.bintray.com/maven/%s/%s/%s/";

    /**
     * version of next development iteration, should contain -SNAPSHOT suffix
     *
     * @parameter  property="developmentVersion"
     */
    private String developmentVersion;
    /**
     * version of release
     *
     * @parameter  property="releaseVersion"
     */
    private String releaseVersion;
    /**
     * github password to authorize changes
     *
     * @parameter  property="githubPassword"
     */
    private String githubPassword;
    /**
     * github username to authorize changes
     *
     * @parameter  property="githubUsername"
     */
    private String githubUsername;
    /**
     * Scm url address, if github repository, has to be https address (no support for ssh connection)
     *
     * @parameter  property="scmUrl"
     */
    private String scmUrl;
    /**
     *
     * @parameter  property="bintrayApiKey"
     */
    private String bintrayApiKey;
    /**
     *
     * @parameter  property="bintrayUsername"
     */
    private String bintrayUsername;
    /**
     *
     * @parameter  property="defaultVersions"
     */
    private String defaultVersions;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        if(mavenProject.getScm()==null){
            boolean agreeToAddScm = wizard.promptYesNo("There is no SCM section in project's pom.xml, would you like to configure it? (it can affect formatting of your pom file)");
            if(agreeToAddScm){
                addScm();
                saveMavenProject();
            }
            else{
                throw new MojoExecutionException("Cannot proceed with release without SCM section in pom.xml. Please add it manually.");
            }
        }

        Git git = new Git(gitHelper.getLocalRepository(mavenProject.getBasedir().getAbsolutePath()));
        //last commit before release, backup to rollback if there are any problems
        RevCommit backupCommit = gitHelper.getLastCommit(git);

        UsernamePasswordCredentials credentials = configureBintrayServer();
        String bintrayUrl = createPackageBintrayUrl(credentials, OpenmrsBintray.OPENMRS_MAVEN_REPO);
        String bintrayDeploymentRepository = "bintray::default::"+bintrayUrl;

        if(scmUrl == null){
            scmUrl = createScmUrl();
        }

        githubUsername = wizard.promptForValueIfMissing(githubUsername, "your GitHub username");
        githubPassword = wizard.promptForPasswordIfMissing(githubPassword, "your GitHub password");

        String defaultReleaseVersion = StringUtils.stripEnd(mavenProject.getVersion(), "-SNAPSHOT");
        String defaultDevelopmentVersion = createNewDevelopmentVersion(mavenProject.getVersion());

        if (wizard.checkYes(defaultVersions)) {
            releaseVersion = defaultReleaseVersion;
            developmentVersion = defaultDevelopmentVersion;
        } else {
            releaseVersion = wizard.promptForValueIfMissingWithDefault(null, releaseVersion, "release version",defaultReleaseVersion);
            developmentVersion = wizard.promptForValueIfMissingWithDefault(null, developmentVersion, "new development version", defaultDevelopmentVersion);
        }


        saveReleaseProperties();
        //enable batch mode, so prepare mojo use created release.properties
        mavenSession.getSettings().setInteractiveMode(false);
        try{
            wizard.showMessage("Preparing release...");
            executeMojo(
                    SDKConstants.getReleasePlugin(),
                    goal("prepare"),
                    configuration(
                            element(name("username"), githubUsername),
                            element(name("password"), githubPassword),
                            element(name("autoVersionSubmodules"), "true")
                    ),
                    executionEnvironment(mavenProject, mavenSession, pluginManager)
            );
            wizard.showMessage("Performing release...");
            executeMojo(
                    SDKConstants.getReleasePlugin(),
                    goal("perform"),
                    configuration(
                            element(name("username"), githubUsername),
                            element(name("password"), githubPassword),
                            element(name("arguments"), "-DaltDeploymentRepository="+bintrayDeploymentRepository+" -Dmaven.javadoc.failOnError=false")
                    ),
                    executionEnvironment(mavenProject, mavenSession, pluginManager)
            );
            wizard.showMessage("Release to OpenMRS Maven Bintray repository completed");

            OpenmrsBintray bintray = new OpenmrsBintray(getProxyFromSettings(),credentials.getUserName(), credentials.getPassword());


            File omodDir = new File(mavenProject.getBasedir(), "omod");
            if(omodDir.exists()){
                deployOmod(credentials, omodDir);
            }
            
            bintray.publishOpenmrsPackageVersion(OpenmrsBintray.OPENMRS_MAVEN_REPO, mavenProject.getArtifactId(), releaseVersion);
            wizard.showMessage("Artifacts published");
        } catch (Exception e){
            CompositeException allExceptions = new CompositeException("Failed to release");
            allExceptions.add("Error during release", e);
            handleError(git, backupCommit, allExceptions);
            allExceptions.checkAndThrow();
        }
        cleanupPluginFiles();
        wizard.showMessage("Release Completed");
    }

    private void deployOmod(UsernamePasswordCredentials credentials, File omodDir) {
        String omodName = mavenProject.getArtifactId()+"-"+releaseVersion+".omod";
        wizard.showMessage("Omod submodule detected...");
        File omodFile = new File(omodDir, "target"+File.separator+omodName);

        OpenmrsBintray openmrsBintray = new OpenmrsBintray(getProxyFromSettings(),credentials.getUserName(), credentials.getPassword());
        BintrayPackage bintrayPackage = openmrsBintray.getPackageMetadata(OpenmrsBintray.OPENMRS_OMOD_REPO, mavenProject.getArtifactId());
        if(bintrayPackage == null){
            bintrayPackage =  openmrsBintray.createPackage(mavenProject, OpenmrsBintray.OPENMRS_OMOD_REPO);
        }
        wizard.showMessage("Deploying "+omodName+" to Bintray OpenMRS Omod repository...");
        openmrsBintray.uploadOmod(bintrayPackage, omodName, omodFile, releaseVersion);
        wizard.showMessage(omodName+" deployed");
    }


    /**
     * Handles rolling back release:
     * -revert automatic commits by release plugin
     * -push revert commits to upstream
     * -delete local and remote tags
     */
    private void handleError(Git git, RevCommit backup, CompositeException allExceptions){
        String pomPath = mavenProject.getBasedir().getAbsolutePath()+File.separator+"pom.xml";
        RevCommit lastCommit = gitHelper.getLastCommit(git);
        try {
            cleanupPluginFiles();
            gitHelper.addRemoteUpstream(git, pomPath);
            if(!lastCommit.getName().equals(backup.getName())) {
                Iterable<RevCommit> commitDifferential = gitHelper.getCommitDifferential(git, "refs/remotes/upstream/master", "refs/heads/master");
                gitHelper.revertCommits(git, commitDifferential);
                gitHelper.push(git, githubUsername, githubPassword, "refs/heads/master", "upstream", false);
            }
            gitHelper.deleteTag(git, releaseVersion, githubUsername, githubPassword);
        } catch (Exception e) {
            allExceptions.add("Failure during clean up", e);
        }

    }

    private void cleanupPluginFiles() throws MojoExecutionException {
        executeMojo(
                SDKConstants.getReleasePlugin(),
                goal("clean"),
                configuration(),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private void saveReleaseProperties() {
        String projectId = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId();
        Properties properties = new Properties();
        properties.setProperty("project.dev."+projectId, developmentVersion);
        properties.setProperty("project.scm."+projectId+".tag", releaseVersion);
        properties.setProperty("scm.url", scmUrl);
        properties.setProperty("project.scm."+projectId+".developerConnection", scmUrl);
        properties.setProperty("project.scm."+projectId+".connection", scmUrl);
        properties.setProperty("project.rel."+projectId, releaseVersion);
        properties.setProperty("scm.tagNameFormat", "${project.version}");
        properties.setProperty("scm.tag", releaseVersion);
        properties.setProperty("autoVersionSubmodules", "true");
        File propsFile = new File(mavenProject.getBasedir(), "release.properties");
        try {
            properties.store(new FileOutputStream(propsFile), null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save release.properties file",e);
        }
    }

    private String createNewDevelopmentVersion(String version){
        Version old = new Version(version);
        int major = old.getMajorVersion();
        int minor = old.getMinorVersion();
        int incremental = old.getIncrementalVersion()+1;
        return major+"."+minor+"."+incremental+"-SNAPSHOT";
    }

    private String createScmUrl() {
        Scm scm = mavenProject.getScm();

        String connection = scm.getConnection();
        if(connection.contains("git@github.com:")){
            connection = connection.replace("git@github.com:", "https://github.com/");
        }
        return connection;
    }

    private String createPackageBintrayUrl(UsernamePasswordCredentials creds, String repository){
        OpenmrsBintray bintray = new OpenmrsBintray(getProxyFromSettings(),creds.getUserName(), creds.getPassword());
        BintrayPackage bintrayPackage = bintray.getPackageMetadata(repository, mavenProject.getArtifactId());

        if(bintrayPackage == null){
            bintray.createPackage(mavenProject, repository);
        }
        return String.format(BINTRAY_MAVEN_REPO_URL_TMPL, OpenmrsBintray.OPENMRS_USERNAME, repository, mavenProject.getArtifactId());
    }

    private UsernamePasswordCredentials configureBintrayServer() throws MojoExecutionException {
        SettingsManager settingsManager = new SettingsManager(mavenSession);
        List<Server> servers = settingsManager.getSettings().getServers();
        Server bintray = null;
        for(Server server : servers){
            if(server.getId().equals(BINTRAY_SERVER_ID)){
                bintray = server;
                break;
            }
        }
        if (bintray != null && StringUtils.isNotBlank(bintrayApiKey) && StringUtils.isNotBlank(bintrayUsername)) {
            wizard.showMessage(String.format("Found Bintray credentials for username '%s'", bintray.getUsername(), bintray.getPassword()));
            boolean useCurrent = wizard.promptYesNo("Would you like to use them? (if no, you will be asked for new credentials)");
            if(!useCurrent){
                String bintrayUser = wizard.promptForValueIfMissing(null, "your Bintray username");
                String bintrayApiKey = wizard.promptForPasswordIfMissing(null, "your Bintray API key (can be found under API keys at https://bintray.com/profile/edit after you log in)");
                bintray.setUsername(bintrayUser);
                bintray.setPassword(bintrayApiKey);
                settingsManager.apply();
            }
        } else {
            wizard.showMessage("No Bintray server configuration found, you have to provide Bintray credentials to proceed");
            bintray = new Server();
            bintray.setId(BINTRAY_SERVER_ID);
            String bintrayUser = wizard.promptForValueIfMissing(this.bintrayUsername, "your Bintray username");
            String bintrayApiKey = wizard.promptForPasswordIfMissing(this.bintrayApiKey, "your Bintray API key");
            bintray.setUsername(bintrayUser);
            bintray.setPassword(bintrayApiKey);

            settingsManager.addServer(bintray);
            settingsManager.apply();
        }

        Server server = settingsManager.getSettings().getServer(BINTRAY_SERVER_ID);
        return new UsernamePasswordCredentials(server.getUsername(), server.getPassword());
    }

    private void addScm() throws MojoExecutionException {
        Scm scm = new Scm();
        String githubUser = wizard.promptForValueIfMissing(null, "your GitHub username");
        String githubRepo = wizard.promptForValueIfMissing(null, "your GitHub repository");

        scm.setConnection(String.format(SCM_TMPL_CONNECTION, githubUser, githubRepo));
        scm.setDeveloperConnection(String.format(SCM_TMPL_DEV_CONNECTION, githubUser, githubRepo));
        scm.setUrl(String.format(SCM_TMPL_URL, githubUser, githubRepo));
        scm.setTag(SCM_TAG);

        mavenProject.setScm(scm);
    }

    private void saveMavenProject() throws MojoExecutionException {
        File pom = new File(mavenProject.getBasedir(), "pom.xml");
        FileWriter writer = null;
        try {
            writer = new FileWriter(pom);
            new MavenXpp3Writer().write(writer, mavenProject.getModel());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(writer);
        }
        if(!pom.exists()){
            throw new RuntimeException("Error during saving backup pom.xml");
        }
    }
}
