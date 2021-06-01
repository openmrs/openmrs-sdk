package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Jira;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.git.GitHelper;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.ModuleInstaller;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.StatsManager;
import org.openmrs.maven.plugins.utility.VersionsHelper;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.util.ArrayDeque;

/**
 *
 */
public abstract class AbstractTask extends AbstractMojo {

    /**
     * The project currently being build.
     *
     * @parameter  property="project"
     */
    MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter  property="session"
     */
    MavenSession mavenSession;

    /**
     * The Maven Settings
     *
     * @parameter  property="settings"
     */
    Settings settings;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     */
    ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     * @required
     */
    ArtifactFactory artifactFactory;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    BuildPluginManager pluginManager;

    /**
     * test mode, if true disables interactive mode and uses batchAnswers, even if there is none
     *
     * @parameter  property="testMode" default-value="false"
     */
    String testMode;

    /**
     * path to openmrs directory
     *
     * @parameter  property="openMRSPath"
     */
    String openMRSPath;

    /***
     *
     * @parameter  property="batchAnswers"
     */
    ArrayDeque<String> batchAnswers;

    /**
     * stats
     *
     * @parameter  property="stats" default-value="false"
     */
    boolean stats;

    /**
     * Give prompts, processes answers, casts spells
     */
    Wizard wizard;

    /**
     * wizard for resolving artifact available versions
     */
    VersionsHelper versionsHelper;

    /**
     * handles installing modules on server
     */
    ModuleInstaller moduleInstaller;

    /**
     * handles distro-properties
     */
    DistroHelper distroHelper;

    /**
     * handles OWAs
     */
    OwaHelper owaHelper;

    /**
     * handles github and provides basic git utilities
     */
    GitHelper gitHelper;

    /**
     * handles jira
     */
    Jira jira;

    /**
     * handles docker
     */
    DockerHelper dockerHelper;


    public AbstractTask(){}

    public AbstractTask(AbstractTask other) {
        this.mavenProject = other.mavenProject;
        this.mavenSession = other.mavenSession;
        this.wizard = other.wizard;
        this.pluginManager = other.pluginManager;
        this.artifactFactory = other.artifactFactory;
        this.artifactMetadataSource = other.artifactMetadataSource;
        this.moduleInstaller = other.moduleInstaller;
        this.versionsHelper = other.versionsHelper;
        this.distroHelper = other.distroHelper;
        this.owaHelper = other.owaHelper;
        this.gitHelper = other.gitHelper;
        this.dockerHelper = other.dockerHelper;
	    this.settings = other.settings;
        initTask();
    }

    public void initTask() {
        if (wizard == null) {
            wizard = new Wizard();
        }
        if(jira == null){
            jira = new Jira();
        }
        if(gitHelper == null){
            gitHelper = new GitHelper();
        }
        if(versionsHelper == null){
            versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
        }
        if(moduleInstaller==null){
            moduleInstaller = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);
        }
        if(distroHelper == null){
            distroHelper = new DistroHelper(mavenProject, mavenSession, pluginManager, wizard);
        }
        if (owaHelper == null) {
            owaHelper = new OwaHelper(mavenSession, mavenProject, pluginManager, wizard);
        }
        if(dockerHelper == null){
            dockerHelper = new DockerHelper(mavenProject, mavenSession, pluginManager, wizard);
        }
        if(StringUtils.isNotBlank(openMRSPath)){
            Server.setServersPath(openMRSPath);
        }
        if((batchAnswers != null && !batchAnswers.isEmpty())||"true".equals(testMode)){
            wizard.setAnswers(batchAnswers);
            wizard.setInteractiveMode(false);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initTask();
        new StatsManager(wizard, mavenSession, stats).incrementGoalStats();
        executeTask();
    }

    abstract public void executeTask() throws MojoExecutionException, MojoFailureException;

    public Server loadValidatedServer(String serverId) throws MojoExecutionException {
        File serversPath = Server.getServersPathFile();
        File serverPath = new File(serversPath, serverId);
        new ServerUpgrader(this).validateServerMetadata(serverPath);
        Server server = Server.loadServer(serverPath);
        return server;
    }

    public Proxy getProxyFromSettings() {
        if (settings == null) {
            return null;
        }

        // Get active http/https proxy
        for (Proxy proxy : settings.getProxies()) {
            if (proxy.isActive() && ("http".equalsIgnoreCase(proxy.getProtocol()) || "https".equalsIgnoreCase(proxy.getProtocol()))) {
                return proxy;
            }
        }

        return null;
    }
}
