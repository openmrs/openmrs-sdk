package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

public class NodeHelper {
	
	private final MavenSession mavenSession;
	
	private final MavenProject mavenProject;
	
	private final BuildPluginManager pluginManager;

	private Path tempDir;
	
	public NodeHelper(MavenProject mavenProject,
			MavenSession mavenSession,
			BuildPluginManager pluginManager) {
		this.mavenProject = mavenProject;
		this.mavenSession = mavenSession;
		this.pluginManager = pluginManager;
		try {
			this.tempDir = Files.createTempDirectory("openmrs-sdk-node");
			this.tempDir.toFile().deleteOnExit();
		} catch (IOException e) {
			throw new IllegalStateException("Exception while trying to create temporary directory for node", e);
		}
	}
	
	public void installNodeAndNpm(String nodeVersion, String npmVersion) throws MojoExecutionException {
		List<MojoExecutor.Element> configuration = new ArrayList<>(3);
		configuration.add(element("nodeVersion", "v" + nodeVersion));
		configuration.add(element("npmVersion", npmVersion));
		configuration.add(element("installDirectory",  tempDir.toAbsolutePath().toString()));
		// The frontend maven plugin fails without a lib directory within the installDirectory
		new File(tempDir.toFile(), "lib").mkdirs();
		runFrontendMavenPlugin("install-node-and-npm", configuration);
	}
	
	public void runNpx(String arguments) throws MojoExecutionException {
		String npmExec = "exec -- " + arguments;
		
		// it's a little weird to use a custom NPM cache for this; however, it seems to be necessary to get things working on Bamboo
		// hack added in December 2021; it's use probably should be re-evaluated at some point
		// additional hack: we do not use --cache on macs due to https://github.com/npm/cli/issues/3256
		if (mavenProject != null && mavenProject.getBuild() != null && !SystemUtils.IS_OS_MAC_OSX) {
			npmExec =
					" --cache=" + tempDir.resolve("npm-cache").toAbsolutePath() + " exec -- " + arguments;
		}
		
		List<MojoExecutor.Element> configuration = new ArrayList<>(3);
		configuration.add(element("arguments", npmExec));
		
		if (mavenProject != null && mavenProject.getBuild() != null) {
			configuration.add(element("installDirectory", tempDir.toAbsolutePath().toString()));
		}
		
		runFrontendMavenPlugin("npm", configuration);
	}
	
	private void runFrontendMavenPlugin(String goal, List<MojoExecutor.Element> configuration)
			throws MojoExecutionException {
		// the proxy already works for installing node and npm; however, it does not work when running npm or npx
		if (goal != null && !goal.equalsIgnoreCase("install-node-and-npm")) {
			NodeProxyHelper.ProxyContext proxyContext = NodeProxyHelper.setupProxyContext(mavenSession);
			proxyContext.applyProxyContext(configuration);
		}
		
		executeMojo(
				plugin(
						groupId(SDKConstants.FRONTEND_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.FRONTEND_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.FRONTEND_PLUGIN_VERSION)
				),
				goal(goal),
				configuration(configuration.toArray(new MojoExecutor.Element[0])),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}
	
}
