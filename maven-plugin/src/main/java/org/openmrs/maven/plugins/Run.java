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
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.ServerHelper;

import java.io.File;
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

@Mojo(name = "run", requiresProject = false)
public class Run extends AbstractServerTask {

	public Run() {
	}

	public Run(AbstractTask other) {
		super(other);
	}

	public Run(AbstractTask other, String serverId) {
		this(other);
		this.serverId = serverId;
	}

	@Parameter(property = "port")
	private Integer port;

	@Parameter(property = "debug")
	private String debug;

	@Parameter(property = "fork")
	private Boolean fork;

	@Parameter(property = "watchApi")
	private Boolean watchApi;

	@Parameter(defaultValue = "false", property = "skipBuild")
	private boolean skipBuild;

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

		if (Boolean.FALSE.equals(fork)) {
			new RunTomcat(serverId, port, mavenSession, mavenProject, pluginManager, wizard).execute();
		} else {
			runInFork(server);
		}
	}

	private void validatePort() {
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
			File serversPath = Server.getServersPathFile();
			File springloadedJar = new File(serversPath, "springloaded.jar");
			if (!springloadedJar.exists()) {
				Artifact artifact = new Artifact("springloaded", "1.2.5.RELEASE", "org.springframework", "jar");
				artifact.setDestFileName("springloaded.jar");
				executeMojo(
						plugin(
								groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
								artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
								version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
						),
						goal("copy"),
						configuration(
								element(name("artifactItems"), artifact.toElement(serversPath.getAbsolutePath()))
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
		mavenOpts = adjustMaxPermSizeToAtLeast(mavenOpts, 512);

		if (server.hasWatchedProjects() && isWatchApi()) {
			mavenOpts +=
					" -javaagent:" + new File(Server.getServersPath(), "springloaded.jar").getAbsolutePath() + " -noverify";
		}

		mavenOpts = setDebugPort(mavenOpts, server);

		System.out.println("\nForking a new process... (use -Dfork=false to prevent forking)\n");

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

		InvocationRequest request = new DefaultInvocationRequest();
		request.setGoals(Collections.singletonList(SDKConstants.getSDKInfo().getGroupId() + ":"
				+ SDKConstants.getSDKInfo().getArtifactId() + ":" + SDKConstants.getSDKInfo().getVersion() + ":run-tomcat"))
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
				throw new IllegalStateException("Failed running Tomcat", result.getExecutionException());
			}
		}
		catch (MavenInvocationException e) {
			throw new MojoFailureException("Failed to start Tomcat process", e);
		}
	}

	private String setDebugPort(String mavenOpts, Server server) {
		String address = null;
		if (StringUtils.isNotBlank(debug)) {
			if (StringUtils.isNumeric(debug)) {
				address = debug;
			} else if ("true".equals(debug)) {
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
			} else if ("false".equals(debug)) {
				address = null;
			} else {
				wizard.showError("Port number must be numeric");
			}
		} else {
			if (StringUtils.isNotBlank(server.getDebugPort())) {
				address = server.getDebugPort();
			}
		}
		if (StringUtils.isNotBlank(address)) {
			mavenOpts += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + address;
			System.out.println("\nConnect remote debugger with port " + address + "\n");
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
