package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Parser;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.NodeDistro;
import org.openmrs.maven.plugins.model.PackageJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import javax.annotation.Nullable;

public class OwaHelper {

	public static final String PACKAGE_JSON_FILENAME = "package.json";
	public static final String NODE_VERSION_KEY = "node";
	public static final String NPM_VERSION_KEY = "npm";

	private static final String OWA_PACKAGE_EXTENSION = ".owa";

	private MavenSession session;

	private File installationDir;

	private MavenProject mavenProject;

	private BuildPluginManager pluginManager;

	private Wizard wizard;

	private static final Logger logger = LoggerFactory.getLogger(OwaHelper.class);

	//enable chaining
	public OwaHelper setInstallationDir(File installationDir) {
		this.installationDir = installationDir;
		return this;
	}

	public OwaHelper(){}

	public OwaHelper(MavenSession session, MavenProject mavenProject, BuildPluginManager pluginManager, Wizard wizard) {
		this.session = session;
		this.mavenProject = mavenProject;
		this.pluginManager = pluginManager;
		this.wizard = wizard;
	}

	public void setWizard(Wizard wizard) {
		this.wizard = wizard;
	}

	public void setMavenProject(MavenProject mavenProject) {
		this.mavenProject = mavenProject;
	}

	public void setPluginManager(BuildPluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	public void setSession(MavenSession session) {
		this.session = session;
	}

	public void downloadOwa(File owaDir, Artifact owa, ModuleInstaller moduleInstaller)
			throws MojoExecutionException {
		boolean isSysAdmin = false;
		if (owa.getArtifactId().startsWith("openmrs-owa-")) {
			owa.setArtifactId(owa.getArtifactId().substring(12));
			isSysAdmin = owa.getArtifactId().equalsIgnoreCase("sysadmin");
		}

		moduleInstaller.installModule(owa, owaDir.getAbsolutePath());
		File owaFile = new File(owaDir, owa.getArtifactId() + "-" + owa.getVersion() + "." + owa.getType());
		if (!owaFile.exists()) {
			throw new MojoExecutionException("Unable to download OWA " + owa + " from Maven");
		}

		File renamedFile;
		if (!isSysAdmin) {
			renamedFile = new File(owaDir, owa.getArtifactId() + OWA_PACKAGE_EXTENSION);
		} else {
			renamedFile = new File(owaDir, "SystemAdministration" + OWA_PACKAGE_EXTENSION);
		}

		if (renamedFile.exists()) {
			renamedFile.delete();
		}

		try {
			FileUtils.moveFile(owaFile, renamedFile);
		}
		catch (IOException ioe) {
			throw new MojoExecutionException("Unable to move OWA file to " + renamedFile, ioe);
		}
	}

	public void createOwaProject() throws MojoExecutionException {
		File owaDir = installationDir != null ? installationDir : prepareOwaDir();
		owaDir.mkdirs();

		wizard.showMessage("Creating OWA project in " + owaDir.getAbsolutePath() + "...\n");

		boolean useSystemNode = resolveNodeAndNpm(null, null, owaDir.getAbsolutePath());

		installYeomanGenerator(useSystemNode, owaDir);

		try {
			runYeomanGenerator(useSystemNode, owaDir);

			if (!useSystemNode) {
				addHelperScripts(owaDir.getAbsolutePath());
			}
		} catch (IOException | InterruptedException e) {
			throw new MojoExecutionException("Failed running yeoman " + e.getMessage(), e);
		}
	}

	public void installNodeModules(boolean useSystemNode) throws MojoExecutionException {
		if (useSystemNode) {
			runSystemNpmCommandWithArgs(Arrays.asList("install", "--no-optional"));
		}
		else {
			runLocalNpmCommandWithArgs(Arrays.asList("install", "--no-optional"));
		}
	}

	private void installYeomanGenerator(boolean useSystemNode, File owaDir) throws MojoExecutionException {
		if (useSystemNode) {
			runSystemNpmCommandWithArgs(Arrays.asList("install", "-g", "yo", "generator-openmrs-owa", "--no-optional"));
		} else {
			runLocalNpmCommandWithArgs(Arrays.asList("install", "-g", "yo", "generator-openmrs-owa", "--no-optional"), owaDir.getAbsolutePath());
		}
	}

	private File prepareOwaDir() throws MojoExecutionException {
		File owaDir = new File(System.getProperty("user.dir"));
		boolean pathCorrect = false;
		do {
			String owaDirValue = wizard.promptForValueIfMissingWithDefault("Please specify a directory for OWA project (a relative or absolute path)", owaDir.getName(), null, null);
			File owaDirPath = new File(owaDirValue);
			if (owaDirPath.isAbsolute()) {
				owaDir = owaDirPath;
			} else {
				owaDir = new File(owaDir, owaDirValue);
			}

			if (owaDir.exists()) {
				if (!owaDir.isDirectory()) {
					wizard.showError("The specified path " + owaDir.getAbsolutePath() + " is not a directory!");
				} else if (owaDir.listFiles().length != 0) {
					wizard.showMessage("The specified directory " + owaDir.getAbsolutePath() + " is not empty.");
					boolean create = wizard.promptYesNo("Are you sure you want to create a new OWA project in it?");
					if (create) {
						//in case someone just deleted the dir after seeing the prompt
						if (!owaDir.exists()) {
							owaDir.mkdir();
						}
						pathCorrect = true;
					}
				}
			} else {
				owaDir.mkdir();
				pathCorrect = true;
			}
		} while (!pathCorrect);

		return owaDir;
	}

	private void addHelperScripts(String path) throws MojoExecutionException {
		File npmCmd = new File(path, "npm.cmd");
		URL npmCmdSrc = getClass().getClassLoader().getResource("npm.cmd");
		File npm = new File(path, "npm");
		URL npmSrc = getClass().getClassLoader().getResource("npm");
		try {
			FileUtils.copyURLToFile(npmSrc, npm);
			FileUtils.copyURLToFile(npmCmdSrc, npmCmd);
			npm.setExecutable(true);
			npmCmd.setExecutable(true);
		} catch (IOException e) {
			throw new MojoExecutionException("Could not copy helper scripts to OWA directory " + e.getMessage(), e);
		}

	}

	private void runYeomanGenerator(boolean useSystemNode, File directory) throws InterruptedException, IOException {
		ProcessBuilder builder = new ProcessBuilder()
				.directory(directory)
				.command(getYoExecutable(useSystemNode, directory))
				.redirectErrorStream(true)
				.inheritIO();

		Process process = builder.start();
		process.waitFor();

	}

	private String[] getYoExecutable(boolean useSystemNode, File directory) {
		if (useSystemNode) {
			if (SystemUtils.IS_OS_WINDOWS) {
				return new String[] {"yo.cmd", "openmrs-owa"};
			} else {
				return new String[] {"yo", "openmrs-owa"};
			}
		} else {
			if (SystemUtils.IS_OS_WINDOWS) {
				return new String[] {new File(directory, "node\\node.exe").getAbsolutePath(), "node\\node_modules\\yo\\lib\\cli.js", "openmrs-owa"};
			} else {
				return new String[] {"node/node", "lib/node_modules/yo/lib/cli.js", "openmrs-owa"};
			}
		}
	}

	public String getSystemNpmVersion() {
		String npmExecutable = getNpmSystemExecutable();
		String npm = runProcessAndGetFirstResponseLine(npmExecutable, "-v");
		return StringUtils.isNotBlank(npm) ? npm : null;
	}

	public String getSystemNodeVersion() {
		String node = runProcessAndGetFirstResponseLine("node", "-v");
		return formatNodeVersion(node);
	}

	public String getProjectNodeVersion() {
		String node;
		if (SystemUtils.IS_OS_WINDOWS) {
			node = runProcessAndGetFirstResponseLine("node\\node.exe", "-v");
		} else {
			node = runProcessAndGetFirstResponseLine("node/node", "-v");
		}
		return formatNodeVersion(node);
	}

	public String getProjectNpmVersion() {
		String npm;
		if (SystemUtils.IS_OS_WINDOWS) {
			npm = runProcessAndGetFirstResponseLine("node\\npm.cmd", "-v");
		} else {
			npm = runProcessAndGetFirstResponseLine("node/npm", "-v");
		}
		return StringUtils.isNotBlank(npm) ? npm : null;
	}

	private String formatNodeVersion(String node) {
		if (StringUtils.isNotBlank(node)) {
			if (node.startsWith("v")) {
				node = node.substring(1);
			}

			return node;
		} else {
			return null;
		}
	}

	private String runProcessAndGetFirstResponseLine(String command, @Nullable String params) {
		Process process = null;
		List<String> lines = null;
		try {
			if (params != null) {
				process = new ProcessBuilder(command, params).redirectErrorStream(true).start();
			}
			else {
				process = new ProcessBuilder(command).redirectErrorStream(true).start();
			}
			lines = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8);
			process.waitFor();
		} catch (InterruptedException | IOException e) {
			logger.error("Exception: ", e);
		}

		if (process != null && process.exitValue() == 0) {
			if (lines != null) {
				return lines.get(0);
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	public void resolveExactVersions(SemVersion node, SemVersion npm) throws MojoExecutionException {
		List<NodeDistro> versions = getNodeDistros();

		for (NodeDistro distVersion : versions) {
			if (distVersion.getNpm() == null) {
				//stop processing for old node distros without npm
				return;
			}

			if (!node.isExact() && node.satisfies(distVersion.getVersion())) {
				node.setExactVersion(distVersion.getVersion());
			}

			if (npm != null && !npm.isExact() && npm.satisfies(distVersion.getNpm())) {
				npm.setExactVersion(distVersion.getNpm());
			}

			if (node.isExact() && (npm == null || npm.isExact())) {
				return;
			}
		}
	}

	public List<NodeDistro> getNodeDistros() throws MojoExecutionException {
		ObjectMapper om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) new URL("https://nodejs.org/dist/index.json").openConnection();
			con.setRequestMethod("GET");

			return om.readValue(con.getInputStream(), new TypeReference<List<NodeDistro>>() {});
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to fetch node distributions " + e.getMessage(), e);
		}
	}

	public static class SemVersion {
		Expression expression;
		String rawVersion;
		boolean exact;

		public SemVersion(String version) {
			this.rawVersion = version;

			try {
				Version.valueOf(version);
				exact = true;
			} catch (Exception e) {
				//it's an expression
				exact = false;
				Parser<Expression> parser = ExpressionParser.newInstance();
				expression = parser.parse(version);
			}
		}

		public static SemVersion valueOf(String version) {
			if (StringUtils.isBlank(version)) {
				return null;
			}

			return new SemVersion(version);
		}

		public boolean isExact() {
			return exact;
		}

		public String getExactVersion() {
			return rawVersion;
		}

		public void setExactVersion(String version) {
			this.rawVersion = version;
			exact = true;
		}

		public boolean satisfies(String version) {
			if (rawVersion.equals(version)) {
				return true;
			} else if (expression != null){
				return Version.valueOf(version).satisfies(expression);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return rawVersion;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SemVersion that = (SemVersion) o;

			return rawVersion.equals(that.rawVersion);
		}

		@Override
		public int hashCode() {
			return rawVersion.hashCode();
		}
	}

	public SemVersion parseVersion(String version) {
		return SemVersion.valueOf(version);
	}

	public SemVersion getProjectNpmFromPackageJson() throws MojoExecutionException {
		PackageJson packageJson = getPackageJson(PACKAGE_JSON_FILENAME);
		if (packageJson != null && packageJson.getEngines() != null) {
			return SemVersion.valueOf(packageJson.getEngines().get(NPM_VERSION_KEY));
		}
		return null;
	}

	public SemVersion getProjectNodeFromPackageJson() throws MojoExecutionException {
		PackageJson packageJson = getPackageJson(PACKAGE_JSON_FILENAME);
		if (packageJson != null && packageJson.getEngines() != null) {
			return SemVersion.valueOf(packageJson.getEngines().get(NODE_VERSION_KEY));
		}
		return null;
	}

	public PackageJson getPackageJson(String jsonFilename) throws MojoExecutionException {
		Reader reader = null;
		File json;
		if (mavenProject == null) {
			json = new File(jsonFilename);
		} else {
			json = new File(mavenProject.getBasedir(), jsonFilename);
		}

		try {
			if (json.exists()) {
				reader = new FileReader(json);
			} else {
				//used only in tests
				InputStream in = OwaHelper.class.getResourceAsStream(jsonFilename);
				if (in != null) {
					reader = new InputStreamReader(in);
				} else {
					return null;
				}
			}

			return new ObjectMapper().readValue(reader, PackageJson.class);
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't find \"" + jsonFilename + "\" at \"" + json.getAbsolutePath() + "\"");
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public void runLocalNpmCommandWithArgs(List<String> arguments) throws MojoExecutionException {
		runLocalNpmCommandWithArgs(arguments, null);
	}

	public void runLocalNpmCommandWithArgs(List<String> arguments, String installDir) throws MojoExecutionException {
		List<MojoExecutor.Element> configuration = new ArrayList<>();

		configuration.add(element("arguments", StringUtils.join(arguments.iterator(), " ")));
		if (installDir != null) {
			configuration.add(element("installDirectory", installDir));
		}
		executeMojo(
				plugin(
						groupId(SDKConstants.FRONTEND_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.FRONTEND_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.FRONTEND_PLUGIN_VERSION)
				),
				goal("npm"),
				configuration(configuration.toArray(new MojoExecutor.Element[0])),
				executionEnvironment(mavenProject, session, pluginManager)
		);
	}

	public void runSystemNpmCommandWithArgs(List<String> arguments) throws MojoExecutionException {
		List<MojoExecutor.Element> configuration = new ArrayList<>();
		configuration.add(element("executable", getNpmSystemExecutable()));

		List<MojoExecutor.Element> elements = new ArrayList<>();

		for (String argument: arguments) {
			elements.add(element("argument", argument));
		}

		MojoExecutor.Element parentArgs = new MojoExecutor.Element("arguments", elements.toArray(new MojoExecutor.Element[0]));

		configuration.add(parentArgs);
		executeMojo(
				plugin(
						groupId(SDKConstants.EXEC_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.EXEC_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.EXEC_PLUGIN_VERSION)
				),
				goal("exec"),
				configuration(configuration.toArray(new MojoExecutor.Element[0])),
				executionEnvironment(mavenProject, session, pluginManager)
		);
	}

	private String getNpmSystemExecutable() {
		String npm;
		if (SystemUtils.IS_OS_WINDOWS) {
			npm = "npm.cmd";
		} else {
			npm = "npm";
		}
		return npm;
	}

	public boolean resolveNodeAndNpm(String nodeVersion, String npmVersion, String projectDir) throws MojoExecutionException {
		OwaHelper.SemVersion node = parseVersion(nodeVersion);
		OwaHelper.SemVersion npm = parseVersion(npmVersion);

		if (node == null && npm != null) {
			throw new MojoExecutionException("You must specify nodeVersion when specifying npmVersion.");
		}

		String modeMessage = "";
		if (node == null) {
			node = getProjectNodeFromPackageJson();
			npm = getProjectNpmFromPackageJson();

			if (node == null) {
				node = parseVersion(SDKConstants.NODE_VERSION);
				npm = parseVersion(SDKConstants.NPM_VERSION);
				modeMessage = " (SDK default, which can be overwritten with nodeVesion and npmVersion arguments or by engines in package.json)";
			} else {
				modeMessage = " as defined in package.json";
			}
		}

		wizard.showMessage("Looking for node " + node +" and npm " + npm + modeMessage + ".");

		boolean useSystemNode = false;

		String systemNode = getSystemNodeVersion();
		String systemNpm = getSystemNpmVersion();

		if (systemNode != null && systemNpm != null) {
			if (node.satisfies(systemNode) && (npm == null || npm.satisfies(systemNpm))) {
				wizard.showMessage("Using system node " + systemNode +" and npm " + systemNpm);
				useSystemNode = true;
			}
		}

		if (!useSystemNode) {
			boolean updateLocalNodeAndNpm = true;

			String projectNode = getProjectNodeVersion();
			String projectNpm = getProjectNpmVersion();

			if (projectNode != null && projectNpm != null) {
				if (node.satisfies(projectNode) && (npm == null || npm.satisfies(projectNpm))) {
					wizard.showMessage("Using project node " + projectNode + " and npm " + projectNpm);
					updateLocalNodeAndNpm = false;
				}
			}

			if (updateLocalNodeAndNpm) {
				installLocalNodeAndNpm(node, npm, projectDir);
			}
		}

		return useSystemNode;
	}

	public void installLocalNodeAndNpm(SemVersion node, SemVersion npm, String installDirectory) throws MojoExecutionException {
		String nodeVersion;
		String npmVersion;

		if (!node.isExact() || (npm != null && !npm.isExact())) {
			resolveExactVersions(node, npm);
		}

		if (node.isExact()) {
			nodeVersion = node.getExactVersion();
			if (npm == null) {
				npmVersion = null;
			} else if (npm.isExact()) {
				npmVersion = npm.getExactVersion();
			} else {
				throw new MojoExecutionException("Could not find a matching npm version.");
			}
		} else {
			throw new MojoExecutionException("Could not find a matching node version.");
		}

		runInstallLocalNodeAndNpm(nodeVersion, npmVersion, installDirectory);
	}

	public void runInstallLocalNodeAndNpm(String nodeVersion, String npmVersion, String installDirectory) throws MojoExecutionException {
		List<MojoExecutor.Element> configuration = new ArrayList<>();
		configuration.add(element("nodeVersion", "v" + nodeVersion));
		if (npmVersion != null) {
			configuration.add(element("npmVersion", npmVersion));
		}
		if (installDirectory != null) {
			configuration.add(element("installDirectory", installDirectory));
		}
		executeMojo(
				plugin(
						groupId(SDKConstants.FRONTEND_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.FRONTEND_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.FRONTEND_PLUGIN_VERSION)
				),
				goal("install-node-and-npm"),
				configuration(configuration.toArray(new MojoExecutor.Element[0])),
				executionEnvironment(mavenProject, session, pluginManager)
		);
	}

}
