package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

public class OwaHelper {
	private final static String FRONTEND_BUILDER_GROUP_ID = "com.github.eirslett";
	private final static String FRONTEND_BUILDER_ARTIFACT_ID = "frontend-maven-plugin";
	private final static String FRONTEND_BUILDER_VERSION = "1.0";

	private MavenSession session;

	private File installationDir;

	private MavenProject mavenProject;

	private BuildPluginManager pluginManager;

	private final Wizard wizard;

	//enable chaining
	public OwaHelper setInstallationDir(File installationDir) {
		this.installationDir = installationDir;
		return this;
	}

	public OwaHelper(MavenSession session, MavenProject mavenProject, BuildPluginManager pluginManager, Wizard wizard) {
		this.session = session;
		this.mavenProject = mavenProject;
		this.pluginManager = pluginManager;
		this.wizard = wizard;
	}

	public void createOwaProject() throws MojoExecutionException {
		File owaDir = installationDir != null ? installationDir : prepareOwaDir();

		wizard.showMessage("Creating OWA project in " + owaDir.getAbsolutePath() + "...\n");

		runMojoExecutor(Arrays.asList(MojoExecutor.element("nodeVersion", SDKConstants.NODE_VERSION), MojoExecutor.element("npmVersion", SDKConstants.NPM_VERSION), MojoExecutor.element("installDirectory", owaDir.getAbsolutePath())), "install-node-and-npm");
		runMojoExecutor(Arrays.asList(MojoExecutor.element("arguments", "install -g yo generator-openmrs-owa"), MojoExecutor.element("installDirectory", owaDir.getAbsolutePath())), "npm");

		try {
			runYeoman(owaDir);
			addHelperScripts(owaDir.getAbsolutePath());
		} catch (IOException e) {
			throw new IllegalStateException("Failed starting yeoman", e);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Failed running yeoman", e);
		}
	}

	private File prepareOwaDir() {
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

	private void addHelperScripts(String path) {
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
			throw new RuntimeException("Could not copy helper scripts to OWA directory", e);
		}

	}

	private void runYeoman(File directory) throws InterruptedException, IOException {
		ProcessBuilder builder = new ProcessBuilder()
				.directory(directory)
				.command(getYoExecutable(directory))
				.redirectErrorStream(true)
				.inheritIO();

		Process process = builder.start();
		process.waitFor();

	}

	private String[] getYoExecutable(File directory) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return new String[]{new File(directory, "node\\node.exe").getAbsolutePath(), "node\\node_modules\\yo\\lib\\cli.js", "openmrs-owa"};
		} else {
			return new String[]{"node/node", "lib/node_modules/yo/lib/cli.js", "openmrs-owa"};
		}
	}

	private void runMojoExecutor(List<MojoExecutor.Element> configuration, String goal) throws MojoExecutionException {
		executeMojo(
				plugin(
						groupId(FRONTEND_BUILDER_GROUP_ID),
						artifactId(FRONTEND_BUILDER_ARTIFACT_ID),
						version(FRONTEND_BUILDER_VERSION)
				),
				goal(goal),
				configuration(configuration.toArray(new MojoExecutor.Element[0])),
				executionEnvironment(mavenProject, session, pluginManager)
		);
	}

}