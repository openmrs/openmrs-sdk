package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * @goal run
 * @requiresProject false
 */
public class Run extends AbstractTask {

    public Run(){};

    public Run(AbstractTask other){
        super(other);
    }

	/**
	 * @parameter expression="${serverId}"
	 */
	private String serverId;

	/**
	 * @parameter expression="${port}"
	 */
	private Integer port;

	/**
	 * @parameter expression="${debug}"
	 */
	private String debug;

	/**
	 * @parameter expression="${fork}"
	 */
	private Boolean fork;

	/**
	 * @parameter expression="${watchApi}"
	 */
	private Boolean watchApi;

	public void executeTask() throws MojoExecutionException, MojoFailureException {
		if (serverId == null) {
			File currentProperties = Server.checkCurrentDirForServer();
			if (currentProperties != null) serverId = currentProperties.getName();
		}
		serverId = wizard.promptForExistingServerIdIfMissing(serverId);
		Server server = loadValidatedServer(serverId);
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
					deployer.deployModule(serverPath.getName(), groupId, artifactId, version);
				}
			}
		}

		if (server.hasWatchedProjects()) {
			new Build(this, serverId).executeTask();
		}

		if (Boolean.FALSE.equals(fork)) {
			new RunTomcat(serverId, port, wizard).execute();
		} else {
			runInFork(server);
		}
	}

	private void runInFork(Server server) throws MojoExecutionException, MojoFailureException {
		String maven = "mvn";
		if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
			maven = "mvn.bat";
		}

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

		String mavenOpts = System.getProperty("MAVEN_OPTS", "");

		mavenOpts = adjustXmxToAtLeast(mavenOpts, 768);
		mavenOpts = adjustMaxPermSizeToAtLeast(mavenOpts, 512);

		if (server.hasWatchedProjects() && isWatchApi()) {
			mavenOpts += " -javaagent:" + new File(Server.getServersPath(), "springloaded.jar").getAbsolutePath() + " -noverify";
		}

		if (debug != null) {
			String address = "1044";
			if (StringUtils.isNumeric(debug)) {
				address = debug;
			}
			mavenOpts += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + address;

			System.out.println("\nConnect remote debugger with port " + address + "\n");
		}

		System.out.println("\nForking a new process... (use -Dfork=false to prevent forking)\n");

		List<String> commands = new ArrayList<String>();
		commands.add(maven);
		commands.add(SDKConstants.getSDKInfo().getGroupId() + ":"
				+ SDKConstants.getSDKInfo().getArtifactId() + ":" + SDKConstants.getSDKInfo().getVersion() + ":run-tomcat");
		commands.add("-DserverId=" + server.getServerId());
		if (port != null) {
			commands.add("-Dport=" + port);
		}
		if (isWatchApi()) {
			commands.add("-DwatchApi=" + watchApi);
		}
		if (server.hasWatchedProjects() && isWatchApi()) {
			commands.add("-Dspringloaded=inclusions=org.openmrs..*");
		}
		commands.add("-e");

		ProcessBuilder processBuilder = new ProcessBuilder(commands);

		processBuilder.environment().put("MAVEN_OPTS", mavenOpts);
		if (server.getJavaHome() != null) {
			processBuilder.environment().put("JAVA_HOME", server.getJavaHome());
		}

		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
		try {
			final Process process = processBuilder.start();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					process.destroy();
				}
			});

			process.waitFor();
		} catch (IOException e) {
			throw new MojoFailureException("Failed to start Tomcat process", e);
		} catch (InterruptedException e) {
			throw new MojoFailureException("Interrupted waiting for Tomcat process", e);
		}
	}

	String adjustXmxToAtLeast(String input, Integer valueInMegabytes) {
		return adjustParamToAtLeast(input, "-Xmx([0-9]+)([kKmMgG]?)", "-Xmx" + valueInMegabytes + "m", valueInMegabytes);
	}

	String adjustMaxPermSizeToAtLeast(String mavenOpts, Integer valueInMegabytes) {
		return adjustParamToAtLeast(mavenOpts, "-XX:MaxPermSize=([0-9]+)([kKmMgG]?)", "-XX:MaxPermSize=" + valueInMegabytes + "m", valueInMegabytes);
	}

	private String adjustParamToAtLeast(String input, String pattern, String replacement, Integer valueInMegabytes) {
		Pattern xmx = Pattern.compile(pattern);
		Matcher xmxMatcher = xmx.matcher(input);
		if (xmxMatcher.find()) {
			boolean xmxReplace;
			Integer value = Integer.valueOf(xmxMatcher.group(1));
			String unit = xmxMatcher.group(2).toLowerCase();
			if ("k".equals(unit)) {
				xmxReplace = value < valueInMegabytes * 1024;
			} else if ("m".equals(unit)) {
				xmxReplace = value < valueInMegabytes;
			} else if ("g".equals(unit)) {
				xmxReplace = value < ((double) valueInMegabytes / 1024);
			} else {
				xmxReplace = value < valueInMegabytes * 1024 * 1024;
			}

			if (xmxReplace) {
				input = xmxMatcher.replaceAll(replacement);
			}
 		} else {
			input += " " + replacement;
		}

		return input;
	}

	private boolean isWatchApi() {
		return Boolean.TRUE.equals(watchApi);
	}
}
