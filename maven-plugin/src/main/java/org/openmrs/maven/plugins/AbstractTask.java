package org.openmrs.maven.plugins;

import lombok.Getter;
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
import org.openmrs.maven.plugins.git.DefaultGitHelper;
import org.openmrs.maven.plugins.git.GitHelper;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.ConfigurationInstaller;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.DefaultJira;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.Jira;
import org.openmrs.maven.plugins.utility.MavenEnvironment;
import org.openmrs.maven.plugins.utility.ModuleInstaller;
import org.openmrs.maven.plugins.utility.OwaHelper;
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
	 * handles content packages
	 */
	ContentHelper contentHelper;

	/**
	 * handles OWAs
	 */
	OwaHelper owaHelper;

	/**
	 * installs SPAs
	 */
	SpaInstaller spaInstaller;

	/**
	 * installs configuration artifacts
	 */
	ConfigurationInstaller configurationInstaller;

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

	/**
	 * provides access to the current Maven environment
	 */
	@Getter
	private MavenEnvironment mavenEnvironment;

	public AbstractTask() {
	}

	public AbstractTask(AbstractTask other) {
		this.mavenEnvironment = other.mavenEnvironment;
		this.mavenProject = other.mavenProject;
		this.mavenSession = other.mavenSession;
		this.wizard = other.wizard;
		this.pluginManager = other.pluginManager;
		this.artifactFactory = other.artifactFactory;
		this.artifactMetadataSource = other.artifactMetadataSource;
		this.moduleInstaller = other.moduleInstaller;
		this.versionsHelper = other.versionsHelper;
		this.distroHelper = other.distroHelper;
		this.contentHelper = other.contentHelper;
		this.owaHelper = other.owaHelper;
		this.spaInstaller = other.spaInstaller;
		this.configurationInstaller = other.configurationInstaller;
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
		if (mavenEnvironment == null) {
			mavenEnvironment = new MavenEnvironment();
			mavenEnvironment.setMavenProject(mavenProject);
			mavenEnvironment.setMavenSession(mavenSession);
			mavenEnvironment.setSettings(settings);
			mavenEnvironment.setArtifactMetadataSource(artifactMetadataSource);
			mavenEnvironment.setArtifactFactory(artifactFactory);
			mavenEnvironment.setPluginManager(pluginManager);
			mavenEnvironment.setWizard(wizard);
		}
		if (jira == null) {
			jira = new DefaultJira();
		}
		if (gitHelper == null) {
			gitHelper = new DefaultGitHelper();
		}
		if (versionsHelper == null) {
			versionsHelper = new VersionsHelper(mavenEnvironment);
		}
		if (moduleInstaller == null) {
			moduleInstaller = new ModuleInstaller(mavenEnvironment);
		}
		if (distroHelper == null) {
			distroHelper = new DistroHelper(mavenEnvironment);
		}
		if (owaHelper == null) {
			owaHelper = new OwaHelper(mavenEnvironment);
		}
		if (contentHelper == null) {
			contentHelper = new ContentHelper(mavenEnvironment);
		}
		if (spaInstaller == null) {
			spaInstaller = new SpaInstaller(mavenEnvironment);
		}
		if (configurationInstaller == null) {
			configurationInstaller = new ConfigurationInstaller(mavenEnvironment);
		}
		if (dockerHelper == null) {
			dockerHelper = new DockerHelper(mavenEnvironment);
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
