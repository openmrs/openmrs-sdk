package org.openmrs.maven.plugins.utility;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for attribute helper functions
 */
@Component(role=Wizard.class)
public class DefaultWizard implements Wizard {
    private static final String EMPTY_STRING = "";
    private static final String NONE = "(none)";
    private static final String DEFAULT_CHOICE_TMPL = "Which one do you choose?";
    private static final String DEFAULT_OPTION_TMPL = "%d) %s";
    private static final String DEFAULT_CUSTOM_OPTION_TMPL = "%d) Other...";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final String DEFAULT_PROMPT_TMPL = "Please specify %s";
    private static final String DEFAULT_VALUE_TMPL = " (default: '%s')";
    private static final String DEFAULT_VALUE_TMPL_WITH_DEFAULT = "Please specify %s: (default: '%s')";
    private static final String DEFAULT_FAIL_MESSAGE = "Server with such serverId is not exists";
    private static final String INVALID_SERVER = "Invalid server Id";
    private static final String YESNO = " [Y/n]";
    private static final String REFERENCEAPPLICATION_2_4 = "org.openmrs.distro:referenceapplication-package:2.4";
    private static final String DEFAULT_CUSTOM_DIST_ARTIFACT = "Please specify custom distribution artifact%s (default: '%s')";
    private static final String CUSTOM_JDK_PATH = "Please specify a path to JDK used for running this server (-Djdk)";
    private static final String SDK_PROPERTIES_FILE = "SDK Properties file";
    private static final String REFAPP_OPTION_TMPL = "Reference Application %s";
    private static final String REFAPP_ARTIFACT_TMPL = "org.openmrs.distro:referenceapplication-package:%s";
    private static final String JDK_ERROR_TMPL = "\nThe JDK %s is not compatible with OpenMRS Platform %s. " +
            "Please use %s to run this server.\n\nIf you are running " +
            "in a forked mode, correct the java.home property in %s\n";
    private static final String UPGRADE_CONFIRM_TMPL = "\nThe %s %s introduces the following changes:";
    private static final String UPDATE_MODULE_TMPL = "^ Updates %s %s to %s";
    private static final String ADD_MODULE_TMPL = "+ Adds %s %s";
    private static final String NO_DIFFERENTIAL = "\nNo modules to update or add found";
    public static final String PLATFORM_VERSION_PROMPT = "You can deploy the following versions of a platform";
    public static final String DISTRIBUTION_VERSION_PROMPT = "You can deploy the following versions of distribution";

    public static final String DB_OPTION_H2 = "H2";
    public static final String DB_OPTION_MYSQL = "MySQL 5.6 (requires pre-installed MySQL 5.6)";
    public static final String DB_OPTION_SDK_DOCKER_MYSQL = "MySQL 5.6 in SDK docker container (requires pre-installed Docker)";
    public static final String DB_OPTION_DOCKER_MYSQL = "Existing docker container (requires pre-installed Docker)";
    public static final Map<String,String> DB_OPTIONS_MAP = new HashMap<String, String>() {{
        put("mysql", DB_OPTION_MYSQL);
        put("h2", DB_OPTION_H2);
        put("docker", DB_OPTION_SDK_DOCKER_MYSQL);
    }};
    public static final String DBNAME_URL_VARIABLE = "@DBNAME@";

    @Requirement
    Prompter prompter;

    Log log;

    private boolean interactiveMode = true;

    private ArrayDeque<String> batchAnswers;

    public DefaultWizard(){};

    public DefaultWizard(Prompter prompter) {
        this.prompter = prompter;
    }

    @Override
    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    @Override
    public void setInteractiveMode(boolean interactiveMode) {
        this.interactiveMode = interactiveMode;
    }

    /**
     * Prompt for serverId, and get default serverId which is not exists,
     * if serverId is not set before
     *
     * @param server @return
     * @throws PrompterException
     */
    @Override
    public void promptForNewServerIfMissing(Server server) {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(Server.getServersPath(), defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        String serverId =  promptForValueIfMissingWithDefault("Specify server id (-D%s)", server.getServerId(), "serverId", defaultServerId);
        server.setServerId(serverId);
    }


    /**
     * Prompt for a value if it not set, and default value is set
     *
     * @param message
     * @param value
     * @param parameterName
     * @param defValue
     * @return value
     * @throws PrompterException
     */
    @Override
    public String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue) {
        return promptForValueIfMissingWithDefault(message, value, parameterName, defValue, false);
    }

