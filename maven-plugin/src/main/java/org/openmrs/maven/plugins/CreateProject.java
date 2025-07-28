package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.model.Model;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromResource;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * Creates a new OpenMRS project from an archetype
 */
// Most of the logic is from https://github.com/openmrs/openmrs-contrib-maven-plugin-module-wizard/blob/master/src/main/java/org/openmrs/maven/plugins/WizardMojo.java
@Mojo(name = "create-project", requiresProject = false)
public class CreateProject extends AbstractTask {

	private static final String TYPE_PLATFORM = "platform-module";

	private static final String TYPE_REFAPP = "referenceapplication-module";

	private static final String TYPE_OWA = "owa-project";

	private static final String TYPE_CONTENT_PACKAGE = "content-package";

	private static final String OPTION_PLATFORM = "Platform module";

	public static final String OPTION_REFAPP = "Reference Application module";

	private static final String OPTION_OWA = "Open Web App";

	private static final String OPTION_CONTENT_PACKAGES = "Content Package";

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

	private static final String CONTENT_MAVEN_INFO =
			"GroupId, artifactId and version combined together identify your module in the maven repository. \n\n" +
					"By convention the OpenMRS Content Packages modules use 'org.openmrs.content' as a groupId \n" +
					"(must follow convention for naming java packages) and the module id \n" +
					"as an artifactId. The version should follow maven versioning convention, \n" +
					"which in short is: major.minor.maintenance(-SNAPSHOT).";

	private static final String DESCRIPTION_PROMPT_TMPL = "Describe your module in a few sentences";

	private static final String GROUP_ID_PROMPT_TMPL = "Please specify %s";

	private static final String AUTHOR_PROMPT_TMPL = "Who is the author of the module?";

	private static final String MODULE_TYPE_PROMPT = "What kind of project would you like to create?";
	private static final Logger log = LoggerFactory.getLogger(CreateProject.class);

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
	 * Applying some filter on displayed archetypes list: format is artifactId or groupId:artifactId.
	 * org.apache: -> displays all archetypes which contain org.apache in groupId
	 * :jee or jee -> displays all archetypes which contain jee in artifactId
	 * org.apache:jee -> displays all archetypes which contain org.apache in groupId AND jee in artifactId
	 * Since:
	 * 2.1
	 */
	@Parameter(defaultValue = "org.openmrs.maven.archetypes:", property = "filter")
	private String filter;

	/**
	 * The output directory.
	 */
	@Parameter(defaultValue = "${project.basedir}", property = "outputDirectory")
	private File outputDirectory;

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
	 * The generated project's Openmrs Content Package Version.
	 */
	@Parameter(property = "contentpackage")
	private String contentpackage;

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

	public void setWizard(Wizard wizard) {
		this.wizard = wizard;
	}

	public void setProjectType(String type) {
		this.type = type;
	}

	@Override
	public void executeTask() throws MojoExecutionException, MojoFailureException {
		setProjectType();

		if (TYPE_OWA.equals(type)) {
			new OwaHelper(getMavenEnvironment()).createOwaProject();
		} else {
			createModule();
		}
	}

	private void setProjectType() throws MojoExecutionException {
		String choice = wizard.promptForMissingValueWithOptions(MODULE_TYPE_PROMPT, type, null,
				Arrays.asList(OPTION_PLATFORM, OPTION_REFAPP, OPTION_OWA, OPTION_CONTENT_PACKAGES));

		if (OPTION_PLATFORM.equals(choice)) {
			type = TYPE_PLATFORM;
		} else if (OPTION_REFAPP.equals(choice)) {
			type = TYPE_REFAPP;
		} else if (OPTION_OWA.equals(choice)) {
			type = TYPE_OWA;
		} else if (OPTION_CONTENT_PACKAGES.equals(choice)) {
			type = TYPE_CONTENT_PACKAGE;
		}
	}

