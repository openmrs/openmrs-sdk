package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.ServerHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Start a server. If it is run from a module/distro/platform project, the project will be redeployed before launching.
 */
@Mojo(name = "run", requiresProject = false)
public class Run extends AbstractServerTask {

	private static final Version TOMCAT_7_CUTOFF = new Version("2.5.0-SNAPSHOT");

	private static final Version TOMCAT_9_CUTOFF = new Version("3.0.0-SNAPSHOT");

	public Run() {
	}

	public Run(AbstractTask other) {
		super(other);
	}

	public Run(AbstractTask other, String serverId) {
		this(other);
		this.serverId = serverId;
	}

	/**
	 * Port to use for running the server (defaults to '8080')
	 */
	@Parameter(property = "port")
	private Integer port;

	/**
	 * Enable remote debugging on the given port (defaults to '1044' if empty)
	 */
	@Parameter(property = "debug")
	private String debug;

	/**
	 * Flag to indicate whether to redeploy API classes
	 */
	@Parameter(property = "watchApi")
	private Boolean watchApi;

	/**
	 * Flag to indicate whether to build server's watched projects, OWA projects, or node projects.
	 */
	@Parameter(defaultValue = "false", property = "skipBuild")
	private boolean skipBuild;

	/**
	 * Goal to execute when running the server."Goal to execute when running the server
	 * (Defaults to 'org.openmrs.maven.plugins:openmrs-sdk-tomcat9-maven-plugin:{$version}')
	 */
	@Parameter(property = "runGoal")
	private String runGoal;


	/**
		Pass JVM arguments to the run command
	 	example: --add-opens java.base/java.lang=ALL-UNNAMED
	 */
	@Parameter(property = "jvmArgs")
	private String jvmArgs;


	private ServerHelper serverHelper;

	public void executeTask() throws MojoExecutionException, MojoFailureException {
		Server server = getServer();

		if (port == null && server.getPort() != null) {
			port = Integer.valueOf(server.getPort());
		}

		if (port == null || port < 1 || port > 65535) {
			port = 8080;
		}

		serverHelper = new ServerHelper(wizard);
		this.validatePort();

		server.setParam("tomcat.port", String.valueOf(port));

		server.save();
		File serverPath = server.getServerDirectory();
		serverPath.mkdirs();
		File userDir = new File(System.getProperty("user.dir"));
		if (Project.hasProject(userDir)) {
			Project config = Project.loadProject(userDir);
			if (config.isOpenmrsModule()) {
				String artifactId = config.getArtifactId();
				String groupId = config.getGroupId();
				String version = config.getVersion();
				if ((artifactId != null) && (groupId != null) && version != null) {
					getLog().info("OpenMRS module detected, installing before run...");
					Deploy deployer = new Deploy(this);
					deployer.deployModule(server, groupId, artifactId, version);
				}
			}
		}

		if (server.hasWatchedProjects() && !skipBuild) {
			new Build(this, serverId).executeTask();
		}


		runInFork(server);
	}

	private void validatePort() throws MojoExecutionException {
		int tmpPort = port;

		tmpPort = serverHelper.findFreePort(tmpPort);
		if (port != tmpPort) {
			String message = String.format("Port %s is already in use. What port would you like to use?", port);

			port = 0;
			while (port < 1 || port > 65535) {
				String result = wizard.promptForValueIfMissingWithDefault(message, null, null, String.valueOf(tmpPort));
				try {
					port = Integer.parseInt(result);
				}
				catch (NumberFormatException ignore) {
				}

				message = String.format("%s is not a valid port. What port would you like to use?", result);
			}
		}
	}

