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
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
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
            "Module id uniquely identifies your module in the OpenMRS world.\n" +
                    "It is advised to consult your module id on https://talk.openmrs.org to eliminate possible collisions. \n" +
                    "Module id must consists of lowercase letters, must start from a letter, can contain alphanumerics and dots,\n" +
                    "e.g. webservices.rest, metadatasharing, reporting, htmlformentry.";
    private static final String MODULE_NAME_INFO =
            "Module name is a user friendly name displayed to the user instead of the module id. \n" +
                    "By convention it is a module id with spaces between words.";
    private static final String MAVEN_INFO =
            "GroupId, artifactId and version combined together identify your module in the maven repository. \n" +
                    "By convention OpenMRS modules use 'org.openmrs.module' as a groupId and the module id as an artifactId. \n" +
                    "The version should follow maven versioning convention, which in short is: major.minor.maintenance(-SNAPSHOT).";

    private static final String DESCRIPTION_PROMPT_TMPL = "Describe Your module in a few sentences %s";
    private static final String GROUP_ID_PROMPT_TMPL = "Please specify %s (default: '%s', must follow java conventions for naming packages): ";
    private static final String ARTIFACT_ID_PROMPT_TMPL = "Please specify '%s' (all lowercase, must start from a letter, allowed a-z, 0-9)";
    private static final String AUTHOR_PROMPT_TMPL = "Who is the author of the module?%s";
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
     * User settings use to check the interactiveMode.
     *
     * @parameter expression="${interactiveMode}" default-value="true"
     * @required
     */
    private Boolean interactiveMode;

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
     * @parameter default-value="org.openmrs.module.basicexample"
     */
    private String packageName;

    /**
     * The generated project's module name (no spaces).
     *
     * @parameter expression="${moduleNameNoSpaces}" default-value="BasicExample"
     */
    private String moduleNameNoSpaces;

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
     * @parameter expression="${user.name}" default-value="You"
     */
    private String moduleAuthor;

    /**
     * The generated project's prefix for classes from archetype
     *
     * @parameter expression="${moduleClassPrefix}"
     */
    private String moduleClassPrefix;

    /**
     * The generated project's openMRSVersion.
     *
     * @parameter expression="${openmrsVersion}" default-value="1.8.2"
     */

    private String openmrsVersion;

    /**
     * The generated project's object name
     *
     * @parameter expression="${serviceDaoName}" default-value="BasicExample"
     */
    private String serviceDaoName;

    /**
     * The generated project's hibernate name.
     *
     * @parameter expression="${objectName}" default-value="BasicExample"
     */
    private String objectName;

    /**
     * The generated project's admin link condition.
     *
     * @parameter expression="${adminLinkReply}" default-value="y"
     */
    private String adminLinkReply;

    /**
     * The generated project's service/dao/hibernate condition.
     *
     * @parameter expression="${serviceReply}" default-value="y"
     */
    private String serviceReply;

    /**
     * The generated project's dependent modules list.
     *
     * @parameter expression="${dependentModules}" default-value="[none]"
     */
    private String dependentModules;

    /**
     * The generated project's dependent modules condition.
     *
     * @parameter expression="${dependentModulesReply}" default-value="n"
     */
    private String dependentModulesReply;

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
    /**
     * The generated project's moduleActivatorManagement condition.
     * Depending on this property module activator will
     * implement Activator interface or extend BaseModuleActivator abstract class
     *
     * @parameter expression="${moduleActivatorManagement}" default-value="y"
     */
    private String moduleActivatorManagement;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if("false".equals(interactiveMode)){
            wizard.setInteractiveMode(false);
        }

        String choice = wizard.promptForMissingValueWithOptions(
                MODULE_TYPE_PROMPT, type, null,
                Arrays.asList(OPTION_PLATFORM, OPTION_REFAPP), null, null);

        if(OPTION_PLATFORM.equals(choice)){
            type = TYPE_PLATFORM;
        } else if(OPTION_REFAPP.equals(choice)) {
            type = TYPE_REFAPP;
        }

        wizard.showMessage(MODULE_ID_INFO);
        moduleId = wizard.promptForValueIfMissingWithDefault(null, moduleId, "module id", "basicexample");

        wizard.showMessage(MODULE_NAME_INFO);
        moduleName = wizard.promptForValueIfMissingWithDefault(null, moduleName, "module name", "Basic example");
        moduleDescription = wizard.promptForValueIfMissingWithDefault(DESCRIPTION_PROMPT_TMPL, moduleDescription, "", "no description");

        wizard.showMessage(MAVEN_INFO);
        groupId = wizard.promptForValueIfMissingWithDefault(GROUP_ID_PROMPT_TMPL, groupId, "groupId", "org.openmrs.module");
        artifactId = wizard.promptForValueIfMissingWithDefault(ARTIFACT_ID_PROMPT_TMPL, artifactId, "artifactId", "basicexample");
        version = wizard.promptForValueIfMissingWithDefault(null, version, "initial version", "1.0.0-SNAPSHOT");
        moduleAuthor = wizard.promptForValueIfMissingWithDefault(AUTHOR_PROMPT_TMPL, moduleAuthor, "", "anonymous");

        if(TYPE_PLATFORM.equals(type)){
            openmrsVersion = wizard.promptForValueIfMissingWithDefault("What is the lowest version of the "+OPTION_PLATFORM+" you want to support?%s", openmrsVersion, "", "");
            archetypeArtifactId = SDKConstants.PLATFORM_ARCH_ARTIFACT_ID;
        } else if(TYPE_REFAPP.equals(type)) {
            openmrsVersion = wizard.promptForValueIfMissingWithDefault("What is the lowest version of the "+OPTION_REFAPP+" you want to support?%s", openmrsVersion, "", "");
            archetypeArtifactId = SDKConstants.REFAPP_ARCH_ARTIFACT_ID;
        } else {
            throw new MojoExecutionException("Invalid project type");
        }

        archetypeVersion = getSdkVersion();

        try {
            setParameterWithOutSpaces("moduleNameNoSpaces", moduleName);
            setParameterWithOutSpaces("serviceDaoName", serviceDaoName);
            setParameterWithOutSpaces("objectName", objectName);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error in removing spaces " + e);
        }

        moduleNameNoSpaces = moduleNameNoSpaces.toLowerCase();
        packageName = "org.openmrs.module." + artifactId;
        moduleName = moduleName + " Module";
        String serviceDaoBeanId = Character.toLowerCase(serviceDaoName.charAt(0))
                + (serviceDaoName.length() > 1 ? serviceDaoName.substring(1) : "");

        Properties properties = new Properties();
        properties.setProperty("artifactId", artifactId);
        properties.setProperty("groupId", groupId);
        properties.setProperty("version", version);
        properties.setProperty("module-name-no-spaces", moduleNameNoSpaces);
        properties.setProperty("module-name", moduleName);
        properties.setProperty("module-description", moduleDescription);
        properties.setProperty("module-author", moduleAuthor);
        properties.setProperty("openmrs-version", openmrsVersion);
        properties.setProperty("service-dao-name-no-spaces", serviceDaoName);
        properties.setProperty("service-dao-bean-id", serviceDaoBeanId);
        properties.setProperty("object-name-no-spaces", objectName);
        properties.setProperty("package", packageName);
        properties.setProperty("adminLinkReply", adminLinkReply);
        properties.setProperty("serviceReply", serviceReply);
        properties.setProperty("dependentModules", dependentModules);
        properties.setProperty("moduleActivatorManagement", moduleActivatorManagement);
        if(TYPE_REFAPP.equals(type)){
            moduleClassPrefix = StringUtils.capitalize(moduleName).replace(" ", "");
            properties.setProperty("D-moduleClassPrefix", moduleClassPrefix);
        }
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

        Map<String, String> archetypeToVersion = new HashMap<>();

        archetypeToVersion.put(archetypeArtifactId, archetypeVersion);

        if ("y".equalsIgnoreCase(adminLinkReply)) {
            archetypeToVersion.put("openmrs-archetype-adminpagelink-creation", "1.1.1");
        }
        if ("y".equalsIgnoreCase(serviceReply)) {
            archetypeToVersion.put("openmrs-archetype-service-dao-hibernate-creation", "1.1.1");
        }

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

    /**
     * Removes spaces from the WizardMojo fields. Mainly used for setting fields whose values are
     * used for naming files in archetypes.
     */
    protected void setParameterWithOutSpaces(String fieldName, String value) throws NoSuchFieldException,
            IllegalAccessException {

        Field fi = CreateProject.class.getDeclaredField(fieldName);
        String brk[] = value.split(" ");
        String valueWithNoSpace = "";
        for (String string : brk) {
            valueWithNoSpace += string;
        }
        fi.set(this, valueWithNoSpace);

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
