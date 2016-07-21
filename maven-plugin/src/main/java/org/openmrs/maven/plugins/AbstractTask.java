package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DefaultJira;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.git.DefaultGitHelper;
import org.openmrs.maven.plugins.git.GitHelper;
import org.openmrs.maven.plugins.utility.Jira;
import org.openmrs.maven.plugins.utility.ModuleInstaller;
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
     * @parameter expression="${project}"
     */
    MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     */
    MavenSession mavenSession;

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
     * @component
     * @required
     */
    Wizard wizard;

    /**
     * test mode, if true disables interactive mode and uses batchAnswers, even if there is none
     *
     * @parameter expression="${testMode}" default-value="false"
     */
    String testMode;

    /**
     * path to openmrs directory
     *
     * @parameter expression="${openMRSPath}"
     */
    String openMRSPath;

    /***
     *
     * @parameter expression="${batchAnswers}"
     */
    ArrayDeque<String> batchAnswers;

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
     * handles github and provides basic git utilities
     */
    GitHelper gitHelper;

    /**
     * handles jira
     */
    Jira jira;


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
        this.gitHelper = other.gitHelper;
        initTask();
    }

    public void initTask() {
        if(jira == null){
            jira = new DefaultJira();
        }
        if(gitHelper == null){
            gitHelper = new DefaultGitHelper();
        }
        if(versionsHelper == null){
            versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
        }
        if(moduleInstaller==null){
            moduleInstaller = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);
        }
        if(distroHelper == null){
            distroHelper = new DistroHelper(mavenProject, mavenSession, pluginManager);
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
        new StatsManager(wizard, mavenSession).incrementGoalStats();
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
}
