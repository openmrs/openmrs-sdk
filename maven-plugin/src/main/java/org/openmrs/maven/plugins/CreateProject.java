package org.openmrs.maven.plugins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.archetype.mojos.CreateProjectFromArchetypeMojo;
import org.apache.maven.archetype.ui.generation.ArchetypeGenerationConfigurator;
import org.apache.maven.archetype.ui.generation.ArchetypeSelector;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.Invoker;
import org.codehaus.plexus.component.annotations.Requirement;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.StatsManager;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

/**
 * Creates a new OpenMRS project from an archetype
 */
// Most of the logic is from https://github.com/openmrs/openmrs-contrib-maven-plugin-module-wizard/blob/master/src/main/java/org/openmrs/maven/plugins/WizardMojo.java
@Mojo(name = "create-project", requiresProject = false)
public class CreateProject extends CreateProjectFromArchetypeMojo {

	private static final String TYPE_PLATFORM = "platform-module";

	private static final String TYPE_REFAPP = "referenceapplication-module";

	private static final String TYPE_OWA = "owa-project";

	private static final String OPTION_PLATFORM = "Platform module";

	public static final String OPTION_REFAPP = "Reference Application module";

	private static final String OPTION_OWA = "Open Web App";

	private static final String MODULE_ID_INFO =
			"Module id uniquely identifies your module in the OpenMRS world.\n\n" +
					"It is advised to consult your module id on https://talk.openmrs.org \n" +
					"to eliminate possible collisions. \n\n" +
					"Module id must consists of lowercase letters, must start from \n" +
					"a letter, can contain alphanumerics and dots, e.g. webservices.rest, \n" +
					"metadatasharing, reporting, htmlformentry.";

	private static final String MODULE_NAME_INFO =
			"Module name is a user friendly name displayed to the user " +
					"\ninstead of the module id. \n\n" +
					"By convention it is a module id with spaces between words.";

	private static final String MAVEN_INFO =
			"GroupId, artifactId and version combined together identify \nyour module in the maven repository. \n\n" +
					"By convention OpenMRS modules use 'org.openmrs.module' as a groupId \n" +
					"(must follow convention for naming java packages) and the module id \n" +
					"as an artifactId. The version should follow maven versioning convention, \n" +
					"which in short is: major.minor.maintenance(-SNAPSHOT).";

	private static final String DESCRIPTION_PROMPT_TMPL = "Describe your module in a few sentences";

	private static final String GROUP_ID_PROMPT_TMPL = "Please specify %s";

	private static final String AUTHOR_PROMPT_TMPL = "Who is the author of the module?";

	private static final String MODULE_TYPE_PROMPT = "What kind of project would you like to create?";

	@Component
	private ArchetypeManager manager;

	@Component
	private ArchetypeSelector selector;

	@Component
	private ArchetypeGenerationConfigurator configurator;

	@Component
	private ArchetypeGenerator generator;

	@Component
	private Invoker invoker;

	@Component
	Wizard wizard;

	/**
	 * answers to use if not running in interactive mode
	 */
	@Parameter(property = "batchAnswers")
	private ArrayDeque<String> batchAnswers;

	/**
	 * stats
	 */
	@Parameter(defaultValue = "false", property = "stats")
	boolean stats;

	/**
	 * The manager's artifactId. This can be an ordered comma separated list.
	 */
	@Parameter(property = "archetypeArtifactId")
	private String archetypeArtifactId;

	/**
	 * The manager's groupId.
	 */
	@Parameter(defaultValue = "org.openmrs.maven.archetypes", property = "archetypeGroupId")
	private String archetypeGroupId;

	/**
	 * The manager's version.
	 */
	@Parameter(property = "archetypeVersion")
	private String archetypeVersion;

	/**
	 * The manager's repository.
	 */
	@Parameter(property = "archetypeRepository")
	private String archetypeRepository;

	/**
	 * The manager's catalogs. It is a comma separated list of catalogs.
	 */
	@Parameter(defaultValue = "https://mavenrepo.openmrs.org/releases/content/archetype-catalog.xml", property = "archetypeCatalog")
	private String archetypeCatalog;

	/**
	 * Local Maven repository.
	 */
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	private ArtifactRepository localRepository;

	/**
	 * List of remote repositories used by the resolver.
	 */
	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
	private List<ArtifactRepository> remoteArtifactRepositories;

