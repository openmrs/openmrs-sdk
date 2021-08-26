package org.openmrs.maven.plugins.utility;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromFile;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;
import org.openmrs.maven.plugins.model.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
@Component(role = Wizard.class)
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

	private static final String INVALID_SERVER = "Invalid server Id";

	private static final String YESNO = " [Y/n]";

	private static final String NOYES = " [N/y]";

	private static final String REFERENCEAPPLICATION_2_4 = "org.openmrs.distro:referenceapplication-package:2.4";

	private static final String SDK_PROPERTIES_FILE = "SDK Properties file";

	private static final String REFAPP_OPTION_TMPL = "Reference Application %s";

	private static final String REFAPP_ARTIFACT_TMPL = "org.openmrs.distro:referenceapplication-package:%s";

	private static final String JDK_ERROR_TMPL = "\nThe JDK %s is not compatible with OpenMRS Platform %s. " +
			"Please use %s to run this server.\n\nIf you are running " +
			"in a forked mode, correct the java.home property in %s\n";

	private static final String UPGRADE_CONFIRM_TMPL = "\nThe %s %s introduces the following changes:";

	private static final String UPDATE_MODULE_TMPL = "^ Updates %s %s to %s";

	private static final String DOWNGRADE_MODULE_TMPL = "v Downgrades %s %s to %s";

	private static final String ADD_MODULE_TMPL = "+ Adds %s %s";

	private static final String DELETE_MODULE_TMPL = "- Deletes %s %s";

	private static final String NO_DIFFERENTIAL = "\nNo modules to update or add found";

	public static final String PLATFORM_VERSION_PROMPT = "You can deploy the following versions of a platform";

	public static final String DISTRIBUTION_VERSION_PROMPT = "You can deploy the following versions of distribution";

	public static final String DB_OPTION_H2 = "H2";

	public static final String DB_OPTION_MYSQL = "MySQL 5.6 and above (requires pre-installed MySQL 5.6 and above)";

	public static final String DB_OPTION_SDK_DOCKER_MYSQL = "MySQL 5.6 and above in SDK docker container (requires pre-installed Docker)";

	public static final String DB_OPTION_DOCKER_MYSQL = "Existing docker container (requires pre-installed Docker)";

	public static final String DB_OPTION_POSTGRESQL = "PostgreSQL 8.2 and above";

	public static final Map<String, String> DB_OPTIONS_MAP = new HashMap<String, String>() {{
		put("mysql", DB_OPTION_MYSQL);
		put("h2", DB_OPTION_H2);
		put("docker", DB_OPTION_SDK_DOCKER_MYSQL);
	}};

	public static final String DBNAME_URL_VARIABLE = "@DBNAME@";

	private static final int MAX_OPTIONS_SIZE = 5;

	private final PrintStream writer;

	private final LineReader reader;

	private boolean interactiveMode = true;

	private ArrayDeque<String> batchAnswers;

	public DefaultWizard() throws IOException {
		this(TerminalBuilder.terminal());
	}

	public DefaultWizard(Terminal terminal) {
		reader = LineReaderBuilder.builder().terminal(terminal).build();
		writer = new PrintStream(terminal.output());
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
	 * @param server the server to look for
	 */
	@Override
	public void promptForNewServerIfMissing(Server server) throws MojoExecutionException {
		Path omrsServerPath = Server.getServersPath();
		String newServerId = DEFAULT_SERVER_NAME;

		for (int idx = 1; omrsServerPath.resolve(newServerId).toFile().exists(); idx++) {
			newServerId = DEFAULT_SERVER_NAME + idx;
		}

		String serverId = promptForValueIfMissingWithDefault("Specify server id (-D%s)", server.getServerId(), "serverId",
				newServerId);
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
	 */
	@Override
	public String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue)
			throws MojoExecutionException {
		return promptForValueIfMissingWithDefault(message, value, parameterName, defValue, false);
	}

	public String promptForPasswordIfMissingWithDefault(String message, String value, String parameterName,
			String defValue) throws MojoExecutionException {
		return promptForValueIfMissingWithDefault(message, value, parameterName, defValue, true);
	}

	public String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defaultValue,
			boolean password) throws MojoExecutionException {
		String textToShow = String.format(message != null ? message : DEFAULT_PROMPT_TMPL, parameterName);
		if (StringUtils.isNotBlank(defaultValue)) {
			textToShow += String.format(DEFAULT_VALUE_TMPL, defaultValue);
		}

		if (value != null) {
			return value;
		} else if (!interactiveMode) {
			return getAnswer(textToShow);
		}

		String val;
		if (password) {
			val = promptForPassword(textToShow);
		} else {
			val = prompt(textToShow);
		}

		if (StringUtils.isBlank(val)) {
			val = defaultValue;
		}
		return val;
	}

	@Override
	public String promptForMissingValueWithOptions(String message, String value, String parameterName,
			List<String> options) throws MojoExecutionException {
		return promptForMissingValueWithOptions(message, value, parameterName, options, null, null);
	}

	@Override
	public String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options,
			String customMessage, String customDefault) throws MojoExecutionException {

		String defaultOption = (options == null || options.isEmpty()) ? "" : options.get(0);
		String question = String
				.format(message != null ? message : DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defaultOption);

		if (value != null) {
			return value;
		} else if (!interactiveMode) {
			return getAnswer(question);
		}

		writer.println("\n" + question + ":");

		List<Integer> choices;
		int i = 0;
		if (options != null) {
			choices = new ArrayList<>(options.size() + 1);
			for (String option : options) {
				i++;
				writer.printf((DEFAULT_OPTION_TMPL) + "%n", i, option);
				choices.add(i);
			}
		} else {
			choices = new ArrayList<>(1);
		}

		if (customMessage != null) {
			i++;
			writer.printf((DEFAULT_CUSTOM_OPTION_TMPL) + "%n", i);
			choices.add(i);
		}

		String choice = prompt(DEFAULT_CHOICE_TMPL + " [" + StringUtils.join(choices, "/") + "]");

		int chosenIndex = -1;
		if (!StringUtils.isBlank(choice)) {
			try {
				chosenIndex = Integer.parseInt(choice) - 1;
			}
			catch (NumberFormatException ignored) {
			}
		}

		if (chosenIndex >= 0) {
			if (options != null) {
				if (chosenIndex < options.size()) {
					return options.get(chosenIndex);
				} else if (chosenIndex == options.size() && customMessage != null) {
					return promptForValueIfMissingWithDefault(customMessage, null, parameterName, customDefault);
				}
			}
		}

		writer.println("\nYou must specify " + StringUtils.join(choices, " or ") + ".");
		return promptForMissingValueWithOptions(message, null, parameterName, options, customMessage, customDefault);
	}

	private String prompt(String textToShow) {
		return reader.readLine("\n" + textToShow + ": ");
	}

	public String promptForPassword(String textToShow) {
		return reader.readLine(textToShow + ": ", '*');
	}

	public String promptForPasswordIfMissing(String value, String parameter) throws MojoExecutionException {
		if (value != null) {
			return value;
		} else if (!interactiveMode) {
			return getAnswer(parameter);
		}

		return promptForPassword(String.format("\n" + DEFAULT_PROMPT_TMPL, parameter));
	}

	public void showMessage(String textToShow) {
		writer.println("\n" + textToShow);
	}

	public void showMessageNoEOL(String textToShow) {
		writer.print(textToShow);
		writer.flush();
	}

	@Override
	public void showError(String textToShow) {
		writer.println("\n[ERROR]" + textToShow);
	}

	/**
	 * Prompt for a value if it not set, and default value is NOT set
	 *
	 * @param value
	 * @param parameterName
	 * @return
	 */
	@Override
	public String promptForValueIfMissing(String value, String parameterName) throws MojoExecutionException {
		return promptForValueIfMissingWithDefault(null, value, parameterName, EMPTY_STRING);
	}

	/**
	 * Print dialog Yes/No
	 *
	 * @param text - text to display
	 * @return true if the user chose yes, false otherwise
	 */
	@Override
	public boolean promptYesNo(String text) throws MojoExecutionException {
		return promptYesNo(text, true);
	}

	/**
	 * @param text         Text to display
	 * @param defaultValue Value to use if response is empty
	 * @return true if the user selected yes, false if the user selected no, and the default value if none was used
	 */
	public boolean promptYesNo(String text, boolean defaultValue) throws MojoExecutionException {
		String yesNo;
		if (interactiveMode) {
			yesNo = prompt(text.concat(defaultValue ? YESNO : NOYES));
		} else {
			yesNo = getAnswer(text);
		}

		if (yesNo.isEmpty()) {
			return true;
		}

		return Character.toLowerCase(yesNo.charAt(0)) == 'y';
	}

	/**
	 * Check if value is submit
	 *
	 * @param value the value to check
	 * @return true if this string should be treated as a "yes"
	 */
	@Override
	public boolean checkYes(String value) {
		return Boolean.parseBoolean(value) || Character.toLowerCase(value.charAt(0)) == 'y';
	}

	/**
	 * Get path to server by serverId and prompt if missing
	 *
	 * @return
	 * @throws MojoFailureException
	 */
	@Override
	public String promptForExistingServerIdIfMissing(String serverId) throws MojoExecutionException {
		Path omrsHome = Server.getServersPath();
		List<String> servers = getListOfServers();
		if (servers.isEmpty()) {
			throw new MojoExecutionException("There are no servers available");
		}

		serverId = promptForMissingValueWithOptions("You have the following servers:", serverId, "serverId", servers);
		if (serverId.equals(NONE)) {
			throw new MojoExecutionException(INVALID_SERVER);
		}

		File serverPath = omrsHome.resolve(serverId).toFile();
		if (!serverPath.exists()) {
			throw new MojoExecutionException(
					"There is no server with server id \"" + serverId + "\". Please create it first by using openmrs-sdk:setup.");
		}

		return serverId;
	}

	@Override
	public String promptForPlatformVersionIfMissing(String version, List<String> versions) throws MojoExecutionException {
		return promptForMissingValueWithOptions(PLATFORM_VERSION_PROMPT,
				version, "version", versions, "Please specify platform version", null);
	}

	@Override
	public String promptForPlatformVersion(List<String> versions) throws MojoExecutionException {
		return promptForMissingValueWithOptions(PLATFORM_VERSION_PROMPT,
				null, "version", versions, "Please specify platform version", null);
	}

	@Override
	public void promptForJavaHomeIfMissing(Server server) throws MojoExecutionException {
		String javaHome = server.getJavaHome();
		if (StringUtils.isNotBlank(javaHome)) {
			if (isJavaHomeValid(javaHome)) {
				addJavaHomeToSdkProperties(javaHome);
				return;
			} else {
				throw new MojoExecutionException("The specified -DjavaHome, \"" + javaHome + "\", is invalid");
			}
		}

		if (interactiveMode) {
			List<String> options = new ArrayList<>();
			options.add("JAVA_HOME (currently: " + System.getProperty("java.home") + ")");
			options.addAll(getJavaHomeOptions());

			String selectedOption = promptForMissingValueWithOptions(SDKConstants.OPENMRS_SDK_JDK_OPTION,
					server.getJavaHome(), "path", options, SDKConstants.OPENMRS_SDK_JDK_CUSTOM, null);

			Version platformVersion = new Version(server.getPlatformVersion());
			String requiredJdkVersion;
			String notRecommendedJdkVersion = "Not recommended";
			if (server.getPlatformVersion().startsWith("1.")) {
				requiredJdkVersion = "1.7";
				notRecommendedJdkVersion = "1.8";
			} else if (platformVersion.getMajorVersion() == 2 && platformVersion.getMinorVersion() < 4) {
				requiredJdkVersion = "1.8";
			} else {
				requiredJdkVersion = "1.8 or above";
			}

			// Use default JAVA_HOME
			if (selectedOption.equals(options.get(0))) {
				String jdkUnderSpecifiedPathVersion = determineJavaVersionFromPath(System.getProperty("java.home"));
				if (isPlatform24OrNewer(platformVersion) && isJava8orAbove(jdkUnderSpecifiedPathVersion)) {
					server.setJavaHome(null);
				} else if (System.getProperty("java.version").startsWith(requiredJdkVersion)) {
					server.setJavaHome(null);
				} else if (System.getProperty("java.version").startsWith(notRecommendedJdkVersion)) {
					boolean isSelectJdk7 = promptYesNo(
							"It is not recommended to run OpenMRS platform " + server.getPlatformVersion()
									+ " on JDK 8. Would you like to select the recommended JDK 7 instead?");
					if (isSelectJdk7) {
						promptForJavaHomeIfMissing(server);
					} else {
						server.setJavaHome(null);
					}
				} else {
					showMessage("Your JAVA_HOME version doesn't fit platform requirements:");
					showMessage("JAVA_HOME version: " + System.getProperty("java.version"));
					showMessage("Required: " + requiredJdkVersion);
					promptForJavaHomeIfMissing(server);
				}
			} else if (!isJavaHomeValid(selectedOption)) {
				writer.println(SDKConstants.OPENMRS_SDK_JDK_CUSTOM_INVALID);
				promptForJavaHomeIfMissing(server);
			} else {
				String jdkUnderSpecifiedPathVersion = determineJavaVersionFromPath(selectedOption);
				if (isPlatform24OrNewer(platformVersion) && isJava8orAbove(jdkUnderSpecifiedPathVersion)) {
					server.setJavaHome(selectedOption);
					addJavaHomeToSdkProperties(selectedOption);
				} else if (jdkUnderSpecifiedPathVersion.startsWith(requiredJdkVersion)) {
					server.setJavaHome(selectedOption);
					addJavaHomeToSdkProperties(selectedOption);
				} else if (jdkUnderSpecifiedPathVersion.startsWith(notRecommendedJdkVersion)) {
					boolean isSelectJdk7 = promptYesNo(
							"It is not recommended to run OpenMRS platform " + server.getPlatformVersion()
									+ " on JDK 8. Would you like to select the recommended JDK 7 instead?");
					if (isSelectJdk7) {
						promptForJavaHomeIfMissing(server);
					} else {
						server.setJavaHome(null);
					}
				} else {
					showMessage("JDK in custom path (" + selectedOption + ") doesn't match platform requirements:");
					showMessage("JDK version: " + jdkUnderSpecifiedPathVersion);
					showMessage("Required: " + requiredJdkVersion);
					promptForJavaHomeIfMissing(server);
				}
			}
		}
	}

	private boolean isPlatform24OrNewer(Version platformVersion) {
		return platformVersion.getMajorVersion() > 2
				|| (platformVersion.getMajorVersion() == 2 && platformVersion.getMinorVersion() > 3);
	}

	private boolean isJava8orAbove(String javaVersion) {
		if (javaVersion.startsWith("1.8")) {
			return true;
		}
		int pos = javaVersion.indexOf('.');
		String version = javaVersion.substring(0, pos);
		return (Integer.parseInt(version) > 8);
	}

	private String determineJavaVersionFromPath(String path) throws MojoExecutionException {
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
			List<String> output = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8);
			result = StringUtils.join(output.iterator(), "\n");
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to fetch Java version from \"" + path + "\" " + e.getMessage(), e);
		}

		Pattern p = Pattern.compile(".*\"(.*)\".*");
		Matcher m = p.matcher(result);
		if (m.find()) {
			return m.group(1);
		} else {
			throw new MojoExecutionException(
					"Failed to fetch Java version from \"" + path + "\". 'java -version' returned " + result);
		}
	}

	private void addJavaHomeToSdkProperties(String path) throws MojoExecutionException {
		Path sdkPropertiesPath = Server.getServersPath().resolve(SDKConstants.OPENMRS_SDK_PROPERTIES).toAbsolutePath();
		Properties sdkProperties = getSdkProperties();
		List<String> jdkPaths = getJavaHomeOptions();

		if (!jdkPaths.contains(path)) {
			if (jdkPaths.size() == 5) {
				jdkPaths.set(4, path);
			} else {
				jdkPaths.add(path);
			}

			Collections.sort(jdkPaths);

			String updatedProperty = StringUtils.join(jdkPaths.iterator(), ", ");
			sdkProperties.setProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS, updatedProperty);
			savePropertiesChangesToFile(sdkProperties, sdkPropertiesPath.toFile());
		}
	}

	private Properties getSdkProperties() throws MojoExecutionException {
		Path sdkPropertiesPath = Server.getServersPath().resolve(SDKConstants.OPENMRS_SDK_PROPERTIES).toAbsolutePath();
		File sdkPropertiesFile = sdkPropertiesPath.toFile();

		if (!sdkPropertiesFile.exists()) {
			try {
				if (!sdkPropertiesFile.createNewFile()) {
					throw new MojoExecutionException("Failed to create SDK properties file in: \"" + sdkPropertiesPath + "\"");
				}
			}
			catch (IOException e) {
				throw new MojoExecutionException(
						"Failed to create SDK properties file in: \"" + sdkPropertiesPath + "\"");
			}

			return new Properties();
		}

		return loadPropertiesFromFile(sdkPropertiesFile);
	}

	private List<String> getJavaHomeOptions() throws MojoExecutionException {
		Properties sdkProperties = getSdkProperties();
		List<String> result = new ArrayList<>();

		if (interactiveMode) {
			String jdkHomeProperty = sdkProperties.getProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS);
			if (jdkHomeProperty != null) {
				for (String path : jdkHomeProperty.split("\\s*,\\s*")) {
					if (isJavaHomeValid(path)) {
						result.add(path);
					}
				}

				// Save properties
				Collections.sort(result);
				String updatedProperty = StringUtils.join(result.iterator(), ", ");
				sdkProperties.setProperty(SDKConstants.OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS, updatedProperty);

				File sdkPropertiesFile = Server.getServersPath().resolve(SDKConstants.OPENMRS_SDK_PROPERTIES).toFile();
				savePropertiesChangesToFile(sdkProperties, sdkPropertiesFile);

				return result;
			} else {
				return new ArrayList<>(0);
			}
		} else {
			return new ArrayList<>(0);
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

	private void savePropertiesChangesToFile(Properties properties, File file)
			throws MojoExecutionException {
		try (OutputStream fos = new FileOutputStream(file)) {
			properties.store(fos, DefaultWizard.SDK_PROPERTIES_FILE + ":");
		}
		catch (IOException e) {
			throw new MojoExecutionException(
					"An exception occurred while saving properties to " + file.getAbsolutePath() + " " + e.getMessage(), e);
		}
	}

	@Override
	public void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper) throws MojoExecutionException {
		promptForRefAppVersionIfMissing(server, versionsHelper, null);
	}

	@Override
	public void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper, String customMessage)
			throws MojoExecutionException {
		if (server.getVersion() == null) {
			String choice = promptForRefAppVersion(versionsHelper);
			Artifact distro = DistroHelper.parseDistroArtifact(choice, versionsHelper);
			server.setVersion(distro.getVersion());
			server.setDistroArtifactId(distro.getArtifactId());
			server.setDistroGroupId(distro.getGroupId());
		}
	}

	public String promptForRefAppVersion(VersionsHelper versionsHelper) throws MojoExecutionException {
		return promptForRefAppVersion(versionsHelper, null);
	}

	@Override
	public String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion,
			String distroName, VersionsHelper versionsHelper) throws MojoExecutionException {
		return promptForDistroVersion(distroGroupId, distroArtifactId, distroVersion, distroName, versionsHelper, null);
	}

	@Override
	public String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion,
			String distroName, VersionsHelper versionsHelper, @Nullable String customMessage) throws MojoExecutionException {
		final String optionTemplate = distroName + " %s";
		final String artifacttemplate = distroGroupId + ":" + distroArtifactId + ":" + "%s";

		Set<String> versions = new LinkedHashSet<>(versionsHelper
				.getSuggestedVersions(SDKConstants.getDistroModule(distroGroupId, distroArtifactId, distroVersion),
						MAX_OPTIONS_SIZE));
		Map<String, String> optionsMap = getDistroVersionsOptionsMap(versions, versionsHelper, optionTemplate,
				artifacttemplate);

		return promptForVersion(optionsMap, customMessage);
	}

	public String promptForRefAppVersion(VersionsHelper versionsHelper, @Nullable String customMessage)
			throws MojoExecutionException {
		Set<String> versions = new LinkedHashSet<>(
				versionsHelper.getSuggestedVersions(SDKConstants.getReferenceModule("2.3.1"), MAX_OPTIONS_SIZE));
		versions.addAll(SDKConstants.SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER);
		Map<String, String> optionsMap = getDistroVersionsOptionsMap(versions, versionsHelper, REFAPP_OPTION_TMPL,
				REFAPP_ARTIFACT_TMPL);
		return promptForVersion(optionsMap, customMessage);
	}

	private String promptForVersion(Map<String, String> optionsMap, @Nullable String customMessage)
			throws MojoExecutionException {
		String message = customMessage != null ? customMessage : DISTRIBUTION_VERSION_PROMPT;
		String version = promptForMissingValueWithOptions(message,
				null, "distribution artifact", Lists.newArrayList(optionsMap.keySet()), "Please specify %s",
				REFERENCEAPPLICATION_2_4);

		String artifact = optionsMap.get(version);
		if (artifact != null) {
			return artifact;
		} else {
			return version;
		}
	}

	private Map<String, String> getDistroVersionsOptionsMap(Set<String> versions, VersionsHelper versionsHelper,
			String optionTemplate, String artifactTemplate) {
		Map<String, String> optionsMap = new LinkedHashMap<>();

		List<ArtifactVersion> artifactVersions = new ArrayList<>();
		for (String version : versions) {
			artifactVersions.add(new DefaultArtifactVersion(version));
		}
		for (String version : versionsHelper.getSuggestedVersions(artifactVersions, MAX_OPTIONS_SIZE)) {
			optionsMap.put(String.format(optionTemplate, version), String.format(artifactTemplate, version));
			if (optionsMap.size() == MAX_OPTIONS_SIZE)
				break;
		}
		return optionsMap;
	}

	@Override
	public void promptForDb(Server server, DockerHelper dockerHelper, boolean h2supported, String dbDriver,
			String dockerHost) throws MojoExecutionException {
		String db = null;

		if (StringUtils.isNotBlank(dbDriver)) {
			db = DB_OPTIONS_MAP.get(dbDriver);
		}

		List<String> options = new ArrayList<>(5);
		if (h2supported) {
			options.add(DB_OPTION_H2);
		}

		options.addAll(Lists.newArrayList(DB_OPTION_MYSQL, DB_OPTION_SDK_DOCKER_MYSQL, DB_OPTION_DOCKER_MYSQL));
		boolean isPlatform24OrNewer = isPlatform24OrNewer(new Version(server.getPlatformVersion()));
		if (isPlatform24OrNewer) {
			options.add(DB_OPTION_POSTGRESQL);
		}

		db = promptForMissingValueWithOptions("Which database would you like to use?", db, null, options);
		switch (db) {
			case DB_OPTION_H2:
				server.setDbDriver(SDKConstants.DRIVER_H2);
				if (server.getDbUri() == null) {
					server.setDbUri(SDKConstants.URI_H2);
				}

				server.setDbUser("root");
				server.setDbPassword("root");
				break;
			case DB_OPTION_MYSQL:
				promptForDbUri(server, isPlatform24OrNewer ? SDKConstants.DRIVER_MYSQL : SDKConstants.DRIVER_MYSQL_OLD);
				break;
			case DB_OPTION_SDK_DOCKER_MYSQL:
				promptForDockerizedSdkMysql(server, dockerHelper, dockerHost);
				break;
			case DB_OPTION_DOCKER_MYSQL:
				promptForDockerizedDb(server, dockerHelper, dockerHost);
				break;
			case DB_OPTION_POSTGRESQL:
				promptForDbUri(server, SDKConstants.DRIVER_POSTGRESQL);
				break;
		}
	}

	private void promptForDbUri(Server server, String driver) throws MojoExecutionException {
		if (server.getDbDriver() == null) {
			server.setDbDriver(driver);
		}

		String uriTemplate;
		if (server.isMySqlDb()) {
			uriTemplate = SDKConstants.URI_MYSQL;
		} else if (server.isPostgreSqlDb()) {
			uriTemplate = SDKConstants.URI_POSTGRESQL;
		} else {
			return;
		}

		String dbUri = promptForValueIfMissingWithDefault(
				"The distribution requires a " +
						(server.isMySqlDb() ? "MySQL" : "PostgreSQL") +
						" database. Please specify database uri (-D%s)",
				server.getDbUri(), "dbUri", uriTemplate);

		if (server.isMySqlDb()) {
			dbUri = addMySQLParamsIfMissing(dbUri);
		} else if (server.isPostgreSqlDb()) {
			dbUri = addPostgreSQLParamsIfMissing(dbUri);
		}

		dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());

		server.setDbUri(dbUri);
		promptForDbCredentialsIfMissing(server);
	}

	public void promptForDockerizedSdkMysql(Server server, DockerHelper dockerHelper, String dockerHost)
			throws MojoExecutionException {
		promptForDockerHostIfMissing(dockerHelper, dockerHost);

		if (server.getDbDriver() == null) {
			if (isPlatform24OrNewer(new Version(server.getPlatformVersion()))) {
				server.setDbDriver(SDKConstants.DRIVER_MYSQL);
			} else {
				server.setDbDriver(SDKConstants.DRIVER_MYSQL_OLD);
			}
		}

		String dbUri = getDefaultDbUri(server, dockerHelper);
		dbUri = addMySQLParamsIfMissing(dbUri);

		server.setDbUri(dbUri);
		server.setDbUser(DockerHelper.DOCKER_MYSQL_USERNAME);
		server.setDbPassword(DockerHelper.DOCKER_MYSQL_PASSWORD);
		server.setContainerId(DockerHelper.DOCKER_DEFAULT_CONTAINER_ID);

		dockerHelper.createMySqlContainer(DockerHelper.DOCKER_DEFAULT_CONTAINER_ID, DockerHelper.DOCKER_MYSQL_PORT);
		dockerHelper.runDbContainer(server.getContainerId(), server.getDbUri(), server.getDbUser(), server.getDbPassword());
	}

	private String getDefaultDbUri(Server server, DockerHelper dockerHelper) throws MojoExecutionException {
		String dbUri = SDKConstants.URI_MYSQL.replace("3306", DockerHelper.DOCKER_MYSQL_PORT);
		//In case of using the Docker Machine, which gets assigned an IP different than the host
		if (dockerHelper.getDockerHost().startsWith("tcp://")) {
			try {
				URI uri = new URI(dockerHelper.getDockerHost());
				dbUri = dbUri.replace("localhost", uri.getHost());
			}
			catch (URISyntaxException e) {
				throw new MojoExecutionException("URI for Docker host, \"" + dockerHelper.getDockerHost() +
						"\" is not valid " + e.getMessage(), e);
			}
		}
		dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());
		return dbUri;
	}

	private void promptForDockerHostIfMissing(DockerHelper dockerHelper, String dockerHost) throws MojoExecutionException {
		// If user specified -DdockerHost
		if (StringUtils.isNotEmpty(dockerHost)) {
			// If specified without value
			if (dockerHost.equals("true")) {
				// Reset state of sdk.properties dockerHost property
				showMessage("Attempting to find default docker host address...");
				dockerHelper.saveDockerHost(determineDefaultDockerHost());
			}
			// If specified with value
			else {
				// Assign that value to sdk.properties
				dockerHelper.saveDockerHost(dockerHost);
			}
		} else if (StringUtils.isBlank(dockerHelper.getDockerHost())) {
			showMessage("-DdockerHost is not specified in batch mode. Attempting to find default docker host address...");
			dockerHelper.saveDockerHost(determineDefaultDockerHost());
		}
	}

	private String determineDefaultDockerHost() {
		String host = null;
		if (SystemUtils.IS_OS_LINUX) {
			showMessage("Trying default UNIX socket as docker host address...");
			host = DockerHelper.DEFAULT_DOCKER_HOST_UNIX_SOCKET;
		} else if (SystemUtils.IS_OS_WINDOWS) {
			host = determineWindowsDockerHost();
			if (host == null) {
				// There is no Docker at any default address
				host = DockerHelper.DEFAULT_HOST_DOCKER_FOR_WINDOWS;
			}
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			host = determineDockerToolboxHost();
			if (host == null) {
				showMessage("Trying default UNIX socket as docker host address...");
				host = DockerHelper.DEFAULT_DOCKER_HOST_UNIX_SOCKET;
			}
		}
		return host;
	}

	private String determineWindowsDockerHost() {
		if (determineDockerForWindowsHost()) {
			return DockerHelper.DEFAULT_HOST_DOCKER_FOR_WINDOWS;
		} else {
			return determineDockerToolboxHost();
		}
	}

	// This method checks if there is HTTP response at default Docker for Windows host address
	private boolean determineDockerForWindowsHost() {
		showMessage("Checking \"Docker for Windows\"");
		String hostUrl = DockerHelper.DEFAULT_HOST_DOCKER_FOR_WINDOWS;
		try {
			URL url = new URL(hostUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			connection.disconnect();
			return true;
		}
		catch (IOException e) {
			showMessage("\"Docker for Windows\" is not running.");
			return false;
		}
	}

	private String determineDockerToolboxHost() {
		showMessage("Checking \"Docker Toolbox\"");
		showMessage("Running `docker-machine url` to determine the docker host...");
		try {
			Process process = new ProcessBuilder("docker-machine", "url").redirectErrorStream(true).start();
			List<String> lines = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8);
			process.waitFor();
			//if success
			if (process.exitValue() == 0) {
				String url = lines.get(0);
				try {
					new URL(url);
				}
				catch (MalformedURLException e) {
					showMessage(
							"Failed to fetch host address from \"Docker Toolbox\"'s machine, which responded with: '" + url
									+ "'");
					return null;
				}
				showMessage("Your docker-machine url is: " + url);
				return url;
			}
		}
		catch (Exception e) {
			showMessage("Failed checking \"Docker Toolbox\"");
		}
		return null;
	}

	public void promptForDockerizedDb(Server server, DockerHelper dockerHelper, String dockerHost)
			throws MojoExecutionException {
		promptForDockerHostIfMissing(dockerHelper, dockerHost);

		String containerId = prompt(
				"Please specify your container id/name/label (you can get it using command `docker ps -a`)");
		String username = prompt("Please specify DB username");
		String password = promptForPassword("Please specify DB password");

		String defaultDbUri = getDefaultDbUri(server, dockerHelper);

		String dbUri = promptForValueIfMissingWithDefault(
				"Please specify database uri (-D%s)", server.getDbUri(), "dbUri", defaultDbUri);

		if (dbUri.startsWith("jdbc:mysql:")) {
			if (isPlatform24OrNewer(new Version(server.getPlatformVersion()))) {
				server.setDbDriver(SDKConstants.DRIVER_MYSQL);
			} else {
				server.setDbDriver(SDKConstants.DRIVER_MYSQL_OLD);
			}

			dbUri = addMySQLParamsIfMissing(dbUri);
		}

		dbUri = dbUri.replace(DBNAME_URL_VARIABLE, server.getServerId());

		server.setDbUri(dbUri);
		server.setDbUser(username);
		server.setDbPassword(password);
		server.setContainerId(containerId);

		dockerHelper.runDbContainer(containerId, server.getDbUri(), username, password);
	}

	private String getDefaultUri(String dockerHost) {
		if (SystemUtils.IS_OS_LINUX) {
			return SDKConstants.URI_MYSQL;
		} else {
			int beginIndex = dockerHost.indexOf("//");
			int endIndex = dockerHost.lastIndexOf(":");
			String dockerMachineIp = dockerHost.substring(beginIndex + 2, endIndex);
			return String.format(SDKConstants.URI_MYSQL_DOCKER, dockerMachineIp);
		}
	}

	@Override
	public void promptForDbCredentialsIfMissing(Server server) throws MojoExecutionException {
		String defaultUser = "root";
		if (server.isPostgreSqlDb()) {
			defaultUser = "postgres";
		}

		String user = promptForValueIfMissingWithDefault(
				"Please specify database username (-D%s)",
				server.getDbUser(), "dbUser", defaultUser);
		server.setDbUser(user);
		//set password
		String dbPassword = promptForPasswordIfMissingWithDefault(
				"Please specify database password (-D%s)",
				server.getDbPassword(), "dbPassword", "");
		server.setDbPassword(dbPassword);
	}

	/**
	 * Get servers with recently used first
	 *
	 * @return
	 */
	@Override
	public List<String> getListOfServers() throws MojoExecutionException {
		Path openMRS = Server.getServersPath();
		Map<Long, String> sortedMap = new TreeMap<>(Collections.reverseOrder());
		try (DirectoryStream<Path> subDirectories = Files.newDirectoryStream(openMRS, new DirectoryStream.Filter<Path>() {

			@Override
			public boolean accept(Path entry) {
				return entry.toFile().isDirectory();
			}
		})) {
			for (Path dir : subDirectories) {
				if (Server.hasServerConfig(dir)) {
					sortedMap.put(dir.toFile().lastModified(), dir.getFileName().toString());
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Exception occurred while trying to read list of servers: " + e.getMessage(),
					e);
		}

		return new ArrayList<>(sortedMap.values());
	}

	public String addMySQLParamsIfMissing(String dbUri) throws MojoExecutionException {
		URIBuilder uri = addDefaultParamsIfMissing(dbUri);
		uri.setParameter("sessionVariables", "default_storage_engine=InnoDB");

		return "jdbc:" + uri;
	}

	public String addPostgreSQLParamsIfMissing(String dbUri) throws MojoExecutionException {
		return "jdbc:" + addDefaultParamsIfMissing(dbUri);
	}

	private URIBuilder addDefaultParamsIfMissing(String dbUri) throws MojoExecutionException {
		String noJdbc = dbUri.substring(5);

		URIBuilder uri;
		try {
			uri = new URIBuilder(noJdbc);
		}
		catch (URISyntaxException e) {
			throw new MojoExecutionException("Database URI \"" + noJdbc + "\" is not a valid URI " + e.getMessage(),  e);
		}
		uri.setParameter("autoReconnect", "true");
		uri.setParameter("useUnicode", "true");
		uri.setParameter("characterEncoding", "UTF-8");

		return uri;
	}

	@Override
	public void showJdkErrorMessage(String jdk, String platform, String recommendedJdk, String pathToServerProperties) {
		writer.printf((JDK_ERROR_TMPL) + "%n", jdk, platform, recommendedJdk, pathToServerProperties);
	}

	/**
	 * Show confirmation prompt if there is any change besides updating modules with SNAPSHOT versions
	 *
	 * @return
	 */
	@Override
	public boolean promptForConfirmDistroUpgrade(UpgradeDifferential upgradeDifferential, Server server,
			DistroProperties distroProperties) throws MojoExecutionException {
		if (upgradeDifferential.isEmpty()) {
			showMessage(NO_DIFFERENTIAL);
			return false;
		}

		boolean needConfirmation = false;

		if (upgradeDifferential.getPlatformArtifact() != null) {
			needConfirmation = showUpdateHeader(distroProperties, needConfirmation);
			writer.printf(
					(upgradeDifferential.isPlatformUpgraded() ? UPDATE_MODULE_TMPL : DOWNGRADE_MODULE_TMPL) + "%n",
					upgradeDifferential.getPlatformArtifact().getArtifactId(),
					server.getPlatformVersion(),
					upgradeDifferential.getPlatformArtifact().getVersion());
		}
		for (Entry<Artifact, Artifact> updateEntry : upgradeDifferential.getUpdateOldToNewMap().entrySet()) {
			//update map should contain entry with equal versions only when they are same snapshots
			//(e.g. update 'appui 0.2-SNAPSHOT' to 'appui 0.2-SNAPSHOT')
			//updating to same SNAPSHOT doesn't require confirmation, they are not shown
			if (!updateEntry.getKey().getVersion().equals(updateEntry.getValue().getVersion())) {
				needConfirmation = showUpdateHeader(distroProperties, needConfirmation);
				writer.printf((UPDATE_MODULE_TMPL) + "%n",
						updateEntry.getKey().getArtifactId(),
						updateEntry.getKey().getVersion(),
						updateEntry.getValue().getVersion());
			}
		}

		for (Entry<Artifact, Artifact> downgradeEntry : upgradeDifferential.getDowngradeNewToOldMap().entrySet()) {
			if (!downgradeEntry.getKey().getVersion().equals(downgradeEntry.getValue().getVersion())) {
				needConfirmation = showUpdateHeader(distroProperties, needConfirmation);
				writer.printf((DOWNGRADE_MODULE_TMPL) + "%n",
						downgradeEntry.getKey().getArtifactId(),
						downgradeEntry.getKey().getVersion(),
						downgradeEntry.getValue().getVersion());
			}
		}

		for (Artifact addArtifact : upgradeDifferential.getModulesToAdd()) {
			needConfirmation = showUpdateHeader(distroProperties, needConfirmation);
			writer.printf((ADD_MODULE_TMPL) + "%n",
					addArtifact.getArtifactId(),
					addArtifact.getVersion());
		}

		for (Artifact deleteArtifact : upgradeDifferential.getModulesToDelete()) {
			needConfirmation = showUpdateHeader(distroProperties, needConfirmation);
			writer.printf((DELETE_MODULE_TMPL) + "%n",
					deleteArtifact.getArtifactId(),
					deleteArtifact.getVersion());
		}

		if (needConfirmation) {
			return promptYesNo(String.format("Would you like to apply those changes to '%s'?", server.getServerId()));
		} else
			return true;
	}

	private boolean showUpdateHeader(DistroProperties distroProperties, boolean needConfirmation) {
		if (!needConfirmation) {
			writer.printf((UPGRADE_CONFIRM_TMPL) + "%n", distroProperties.getName(), distroProperties.getVersion());
		}
		return true;
	}

	@Override
	public void setAnswers(ArrayDeque<String> batchAnswers) {
		this.batchAnswers = batchAnswers;
	}

	private String getAnswer(String question) throws MojoExecutionException {
		String answer = batchAnswers.poll();
		if (answer == null) {
			throw new MojoExecutionException("Answer not provided for question: " + question);
		}
		return answer.trim();
	}
}