    public String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue, boolean password) {
        String textToShow;
        if (StringUtils.isEmpty(defValue)){
            textToShow = String.format(message != null ? message : DEFAULT_PROMPT_TMPL, parameterName);
        }
        else {
            textToShow = String.format(message != null? message : DEFAULT_PROMPT_TMPL, parameterName) + String.format(DEFAULT_VALUE_TMPL, defValue);
        }

        if (value != null) {
            return value;
        }else if(!interactiveMode){
            return getAnswer(textToShow);
        }

        String val;
        if (password) {
            val = promptForPassword(textToShow);
        } else {
            val = prompt(textToShow);
        }
        if (StringUtils.isBlank(val)) {
            val = defValue;
        }
        return val;
    }

    @Override
    public String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options){
        return promptForMissingValueWithOptions(message, value, parameterName, options, null, null);
    }

    @Override
    public String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, String customMessage, String customDefault){

        String defaultOption = options.isEmpty()? "" : options.get(0);
        String question = String.format(message != null? message : DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defaultOption);

        if (value != null) {
            return value;
        } else if(!interactiveMode){
            return getAnswer(question);
        }


        System.out.println("\n" + question + ":");
        List<Integer> choices = new ArrayList<>();
        int i = 0;
        for(String option : options){
            i++;
            System.out.println(String.format(DEFAULT_OPTION_TMPL, i, option));
            choices.add(i);
        }
        if(customMessage != null){
            i++;
            System.out.println(String.format(DEFAULT_CUSTOM_OPTION_TMPL, i));
            choices.add(i);
        }

        String choice = prompt(DEFAULT_CHOICE_TMPL + " [" + StringUtils.join(choices.iterator(), "/") + "]");
        int chosenIndex = -1;
        if(!StringUtils.isBlank(choice) && StringUtils.isNumeric(choice)) {
            chosenIndex = Integer.parseInt(choice) - 1;
        }

        if(chosenIndex >= 0) {
            if (chosenIndex < options.size()){
                return options.get(chosenIndex);
            } else if(chosenIndex == options.size() && customMessage != null) {
                return promptForValueIfMissingWithDefault(customMessage, null, parameterName, customDefault);
            }
        }

        System.out.println("\nYou must specify " + StringUtils.join(choices.iterator(), " or ") + ".");
        return promptForMissingValueWithOptions(message, value, parameterName, options, customMessage, customDefault);
    }


    private String prompt(String textToShow){
        try {
            return prompter.prompt("\n" + textToShow);
        } catch (PrompterException e) {
            throw new RuntimeException(e);
        }
    }

    public String promptForPassword(String textToShow){
        return new String(System.console().readPassword(textToShow));
    }

    public String promptForPasswordIfMissing(String value, String parameter){
        if (value != null) {
            return value;
        } else if(!interactiveMode){
            return getAnswer(parameter);
        } if(System.console() == null){
            return promptForValueIfMissing(value, parameter);
        }else {
            return new String(System.console().readPassword("\n"+ DEFAULT_PROMPT_TMPL +": ", parameter));
        }
    }

    public void showMessage(String textToShow){
        System.out.println("\n" + textToShow);
    }

    @Override
    public void showError(String textToShow) {
        System.out.println("\n[ERROR]" + textToShow);
    }

    /**
     * Prompt for a value with list of proposed values
     * @param value
     * @param parameterName
     * @param values
     * @return value
     * @throws PrompterException
     */
    @Override
    public String promptForValueWithDefaultList(String value, String parameterName, List<String> values) {
        if (value != null) return value;
        String defaultValue = values.size() > 0 ? values.get(0) : NONE;
        final String text = DEFAULT_PROMPT_TMPL + DEFAULT_VALUE_TMPL + " (possible: %s)";
        String val = prompt(String.format(text, parameterName, defaultValue, StringUtils.join(values.toArray(), ", ")));
        if (val.equals(EMPTY_STRING)) val = defaultValue;
        return val;
    }

    /**
     * Prompt for a value if it not set, and default value is NOT set
     * @param value
     * @param parameterName
     * @return
     * @throws PrompterException
     */
    @Override
    public String promptForValueIfMissing(String value, String parameterName) {
        return promptForValueIfMissingWithDefault(null, value, parameterName, EMPTY_STRING);
    }

    /**
     * Print dialog Yes/No
     * @param text - text to display
     * @return
     */
    @Override
    public boolean promptYesNo(String text) {
        String yesNo = null;
        if(interactiveMode){
            yesNo = prompt(text.concat(YESNO));
        } else {
            yesNo = getAnswer(text);
        }
        return yesNo.equals("") || yesNo.toLowerCase().equals("y");

    }

    /**
     * Check if value is submit
     * @param value
     * @return
     */
    @Override
    public boolean checkYes(String value) {
        String val = value.toLowerCase();
        return val.equals("true") || val.equals("yes");
    }

    /**
     * Get path to server by serverId and prompt if missing
     * @return
     * @throws MojoFailureException
     */
    @Override
    public String promptForExistingServerIdIfMissing(String serverId) {
        File omrsHome = new File(Server.getServersPath());
        List<String> servers = getListOfServers();
        if(servers.isEmpty()){
            throw new RuntimeException("There is no servers available");
        }
        serverId = promptForMissingValueWithOptions("You have the following servers:", serverId, "serverId", servers);
        if (serverId.equals(NONE)) {
            throw new RuntimeException(INVALID_SERVER);
        }
        File serverPath = new File(omrsHome, serverId);
        if (!serverPath.exists()) {
            throw new RuntimeException("There is no server with the given server id. Please create it first using openmrs-sdk:setup.");
        }
        return serverId;
    }

    @Override
    public void promptForPlatformVersionIfMissing(Server server, List<String> versions) {
        String version = promptForMissingValueWithOptions(PLATFORM_VERSION_PROMPT,
                server.getVersion(), "version", versions, "Please specify platform version", null);
        server.setVersion(version);
    }

	@Override
	public String promptForPlatformVersion(List<String> versions) {
        String version = promptForMissingValueWithOptions(PLATFORM_VERSION_PROMPT,
                null, "version", versions, "Please specify platform version", null);
        return version;
	}

    @Override
    public void promptForJavaHomeIfMissing(Server server) {
        if (!StringUtils.isBlank(server.getJavaHome())) {
            if (isJavaHomeValid(server.getJavaHome())) {
                addJavaHomeToSdkProperties(server.getJavaHome());
                return;
            } else {
                throw new IllegalArgumentException("The specified -DjavaHome property is invalid");
            }
        }

        if (interactiveMode) {
            List<String> options = new ArrayList<>();
            options.add("JAVA_HOME (currently: " + System.getProperty("java.home") + ")");
            options.addAll(getJavaHomeOptions());

            String selectedOption = promptForMissingValueWithOptions(SDKConstants.OPENMRS_SDK_JDK_OPTION,
                    server.getJavaHome(), "path", options, SDKConstants.OPENMRS_SDK_JDK_CUSTOM, null);

            String requiredJdkVersion;
            String notRecommendedJdkVersion = "Not recommended";
            if (server.getPlatformVersion().startsWith("1.")) {
                requiredJdkVersion = "1.7";
                notRecommendedJdkVersion = "1.8";
            }
            else {
                requiredJdkVersion = "1.8";
            }

            // Use default JAVA_HOME
            if (selectedOption.equals(options.get(0))) {
                if (System.getProperty("java.version").startsWith(requiredJdkVersion)) {
                    server.setJavaHome(null);
                }
                else if (System.getProperty("java.version").startsWith(notRecommendedJdkVersion)) {
                    boolean isSelectJdk7 = promptYesNo("It is not recommended to run OpenMRS platform " + server.getPlatformVersion() + " on JDK 8. Would you like to select the recommended JDK 7 instead?");
                    if (isSelectJdk7) {
                        promptForJavaHomeIfMissing(server);
                    }
                    else {
                        server.setJavaHome(null);
                    }
                }
                else {
                    showMessage("Your JAVA_HOME version doesn't fit platform requirements:");
                    showMessage("JAVA_HOME version: " + System.getProperty("java.version"));
                    showMessage("Required: " + requiredJdkVersion);
                    promptForJavaHomeIfMissing(server);
                }
            } else if (!isJavaHomeValid(selectedOption)) {
                System.out.println(SDKConstants.OPENMRS_SDK_JDK_CUSTOM_INVALID);
                promptForJavaHomeIfMissing(server);
            } else {
                String jdkUnderSpecifiedPathVersion = determineJavaVersionFromPath(selectedOption);
                if (jdkUnderSpecifiedPathVersion.startsWith(requiredJdkVersion)) {
                    server.setJavaHome(selectedOption);
                    addJavaHomeToSdkProperties(selectedOption);
                }
                else if (jdkUnderSpecifiedPathVersion.startsWith(notRecommendedJdkVersion)) {
                    boolean isSelectJdk7 = promptYesNo("It is not recommended to run OpenMRS platform " + server.getPlatformVersion() + " on JDK 8. Would you like to select the recommended JDK 7 instead?");
                    if (isSelectJdk7) {
                        promptForJavaHomeIfMissing(server);
                    }
                    else {
                        server.setJavaHome(null);
                    }
                }
                else {
                    showMessage("JDK in custom path (" + selectedOption + ") doesn't match platform requirements:");
                    showMessage("JDK version: " + jdkUnderSpecifiedPathVersion);
                    showMessage("Required: " + requiredJdkVersion);
                    promptForJavaHomeIfMissing(server);
                }
            }
        }
    }

    private String determineJavaVersionFromPath(String path) {
        File javaPath = new File(path, "bin");

        List<String> commands = new ArrayList<>();
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
            javaPath = new File(javaPath, "java.exe");
        } else {
            javaPath = new File(javaPath, "java");
        }
        commands.add(javaPath.toString());
        commands.add("-version");

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);

        String result;
        try {
            final Process process = processBuilder.start();
            List<String> output = IOUtils.readLines(process.getInputStream());
            result = StringUtils.join(output.iterator(), "\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch Java version from \"" + path + "\"");
        }

        Pattern p = Pattern.compile(".*\\\"(.*)\\\".*");
        Matcher m = p.matcher(result);
        if (m.find()) {
            return m.group(1);
        }
        else {
            throw new RuntimeException("Failed to fetch Java version from \"" + path + "\". 'java -version' returned " + result);
        }
    }

    private void addJavaHomeToSdkProperties(String path) {
        File sdkPropertiesFile = new File(Server.getServersPathFile(), SDKConstants.OPENMRS_SDK_PROPERTIES);
        Properties sdkProperties = getSdkProperties();
        List<String> jdkPaths = getJavaHomeOptions();

        if (!jdkPaths.contains(path)) {
            if (jdkPaths.size() == 5) {
                jdkPaths.set(4, path);
            }
            else {
                jdkPaths.add(path);
            }

            Collections.sort(jdkPaths);

            String updatedProperty = StringUtils.join(jdkPaths.iterator(), ", ");
            sdkProperties.setProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS, updatedProperty);
            savePropertiesChangesToFile(sdkProperties, sdkPropertiesFile, SDK_PROPERTIES_FILE);
        }
    }

    private Properties getSdkProperties() {
        File sdkPropertiesFile = new File(Server.getServersPathFile(), SDKConstants.OPENMRS_SDK_PROPERTIES);

        if (!sdkPropertiesFile.exists()) {
            try {
                sdkPropertiesFile.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create SDK properties file in: \"" + Server.getServersPathFile() + "/" + SDKConstants.OPENMRS_SDK_PROPERTIES + "\"");
            }
        }

        InputStream in;
        try {
            in = new FileInputStream(sdkPropertiesFile);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("SDK properties file not found at: \"" + Server.getServersPathFile() + "/" + SDKConstants.OPENMRS_SDK_PROPERTIES + "\"");
        }
        Properties sdkProperties = new Properties();
        try {
            sdkProperties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties from file");
        }
        return sdkProperties;
    }

    private List<String> getJavaHomeOptions() {
        Properties sdkProperties = getSdkProperties();
        List<String> result = new ArrayList<>();

        if (interactiveMode) {
            String jdkHomeProperty = sdkProperties.getProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS);
            if (jdkHomeProperty != null) {
                for (String path: Arrays.asList(jdkHomeProperty.split("\\s*,\\s*"))) {
                    if (isJavaHomeValid(path)) {
                        result.add(path);
                    }
                }

                // Save properties
                Collections.sort(result);
                String updatedProperty = StringUtils.join(result.iterator(), ", ");
                sdkProperties.setProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS, updatedProperty);
                File sdkPropertiesFile = new File(Server.getServersPathFile(), SDKConstants.OPENMRS_SDK_PROPERTIES);
                savePropertiesChangesToFile(sdkProperties, sdkPropertiesFile, SDK_PROPERTIES_FILE);

                return result;
            } else {
                return new ArrayList<>();
            }
        }else {
            return new ArrayList<>();
        }

    }

    private boolean isJavaHomeValid(String jdkPath) {
        File jdk = new File(jdkPath, "bin");
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            jdk = new File(jdk, "java.exe");
        } else {
            jdk = new File(jdk, "java");
        }

        return jdk.exists();
    }

    private void savePropertiesChangesToFile(Properties properties, File file, String message) {
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            properties.store(fos, message + ":");
            fos.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

	@Override
    public void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper) throws MojoExecutionException {
        if(server.getVersion()==null){
            String choice = promptForRefAppVersion(versionsHelper);
            Artifact distro = DistroHelper.parseDistroArtifact(choice);
            if(distro != null){
                server.setVersion(distro.getVersion());
                server.setDistroArtifactId(distro.getArtifactId());
                server.setDistroGroupId(distro.getGroupId());
            } else {
                server.setDistroArtifactId(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID);
                server.setDistroGroupId(Artifact.GROUP_DISTRO);
                server.setVersion(choice);
            }
        }
    }

    public String promptForRefAppVersion(VersionsHelper versionsHelper) {
        int maxOptionsSize = 5;
        Map<String, String> optionsMap = new LinkedHashMap<>();
        Set<String> versions = new LinkedHashSet<>(versionsHelper.getVersionAdvice(SDKConstants.getReferenceModule("2.3.1"), maxOptionsSize));
        versions.addAll(SDKConstants.SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER);
        List<ArtifactVersion> artifactVersions = new ArrayList<>();
        for(String version : versions){
            artifactVersions.add(new DefaultArtifactVersion(version));
        }
        for(String version : versionsHelper.getVersionAdvice(artifactVersions, 5)){
            optionsMap.put(String.format(REFAPP_OPTION_TMPL, version), String.format(REFAPP_ARTIFACT_TMPL, version));
            if(optionsMap.size()== maxOptionsSize) break;
        }

        String version = promptForMissingValueWithOptions(DISTRIBUTION_VERSION_PROMPT,
                null, "distribution artifact", Lists.newArrayList(optionsMap.keySet()), "Please specify %s", REFERENCEAPPLICATION_2_4);

        String artifact = optionsMap.get(version);
        if (artifact != null) {
            return artifact;
        } else {
            return version;
        }
    }

    @Override
    public void promptForDb(Server server, DockerHelper dockerHelper, boolean h2supported, String dbDriver) throws MojoExecutionException {
        String db = null;
        if(StringUtils.isNotBlank(dbDriver)){
            db = DB_OPTIONS_MAP.get(dbDriver);
        }
        List<String> options = new ArrayList<>();
        if(h2supported) options.add(DB_OPTION_H2);
        options.addAll(Lists.newArrayList(DB_OPTION_MYSQL, DB_OPTION_SDK_DOCKER_MYSQL, DB_OPTION_DOCKER_MYSQL));
        db = promptForMissingValueWithOptions("Which database would you like to use?", db, null, options);
        switch(db){
            case(DB_OPTION_H2): {
                server.setDbDriver(SDKConstants.DRIVER_H2);
                if (server.getDbUri() == null) {
                    server.setDbUri(SDKConstants.URI_H2);
                }
                server.setDbUser("root");
                server.setDbPassword("root");
                break;
            }
            case(DB_OPTION_MYSQL):{
                promptForMySQLDb(server);
                break;
            }
            case(DB_OPTION_SDK_DOCKER_MYSQL):{
                promptForDockerizedSdkMysql(server, dockerHelper);
                break;
            }
            case(DB_OPTION_DOCKER_MYSQL):{
                promptForDockerizedDb(server, dockerHelper);
            }
        }
    }

    @Override
    public void promptForMySQLDb(Server server) throws MojoExecutionException {
        if(server.getDbDriver() == null){
            server.setDbDriver(SDKConstants.DRIVER_MYSQL);
        }
        String dbUri = promptForValueIfMissingWithDefault(
                "The distribution requires MySQL database. Please specify database uri (-D%s)",
                server.getDbUri(), "dbUri", SDKConstants.URI_MYSQL);
        if (dbUri.startsWith("jdbc:mysql:")) {
            dbUri = addMySQLParamsIfMissing(dbUri);
        }
        dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());

        server.setDbUri(dbUri);
        promptForDbCredentialsIfMissing(server);
    }

    public void promptForDockerizedSdkMysql(Server server, DockerHelper dockerHelper) throws MojoExecutionException {
        promptForDockerHostIfMissing(dockerHelper);

        if(server.getDbDriver() == null){
            server.setDbDriver(SDKConstants.DRIVER_MYSQL);
        }

        String dbUri = (SDKConstants.URI_MYSQL.replace("3306", DockerHelper.DOCKER_MYSQL_PORT));
        //Windows and Mac use the Docker Machine, which gets assigned an IP different than the host
        if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
            try {
                URI uri = new URI(dockerHelper.getDockerHost());
                dbUri = dbUri.replace("localhost", uri.getHost());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());
        dbUri = addMySQLParamsIfMissing(dbUri);

        server.setDbUri(dbUri);
        server.setDbUser(DockerHelper.DOCKER_MYSQL_USERNAME);
        server.setDbPassword(DockerHelper.DOCKER_MYSQL_PASSWORD);
        server.setContainerId(DockerHelper.DOCKER_DEFAULT_CONTAINER_ID);

        dockerHelper.createMySqlContainer(DockerHelper.DOCKER_DEFAULT_CONTAINER_ID, DockerHelper.DOCKER_MYSQL_PORT);
        dockerHelper.runDbContainer(server.getContainerId(), server.getDbUri(), server.getDbUser(), server.getDbPassword());
    }

    private void promptForDockerHostIfMissing(DockerHelper dockerHelper) {
        if (StringUtils.isBlank(dockerHelper.getDockerHost())) {
            String dockerHost;
            if(SystemUtils.IS_OS_LINUX){
                dockerHost = promptForValueIfMissingWithDefault("Please specify Docker host URL (either 'tcp://' or 'unix://')",
                        dockerHelper.getDockerHost(), "dockerHost", DockerHelper.DEFAULT_HOST_LINUX);
            } else {
                dockerHost = promptForValueIfMissingWithDefault(
                        "Please specify Docker Machine URL (find out by running `docker-machine url`)",dockerHelper.getDockerHost(), "dockerHost", null);
            }
            dockerHelper.saveDockerHost(dockerHost);
        }
    }

    public void promptForDockerizedDb(Server server, DockerHelper dockerHelper) throws MojoExecutionException {
        promptForDockerHostIfMissing(dockerHelper);

        String containerId = prompt("Please specify your container id/name/label (you can get it using command `docker ps -a`)");
        String username = prompt("Please specify DB username");
        String password = prompt("Please specify DB password");

        String dbUri = promptForValueIfMissingWithDefault(
                "Please specify database uri (-D%s)", server.getDbUri(), "dbUri", SDKConstants.URI_MYSQL);
        if (dbUri.startsWith("jdbc:mysql:")) {
            server.setDbDriver(SDKConstants.DRIVER_MYSQL);
            dbUri = addMySQLParamsIfMissing(dbUri);
        }
        dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());

        server.setDbUri(dbUri);
        server.setDbUser(username);
        server.setDbPassword(password);
        server.setContainerId(containerId);

        dockerHelper.runDbContainer(containerId, server.getDbUri(), username, password);
    }

    @Override
    public void promptForDbCredentialsIfMissing(Server server) {
        String defaultUser = "root";
        String user = promptForValueIfMissingWithDefault(
                "Please specify database username (-D%s)",
                server.getDbUser(), "dbUser", defaultUser);
        server.setDbUser(user);
        //set password
        String dbPassword = promptForValueIfMissingWithDefault(
                "Please specify database password (-D%s)",
                server.getDbPassword(), "dbPassword", " ");
        server.setDbPassword(dbPassword);
    }

    /**
     * Get servers with recently used first
     * @return
     */
    @Override
    public List<String> getListOfServers() {
        File openMRS = new File(Server.getServersPath());
        Map<Long, String> sortedMap = new TreeMap<Long, String>(Collections.reverseOrder());
        File [] list = (openMRS.listFiles() == null) ? new File[0] : openMRS.listFiles();
        for (File f: list) {
            if (f.isDirectory()){
                if(Server.hasServerConfig(f)){
                    sortedMap.put(f.lastModified(), f.getName());
                }
            }
        }
        return new ArrayList<String>(sortedMap.values());
    }

    @Override
    public String addMySQLParamsIfMissing(String dbUri) {
        String noJdbc = dbUri.substring(5);

        URIBuilder uri;
        try {
            uri = new URIBuilder(noJdbc);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        uri.setParameter("autoReconnect", "true");
        uri.setParameter("sessionVariables", "storage_engine=InnoDB");
        uri.setParameter("useUnicode", "true");
        uri.setParameter("characterEncoding", "UTF-8");

        return "jdbc:" + uri.toString();
    }

    public Log getLog() {
        if(log == null){
            log = new SystemStreamLog();
        }
        return log;
    }

    @Override
    public void showJdkErrorMessage(String jdk, String platform, String recommendedJdk, String pathToServerProperties) {
        System.out.println(String.format(JDK_ERROR_TMPL, jdk, platform, recommendedJdk, pathToServerProperties));
    }

    /**
     * Show confirmation prompt if there is any change besides updating modules with SNAPSHOT versions
     * @return
     */
    @Override
    public boolean promptForConfirmDistroUpgrade(UpgradeDifferential upgradeDifferential, Server server, DistroProperties distroProperties){
        if(upgradeDifferential.isEmpty()){
            showMessage(NO_DIFFERENTIAL);
            return false;
        }

        boolean needConfirmation = false;

        if(upgradeDifferential.getPlatformArtifact() !=null){
            if(!needConfirmation){
                System.out.println(String.format(UPGRADE_CONFIRM_TMPL, distroProperties.getName(), distroProperties.getServerVersion()));
                needConfirmation = true;
            }
            System.out.println(String.format(UPDATE_MODULE_TMPL,
                    upgradeDifferential.getPlatformArtifact().getArtifactId(),
                    server.getPlatformVersion(),
                    upgradeDifferential.getPlatformArtifact().getVersion()));
        }
        for(Entry<Artifact, Artifact> updateEntry : upgradeDifferential.getUpdateOldToNewMap().entrySet()){
            //update map should contain entry with equal versions only when they are same snapshots
            //(e.g. update 'appui 0.2-SNAPSHOT' to 'appui 0.2-SNAPSHOT')
            //updating to same SNAPSHOT doesn't require confirmation, they are not shown
            if(!updateEntry.getKey().getVersion().equals(updateEntry.getValue().getVersion())){
                if(!needConfirmation){
                    System.out.println(String.format(UPGRADE_CONFIRM_TMPL, distroProperties.getName(), distroProperties.getServerVersion()));
                    needConfirmation = true;
                }
                System.out.println(String.format(UPDATE_MODULE_TMPL,
                        updateEntry.getKey().getArtifactId(),
                        updateEntry.getKey().getVersion(),
                        updateEntry.getValue().getVersion()));
            }
        }

        for(Artifact addArtifact : upgradeDifferential.getModulesToAdd()){
            if(!needConfirmation){
                System.out.println(String.format(UPGRADE_CONFIRM_TMPL, distroProperties.getName(), distroProperties.getServerVersion()));
                needConfirmation = true;
            }
            System.out.println(String.format(ADD_MODULE_TMPL,
                    addArtifact.getArtifactId(),
                    addArtifact.getVersion()));
        }

        if(needConfirmation){
            return promptYesNo(String.format("Would you like to apply those changes to '%s'?", server.getServerId()));
        }
        else return true;
    }

    @Override
    public void setAnswers(ArrayDeque<String> batchAnswers) {
        this.batchAnswers = batchAnswers;
    }

    private String getAnswer(String question){
        String answer = batchAnswers.poll();
        if(answer == null){
            throw new RuntimeException("Answer not provided for question: " + question);
        }
        return answer.trim();
    }
}