	/**
	 * test mode, if true disables interactive mode and uses batchAnswers, even if there is none
	 *
	 * @parameter property="testMode" default-value="false"
	 */
	@Parameter(defaultValue = "false", property = "testMode")
	boolean testMode;

	@Parameter(defaultValue = "${project.basedir}")
	private File basedir;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	/**
	 * The project currently being build.
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject mavenProject;

	/**
	 * The Maven BuildPluginManager component.
	 */
	@Component
	BuildPluginManager pluginManager;

	/**
	 * Additional goals that can be specified by the user during the creation of the manager.
	 */
	@Parameter(property = "goals")
	private String goals;

	/**
	 * The generated project's artifactId.
	 */
	@Parameter(property = "artifactId")
	private String artifactId;

	/**
	 * The generated project's groupId.
	 */
	@Parameter(property = "groupId")
	private String groupId;

	/**
	 * The generated project's version.
	 */
	@Parameter(property = "version")
	private String version;

	/**
	 * The generated project's package name.
	 */
	@Parameter(property = "package")
	private String packageName;

	/**
	 * The generated project's module name (no spaces).
	 */
	@Parameter(property = "moduleClassnamePrefix")
	private String moduleClassnamePrefix;

	/**
	 * The generated project's module name.
	 */
	@Parameter(property = "moduleName")
	private String moduleName;

	/**
	 * The generated project's module description.
	 */
	@Parameter(property = "moduleDescription")
	private String moduleDescription;

	/**
	 * The generated project's module author.
	 */
	@Parameter(defaultValue = "${user.name}")
	private String moduleAuthor;

	/**
	 * The generated project's Openmrs Platform Version.
	 */
	@Parameter(property = "platform")
	private String platform;

	/**
	 * The generated project's Openmrs Reference Application Version.
	 */
	@Parameter(property = "refapp")
	private String refapp;

	/**
	 * unique identifier module in the OpenMRS world
	 */
	@Parameter(property = "moduleId")
	private String moduleId;

	/**
	 * type of generated project module, platform or refapp
	 */
	@Parameter(property = "type")
	private String type;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if ((batchAnswers != null && !batchAnswers.isEmpty()) || testMode) {
			wizard.setAnswers(batchAnswers);
			wizard.setInteractiveMode(false);
		}

		new StatsManager(wizard, session, stats).incrementGoalStats();
		setProjectType();