	private void runInFork(Server server) throws MojoExecutionException, MojoFailureException {

		if (server.hasWatchedProjects()) {
			Path serversPath = Server.getServersPath();
			File springloadedJar = serversPath.resolve("springloaded.jar").toFile();
            if (!springloadedJar.exists()) {
				Artifact artifact = new Artifact("springloaded", "1.2.5.RELEASE", "org.springframework", "jar");
				artifact.setDestFileName("springloaded.jar");
				executeMojo(
						plugin(
								groupId(SDKConstants.DEPENDENCY_PLUGIN_GROUP_ID),
								artifactId(SDKConstants.DEPENDENCY_PLUGIN_ARTIFACT_ID),
								version(SDKConstants.DEPENDENCY_PLUGIN_VERSION)
						),
						goal("copy"),
						configuration(
								element(name("artifactItems"), artifact.toElement(serversPath.toAbsolutePath().toString()))
						),
						executionEnvironment(mavenProject, mavenSession, pluginManager)
				);
			}
		}

		String mavenOpts = mavenSession.getRequest().getUserProperties().getProperty("MAVEN_OPTS", "");
		if (StringUtils.isBlank(mavenOpts)) {
			mavenOpts = mavenSession.getRequest().getUserProperties().getProperty("env.MAVEN_OPTS", "");
		}
		if (StringUtils.isBlank(mavenOpts)) {
			mavenOpts = mavenSession.getRequest().getSystemProperties().getProperty("MAVEN_OPTS", "");
		}
		if (StringUtils.isBlank(mavenOpts)) {
			mavenOpts = mavenSession.getRequest().getSystemProperties().getProperty("env.MAVEN_OPTS", "");
		}

		mavenOpts = adjustXmxToAtLeast(mavenOpts, 768);

		if (isJava11OrHigher(server)) {
			mavenOpts +=
					" --add-opens java.base/java.lang=ALL-UNNAMED" +
					" --add-opens java.base/java.lang.reflect=ALL-UNNAMED" +
					" --add-opens java.base/java.io=ALL-UNNAMED" +
					" --add-opens java.base/java.util=ALL-UNNAMED";
		}

		if (server.hasWatchedProjects() && isWatchApi()) {
			mavenOpts +=
					" -javaagent:\"" + Server.getServersPath().resolve("springloaded.jar").toAbsolutePath() + "\" -noverify";
		}

		mavenOpts = setDebugPort(mavenOpts, server);

		if (StringUtils.isNotBlank(jvmArgs)) {
			mavenOpts += " " + jvmArgs + " ";
		}

		Properties properties = new Properties();
		properties.put("serverId", server.getServerId());
		if (port != null) {
			properties.put("port", port.toString());
		}

		if (isWatchApi()) {
			properties.put("watchApi", "true");
		}

		if (server.hasWatchedProjects() && isWatchApi()) {
			properties.put("springloaded", "inclusions=org.openmrs..*");
		}

		// if the runGoal isn't specified, we default to Tomcat 9, unless running a platform version before 2.5.0-SNAPSHOT
		if (StringUtils.isBlank(runGoal)) {
			String tomcatArtifactId = SDKConstants.OPENMRS_TOMCAT11_PLUGIN_ARTIFACT_ID;
			Version platformVersion = new Version(server.getPlatformVersion());
			if (platformVersion.lower(TOMCAT_7_CUTOFF)) {
				tomcatArtifactId = SDKConstants.OPENMRS_TOMCAT7_PLUGIN_ARTIFACT_ID;
			} else if (platformVersion.lower(TOMCAT_9_CUTOFF)) {
				tomcatArtifactId =SDKConstants.OPENMRS_TOMCAT9_PLUGIN_ARTIFACT_ID;
			}

			runGoal = String.format("%s:%s:%s:run-tomcat",
					SDKConstants.OPENMRS_TOMCAT_PLUGIN_GROUP_ID,
					tomcatArtifactId,
					SDKConstants.getSDKInfo().getVersion());
		}

		InvocationRequest request = new DefaultInvocationRequest();
		request.setGoals(Collections.singletonList(runGoal))
				.setMavenOpts(mavenOpts)
				.setProperties(properties)
				.setShowErrors(mavenSession.getRequest().isShowErrors())
				.setOffline(mavenSession.getRequest().isOffline())
				.setLocalRepositoryDirectory(mavenSession.getRequest().getLocalRepositoryPath())
				.setUpdateSnapshots(mavenSession.getRequest().isUpdateSnapshots())
				.setShowVersion(true);

		if (server.getJavaHome() != null) {
			request.setJavaHome(new File(server.getJavaHome()));
		}

		try {
			Invoker invoker = new DefaultInvoker();
			InvocationResult result = invoker.execute(request);
			if (result.getExitCode() != 0) {
				if (result.getExecutionException() != null) {
					throw new MojoFailureException("Failed running Tomcat " + result.getExecutionException().getMessage(),
							result.getExecutionException());
				} else {
					throw new MojoExecutionException("Failed running Tomcat");
				}
			}
		}
		catch (MavenInvocationException e) {
			throw new MojoFailureException("Failed to start Tomcat process", e);
		}
	}

