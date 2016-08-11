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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.Invoker;
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

/**
 *
 * @goal create-project
 * @requiresProject false
 *
 * Most of the logic is from https://github.com/openmrs/openmrs-contrib-maven-plugin-module-wizard/blob/master/src/main/java/org/openmrs/maven/plugins/WizardMojo.java
 *
 */
public class CreateProject extends CreateProjectFromArchetypeMojo {

    private static final String TYPE_PLATFORM = "platform-module";

    private static final String TYPE_REFAPP = "referenceapplication-module";

    private static final String OPTION_PLATFORM = "Platform module";

    public static final String OPTION_REFAPP = "Reference Application module";

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

    /** @component */
    private ArchetypeManager manager;

    /** @component */
    private ArchetypeSelector selector;

    /** @component */
    private ArchetypeGenerationConfigurator configurator;

    /** @component */
    private ArchetypeGenerator generator;

    /** @component */
    private Invoker invoker;

    /**
     * @component
     * @required
     */
    Wizard wizard;

    /**
     * @parameter expression="${batchAnswers}"
     */
    private ArrayDeque<String> batchAnswers;

    /**
     * The manager's artifactId. This can be an ordered comma separated list.
     *
     * @parameter expression="${archetypeArtifactId}"
     */
    private String archetypeArtifactId;

    /**
     * The manager's groupId.
     *
     * @parameter expression="${archetypeGroupId}" default-value="org.openmrs.maven.archetypes"
     */
    private String archetypeGroupId;

    /**
     * The manager's version.
     *
     * @parameter expression="${archetypeVersion}"
     */
    private String archetypeVersion;

    /**
     * The manager's repository.
     *
     * @parameter expression="${archetypeRepository}"
     */
    private String archetypeRepository;

    /**
     * The manager's catalogs. It is a comma separated list of catalogs.
     *
     * @parameter expression="${archetypeCatalog}"
     * default-value="http://mavenrepo.openmrs.org/nexus/service/local/repositories/releases/content/archetype-catalog.xml"
     */
    private String archetypeCatalog;

    /**
     * Local Maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of remote repositories used by the resolver.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * test mode, if true disables interactive mode and uses batchAnswers, even if there is none
     *
     * @parameter expression="${testMode}" default-value="false"
     */
    String testMode;

    /** @parameter expression="${basedir}" */
    private File basedir;

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * Additional goals that can be specified by the user during the creation of the manager.
     *
     * @parameter expression="${goals}"
     */
    private String goals;

    /**
     * The generated project's artifactId.
     *
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * The generated project's groupId.
     *
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * The generated project's version.
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * The generated project's package name.
     *
     * @parameter expression="${package}"
     */
    private String packageName;

    /**
     * The generated project's module name (no spaces).
     *
     * @parameter expression="${moduleClassnamePrefix}"
     */
    private String moduleClassnamePrefix;

    /**
     * The generated project's module name.
     *
     * @parameter expression="${moduleName}"
     */
    private String moduleName;

    /**
     * The generated project's module description.
     *
     * @parameter expression="${moduleDescription}"
     */
    private String moduleDescription;

    /**
     * The generated project's module author.
     *
     * @parameter expression="${user.name}"
     */
    private String moduleAuthor;

    /**
     * The generated project's Openmrs Platform Version.
     *
     * @parameter expression="${platform}"
     */
    private String platform;

    /**
     * The generated project's Openmrs Reference Application Version.
     *
     * @parameter expression="${refapp}"
     */
    private String refapp;

    /**
     * unique identifier module in the OpenMRS world
     *
     * @parameter expression="${moduleId}"
     */
    private String moduleId;
    /**
     * type of generated project module, platform or refapp
     *
     * @parameter expression="${type}"
     */
    private String type;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if((batchAnswers != null && !batchAnswers.isEmpty())||"true".equals(testMode)){
            wizard.setAnswers(batchAnswers);
            wizard.setInteractiveMode(false);
        }

        new StatsManager(wizard, session).incrementGoalStats();
        String choice = wizard.promptForMissingValueWithOptions(MODULE_TYPE_PROMPT, type, null, Arrays.asList(OPTION_PLATFORM, OPTION_REFAPP));

        if(OPTION_PLATFORM.equals(choice)){
            type = TYPE_PLATFORM;
        } else if(OPTION_REFAPP.equals(choice)) {
            type = TYPE_REFAPP;
        }

        wizard.showMessage(MODULE_ID_INFO);
        moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
        moduleId = moduleId.toLowerCase();
        while (!moduleId.matches("[a-z][a-z0-9\\.]*")) {
            wizard.showError("The specified moduleId " + moduleId + " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
            moduleId = null;
            moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");
            moduleId = moduleId.toLowerCase();
        }

        wizard.showMessage(MODULE_NAME_INFO);
        moduleName = wizard.promptForValueIfMissingWithDefault(null, moduleName, "module name", StringUtils.capitalize(moduleId));
        while (!moduleName.matches("[a-zA-Z][a-zA-Z0-9\\.\\s]*")) {
            wizard.showError("The specified module name " + moduleName + " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
            moduleName = null;
            moduleName = wizard.promptForValueIfMissingWithDefault(null, moduleName, "module name", StringUtils.capitalize(moduleId));
        }
        moduleName = StringUtils.capitalize(moduleName);
        moduleClassnamePrefix = StringUtils.deleteWhitespace(moduleName).replace(".", "");

        moduleDescription = wizard.promptForValueIfMissingWithDefault(DESCRIPTION_PROMPT_TMPL, moduleDescription, "", "no description");

        wizard.showMessage(MAVEN_INFO);
        groupId = wizard.promptForValueIfMissingWithDefault(GROUP_ID_PROMPT_TMPL, groupId, "groupId", "org.openmrs.module");
        while (!groupId.matches("[a-z][a-z0-9.]*")) {
            wizard.showError("The specified groupId " + groupId + " is not valid. It must start from a letter and can contain only alphanumerics and dots.");
            groupId = null;
            groupId = wizard.promptForValueIfMissingWithDefault(GROUP_ID_PROMPT_TMPL, groupId, "groupId", "org.openmrs.module");
        }

        artifactId = moduleId;

        version = wizard.promptForValueIfMissingWithDefault(null, version, "initial version", "1.0.0-SNAPSHOT");

        moduleAuthor = wizard.promptForValueIfMissingWithDefault(AUTHOR_PROMPT_TMPL, moduleAuthor, "", "anonymous");

        if(TYPE_PLATFORM.equals(type)){
            platform = wizard.promptForValueIfMissingWithDefault("What is the lowest version of the platform (-D%s) you want to support?", platform, "platform", "1.11.6");
            archetypeArtifactId = SDKConstants.PLATFORM_ARCH_ARTIFACT_ID;
        } else if(TYPE_REFAPP.equals(type)) {
            refapp = wizard.promptForValueIfMissingWithDefault("What is the lowest version of the Reference Application (-D%s) you want to support?", refapp, "refapp", "2.4");
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
        session.getExecutionProperties().putAll(properties);

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
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(sdkPom);
        }
        return sdk.getProperty("version");
    }

    protected void setPrivateField(String fieldName, Object value) throws MojoExecutionException {
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
