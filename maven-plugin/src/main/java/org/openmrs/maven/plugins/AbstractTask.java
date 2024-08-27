package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DefaultJira;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.git.DefaultGitHelper;
import org.openmrs.maven.plugins.git.GitHelper;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.Jira;
import org.openmrs.maven.plugins.utility.ModuleInstaller;
import org.openmrs.maven.plugins.utility.NodeHelper;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.SpaInstaller;
import org.openmrs.maven.plugins.utility.StatsManager;
import org.openmrs.maven.plugins.utility.VersionsHelper;
import org.openmrs.maven.plugins.utility.Wizard;

import java.util.ArrayDeque;

/**
 * Base class for all OpenMRS SDK Maven Mojos
 */
public abstract class AbstractTask extends AbstractMojo {
	
	/**
	 * The project currently being build
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject mavenProject;
	
	/**
	 * The current Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	MavenSession mavenSession;
	
	/**
	 * The Maven Settings
	 */
	@Parameter(defaultValue = "${settings}", readonly = true)
	Settings settings;
	
	/**
	 * test mode, if true disables interactive mode and uses batchAnswers, even if there is none
	 */
	@Parameter(defaultValue = "false", property = "testMode")
	boolean testMode;
	
	/**
	 * path to openmrs directory
	 */
	@Parameter(property = "openMRSPath")
	String openMRSPath;
	
	/***
	 * answers to use if not running in interactive mode
	 */
	@Parameter(property = "batchAnswers")
	ArrayDeque<String> batchAnswers;
	
	/**
	 * stats
	 */
	@Parameter(defaultValue = "false", property = "stats")
	boolean stats;
	
	/**
	 * The artifact metadata source to use.
	 */
	@Component
	ArtifactMetadataSource artifactMetadataSource;
	
	@Component
	ArtifactFactory artifactFactory;
	
	/**
	 * The Maven BuildPluginManager component.
	 */
	@Component
	BuildPluginManager pluginManager;
	
	@Component
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
	 * handles Contents
	 */
	ContentHelper contentHelper;
	
	/**
	 * installs SPAs
	 */
	SpaInstaller spaInstaller;
	
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
	
	public AbstractTask() {
	}
	
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
		this.contentHelper = other.contentHelper;
		this.spaInstaller = other.spaInstaller;
		this.gitHelper = other.gitHelper;
		this.dockerHelper = other.dockerHelper;
		this.settings = other.settings;
		this.batchAnswers = other.batchAnswers;
		this.testMode = other.testMode;
		this.openMRSPath = other.openMRSPath;
		this.stats = other.stats;
		initTask();
	}
	
	public void initTask() {
		if (jira == null) {
			jira = new DefaultJira();
		}
		if (gitHelper == null) {
			gitHelper = new DefaultGitHelper();
		}
		if (versionsHelper == null) {
			versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
		}
		if (moduleInstaller == null) {
			moduleInstaller = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);
		}
		if (distroHelper == null) {
			distroHelper = new DistroHelper(mavenProject, mavenSession, pluginManager, wizard, versionsHelper);
		}
		if (owaHelper == null) {
			owaHelper = new OwaHelper(mavenSession, mavenProject, pluginManager, wizard);
		}
		if (contentHelper == null) {
			contentHelper = new ContentHelper(mavenSession, mavenProject, pluginManager);
		}
		if (spaInstaller == null) {
			spaInstaller = new SpaInstaller(distroHelper, new NodeHelper(mavenProject, mavenSession, pluginManager));
		}
		if (dockerHelper == null) {
			dockerHelper = new DockerHelper(mavenProject, mavenSession, pluginManager, wizard);
		}
		if (StringUtils.isNotBlank(openMRSPath)) {
			Server.setServersPath(openMRSPath);
		}
		
		if ((batchAnswers != null && !batchAnswers.isEmpty()) || testMode) {
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
}