		if (TYPE_OWA.equals(type)) {
			new OwaHelper(session, mavenProject, pluginManager, wizard).createOwaProject();
		} else {
			createModule();
		}

	}

	private void setProjectType() {
		String choice = wizard.promptForMissingValueWithOptions(MODULE_TYPE_PROMPT, type, null,
				Arrays.asList(OPTION_PLATFORM, OPTION_REFAPP, OPTION_OWA));

		if (OPTION_PLATFORM.equals(choice)) {
			type = TYPE_PLATFORM;
		} else if (OPTION_REFAPP.equals(choice)) {
			type = TYPE_REFAPP;
		} else if (OPTION_OWA.equals(choice)) {
			type = TYPE_OWA;
		}
	}

	private void createModule() throws MojoExecutionException, MojoFailureException {
		wizard.showMessage(MODULE_ID_INFO);
		moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
		moduleId = moduleId.toLowerCase();
		while (!moduleId.matches("[a-z][a-z0-9\\.]*")) {
			wizard.showError("The specified moduleId " + moduleId
					+ " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
			moduleId = null;
			moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
			moduleId = moduleId.toLowerCase();
		}

		wizard.showMessage(MODULE_NAME_INFO);
		moduleName = wizard
				.promptForValueIfMissingWithDefault(null, moduleName, "module name", StringUtils.capitalize(moduleId));
		while (!moduleName.matches("[a-zA-Z][a-zA-Z0-9\\.\\s]*")) {
			wizard.showError("The specified module name " + moduleName
					+ " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
			moduleName = null;
			moduleName = wizard
					.promptForValueIfMissingWithDefault(null, moduleName, "module name", StringUtils.capitalize(moduleId));
		}
		moduleName = StringUtils.capitalize(moduleName);
		moduleClassnamePrefix = StringUtils.deleteWhitespace(moduleName).replace(".", "");

		moduleDescription = wizard
				.promptForValueIfMissingWithDefault(DESCRIPTION_PROMPT_TMPL, moduleDescription, "", "no description");

		wizard.showMessage(MAVEN_INFO);
		groupId = wizard.promptForValueIfMissingWithDefault(GROUP_ID_PROMPT_TMPL, groupId, "groupId", "org.openmrs.module");
		while (!groupId.matches("[a-z][a-z0-9.]*")) {
			wizard.showError("The specified groupId " + groupId
					+ " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
			groupId = null;
			groupId = wizard
					.promptForValueIfMissingWithDefault(GROUP_ID_PROMPT_TMPL, groupId, "groupId", "org.openmrs.module");
		}

		artifactId = moduleId;

		version = wizard.promptForValueIfMissingWithDefault(null, version, "initial version", "1.0.0-SNAPSHOT");

		moduleAuthor = wizard.promptForValueIfMissingWithDefault(AUTHOR_PROMPT_TMPL, moduleAuthor, "", "anonymous");

		if (TYPE_PLATFORM.equals(type)) {
			platform = wizard.promptForValueIfMissingWithDefault(
					"What is the lowest version of the platform (-D%s) you want to support?", platform, "platform",
					"1.11.6");
			archetypeArtifactId = SDKConstants.PLATFORM_ARCH_ARTIFACT_ID;
		} else if (TYPE_REFAPP.equals(type)) {
			refapp = wizard.promptForValueIfMissingWithDefault(
					"What is the lowest version of the Reference Application (-D%s) you want to support?", refapp, "refapp",
					"2.4");
			archetypeArtifactId = SDKConstants.REFAPP_ARCH_ARTIFACT_ID;
		} else {
			throw new MojoExecutionException("Invalid project type");
		}

		archetypeVersion = getSdkVersion();
		packageName = "org.openmrs.module." + artifactId;

		Properties properties = new Properties();
		properties.setProperty("artifactId", artifactId);
		properties.setProperty("groupId", groupId);
		properties.setProperty("version", version);
		properties.setProperty("moduleClassnamePrefix", moduleClassnamePrefix);
		properties.setProperty("moduleName", moduleName);
		properties.setProperty("moduleDescription", moduleDescription);
		properties.setProperty("moduleAuthor", moduleAuthor);
		if (platform != null) {
			properties.setProperty("openmrsPlatformVersion", platform);
		} else if (refapp != null) {
			properties.setProperty("openmrsRefappVersion", refapp);
		}
		properties.setProperty("package", packageName);
		session.getUserProperties().putAll(properties);

		// Using custom prompts, avoid manager plugin interaction
		setPrivateField("interactiveMode", Boolean.FALSE);
		setPrivateField("archetypeArtifactId", archetypeArtifactId);
		setPrivateField("archetypeGroupId", archetypeGroupId);
		setPrivateField("archetypeVersion", archetypeVersion);
		setPrivateField("archetypeRepository", archetypeRepository);
		setPrivateField("archetypeCatalog", archetypeCatalog);
		setPrivateField("localRepository", localRepository);
		setPrivateField("remoteArtifactRepositories", remoteArtifactRepositories);
		setPrivateField("basedir", basedir);
		setPrivateField("session", session);
		setPrivateField("goals", goals);
		setPrivateField("manager", manager);
		setPrivateField("selector", selector);
		setPrivateField("configurator", configurator);
		setPrivateField("invoker", invoker);

		setPrivateField("session", session);

		Map<String, String> archetypeToVersion = new LinkedHashMap<>();

		archetypeToVersion.put(archetypeArtifactId, archetypeVersion);

		for (Map.Entry<String, String> archetype : archetypeToVersion.entrySet()) {
			getLog().info("Archetype: " + archetype.getKey());
			setPrivateField("archetypeArtifactId", archetype.getKey());
			setPrivateField("archetypeVersion", archetype.getValue());
			// Execute creating archetype for each archetype id
			super.execute();
		}
	}

	private String getSdkVersion() throws MojoExecutionException {
		InputStream sdkPom = CreateProject.class.getClassLoader().getResourceAsStream("sdk.properties");
		Properties sdk = new Properties();
		try {
			sdk.load(sdkPom);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		finally {
			IOUtils.closeQuietly(sdkPom);
		}
		return sdk.getProperty("version");
	}

	protected void setPrivateField(String fieldName, Object value) throws MojoExecutionException {
		if (value == null) {
			return;
		}

		try {
			Class<?> superClass = this.getClass().getSuperclass();
			Field field = superClass.getDeclaredField(fieldName);
			field.setAccessible(true); // Allow access to private field
			field.set(this, value);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unable to set mojo field: " + fieldName, e);
		}
	}
}