	private void createModule() throws MojoExecutionException {
		if (outputDirectory == null) {
			outputDirectory = Paths.get("").toFile();
		}
		
		wizard.showMessage(MODULE_ID_INFO);
		moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
		moduleId = moduleId.toLowerCase();
		while (!moduleId.matches("[a-z][a-z0-9.]*")) {
			wizard.showError("The specified moduleId " + moduleId
					+ " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
			moduleId = null;
			moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
			moduleId = moduleId.toLowerCase();
		}

		wizard.showMessage(MODULE_NAME_INFO);
		String enteredModuleName = moduleName;
		moduleName = wizard
				.promptForValueIfMissingWithDefault(null, enteredModuleName, "module name", StringUtils.capitalize(moduleId));
		while (!moduleName.matches("[a-zA-Z][a-zA-Z0-9.\\s]*")) {
			wizard.showError("The specified module name " + moduleName
					+ " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
			moduleName = wizard
					.promptForValueIfMissingWithDefault(null, enteredModuleName, "module name", StringUtils.capitalize(moduleId));
		}

		moduleName = StringUtils.capitalize(moduleName);
		moduleClassnamePrefix = StringUtils.deleteWhitespace(moduleName).replace(".", "");

		moduleDescription = wizard
				.promptForValueIfMissingWithDefault(DESCRIPTION_PROMPT_TMPL, moduleDescription, "", "no description");

		if (TYPE_CONTENT_PACKAGE.equals(type)) {
			groupId = "org.openmrs.content";
			wizard.showMessage(CONTENT_MAVEN_INFO);
		} else {
			groupId = "org.openmrs.module";
			wizard.showMessage(CONTENT_MAVEN_INFO);
		}

		artifactId = moduleId;

		version = wizard.promptForValueIfMissingWithDefault(null, version, "initial version", "1.0.0-SNAPSHOT");

		moduleAuthor = wizard.promptForValueIfMissingWithDefault(AUTHOR_PROMPT_TMPL, moduleAuthor, "", "anonymous");

		if (TYPE_PLATFORM.equals(type)) {
			choosePlatformVersion();
			archetypeArtifactId = SDKConstants.PLATFORM_ARCH_ARTIFACT_ID;
		} else if (TYPE_REFAPP.equals(type)) {
			refapp = wizard.promptForValueIfMissingWithDefault(
			    "What is the lowest version of the Reference Application (-D%s) you want to support?", refapp, "refapp",
			    "2.4");
			archetypeArtifactId = SDKConstants.REFAPP_ARCH_ARTIFACT_ID;
		} else if (TYPE_CONTENT_PACKAGE.equals(type)) {
			archetypeArtifactId = SDKConstants.CONTENT_PACKAGE_ARCH_ARTIFACT_ID;
		} else {
			throw new MojoExecutionException("Invalid project type");
		}

		archetypeVersion = getSdkVersion();
		packageName = groupId + "." + artifactId;

		Properties properties = new Properties();
		properties.setProperty("artifactId", artifactId);
		properties.setProperty("groupId", groupId);
		properties.setProperty("version", version);
		properties.setProperty("moduleName", moduleName);
		properties.setProperty("moduleDescription", moduleDescription);
		properties.setProperty("moduleAuthor", moduleAuthor);
		if (platform != null) {
			properties.setProperty("openmrsPlatformVersion", platform);
			properties.setProperty("moduleClassnamePrefix", moduleClassnamePrefix);

			Model model = loadModelFromPom(platform);
			extractTestDependencyVersionsFromModel(model, properties);
		} else if (refapp != null) {
			properties.setProperty("openmrsRefappVersion", refapp);
			properties.setProperty("moduleClassnamePrefix", moduleClassnamePrefix);
		}
		properties.setProperty("package", packageName);
		mavenSession.getUserProperties().putAll(properties);

		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-archetype-plugin"),
						version("3.2.0")
				),
				goal("generate"),
				configuration(
					element(name("interactiveMode"), "false"),
					element(name("archetypeArtifactId"), archetypeArtifactId),
					element(name("archetypeGroupId"), archetypeGroupId),
					element(name("archetypeVersion"), archetypeVersion),
					element(name("filter"), filter),
					element(name("outputDirectory"), outputDirectory.getAbsolutePath()),
					element(name("goals"), goals)
				),
				executionEnvironment(
						mavenProject,
						mavenSession,
						pluginManager
				)
		);
	}

	private String getSdkVersion() throws MojoExecutionException {
		return loadPropertiesFromResource("sdk.properties").getProperty("version", null);
	}

	private Model loadModelFromPom(String platformVersion) throws MojoExecutionException {
		File tempDir = new File("temp-pom");
		tempDir.mkdir();
		try {
			executeMojo(
					plugin(
							groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
							artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
							version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
					),
					goal("copy"),
					configuration(
							element(name("artifactItems"),
									element(name("artifactItem"),
											element(name("groupId"), "org.openmrs"),
											element(name("artifactId"), "openmrs"),
											element(name("version"), platformVersion),
											element(name("type"), "pom"),
											element(name("outputDirectory"), tempDir.getAbsolutePath())
									)
							)
					),
					executionEnvironment(mavenProject, mavenSession, pluginManager)
			);

			File pomFile = new File(tempDir, "openmrs-" + platformVersion + ".pom");
			Project project = Project.loadProject(tempDir, pomFile.getName());
			return project.getModel();
		} finally {
			if (tempDir.exists()) {
				deleteDirectoryRecursively(tempDir);
			}
		}
	}

	private void extractTestDependencyVersionsFromModel(Model model, Properties properties) {
		Properties modelProperties = model.getProperties();
		
		properties.setProperty("mockitoVersion", modelProperties.getProperty("mockitoVersion"));
		properties.setProperty("junitJupiterVersion", modelProperties.getProperty("junitVersion"));

		for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
			String groupId = dependency.getGroupId();
			String artifactId = dependency.getArtifactId();
			String version = resolveVersion(dependency.getVersion(), modelProperties);

			switch (groupId) {
				case "org.powermock":
					log.info("powerMockVersion: {}", version);
					properties.setProperty("powerMockVersion", version);
					break;

				case "junit":
					if ("junit".equals(artifactId)) {
						log.info("junitVersion: {}", version);
						properties.setProperty("junitVersion", version);
					}
					break;
			}
		}
	}

	private String resolveVersion(String version, Properties modelProperties) {
		if (version != null && version.startsWith("${") && version.endsWith("}")) {
			String propertyName = version.substring(2, version.length() - 1);
			return modelProperties.getProperty(propertyName);
		}
		return version;
	}

	private void deleteDirectoryRecursively(File dir) {
		if (dir.isDirectory()) {
			File[] entries = dir.listFiles();
			if (entries != null) {
				for (File file : entries) {
					deleteDirectoryRecursively(file);
				}
			}
		}
		dir.delete();
	}

	public void choosePlatformVersion() throws MojoExecutionException {
		if (TYPE_PLATFORM.equals(type)) {
			boolean validVersion = false;
			while (!validVersion) {
				platform = wizard.promptForValueIfMissingWithDefault(
						"What is the lowest version of the platform (-D%s) you want to support?", platform, "platform", "2.5.0");
				
				if (compareVersions(platform, "2.5.0")) {
					wizard.showMessage("Platform version must be at least 2.5.0. Please try again.");
					platform = null;
				} else {
					validVersion = true;
				}
			}
		}
	}

	public boolean compareVersions(String version1, String version2) {
		Version v1 = new Version(version1);
		Version v2 = new Version(version2);
		return v1.lower(v2);
	}
}