	private boolean isJava11OrHigher(Server server) {
		String version = System.getProperty("java.version");
		if (version == null || version.startsWith("1.")) {
			return false;
		}
		try {
			int major = Integer.parseInt(version.split("\\.")[0]);
			return major >= 11;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String setDebugPort(String mavenOpts, Server server) throws MojoExecutionException {
		String address = null;
		if (StringUtils.isNotBlank(debug)) {
			if (StringUtils.isNumeric(debug)) {
				address = debug;
			} else if (Boolean.parseBoolean(debug)) {
				if (StringUtils.isNotBlank(server.getDebugPort())) {
					address = server.getDebugPort();
				} else {
					debug = wizard.promptForValueIfMissingWithDefault(
							"Specify %s for debugging",
							null,
							"port number",
							"1044");
					if (StringUtils.isNumeric(debug)) {
						address = debug;
					} else {
						wizard.showError("Port number must be numeric");
					}
				}
			} else if (!"false".equalsIgnoreCase(debug)) {
				wizard.showError("Port number must be numeric");
			}
		} else {
			if (StringUtils.isNotBlank(server.getDebugPort())) {
				address = server.getDebugPort();
			}
		}
		if (StringUtils.isNotBlank(address)) {
			mavenOpts += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + address;
			wizard.showMessage("Connect remote debugger via port " + address);
		}
		return mavenOpts;
	}

	String adjustXmxToAtLeast(String input, Integer valueInMegabytes) {
		return adjustParamToAtLeast(input, "-Xmx([0-9]+)([kKmMgG]?)", "-Xmx" + valueInMegabytes + "m", valueInMegabytes);
	}

	String adjustMaxPermSizeToAtLeast(String mavenOpts, Integer valueInMegabytes) {
		return adjustParamToAtLeast(mavenOpts, "-XX:MaxPermSize=([0-9]+)([kKmMgG]?)",
				"-XX:MaxPermSize=" + valueInMegabytes + "m", valueInMegabytes);
	}

	private String adjustParamToAtLeast(String input, String pattern, String replacement, Integer valueInMegabytes) {
		Pattern xmx = Pattern.compile(pattern);
		Matcher xmxMatcher = xmx.matcher(input);
		if (xmxMatcher.find()) {
			boolean xmxReplace;
			int value = Integer.parseInt(xmxMatcher.group(1));
			String unit = xmxMatcher.group(2).toLowerCase();
			switch (unit) {
				case "k":
					xmxReplace = value < valueInMegabytes * 1024;
					break;
				case "m":
					xmxReplace = value < valueInMegabytes;
					break;
				case "g":
					xmxReplace = value < ((double) valueInMegabytes / 1024);
					break;
				default:
					xmxReplace = value < valueInMegabytes * 1024 * 1024;
					break;
			}

			if (xmxReplace) {
				input = xmxMatcher.replaceAll(replacement);
			}
		} else {
			input += " " + replacement;
		}

		return input;
	}

	@Override
	protected Server loadServer() throws MojoExecutionException {
		return loadValidatedServer(serverId);
	}

	private boolean isWatchApi() {
		return Boolean.TRUE.equals(watchApi);
	}
}
