package org.openmrs.maven.plugins.utility;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

@Data
public class NodeHelper implements AutoCloseable {

	private MavenEnvironment mavenEnvironment;
	private boolean reuseNodeCache;
	private File nodeInstallDir = null;

	private static final Logger logger = LoggerFactory.getLogger(NodeHelper.class);

	public NodeHelper(MavenEnvironment mavenEnvironment) {
		this.mavenEnvironment = mavenEnvironment;
	}

	/**
	 * Installs node and npm, creating a new installation directory if necessary
	 */
	public void installNodeAndNpm(String nodeVersion, String npmVersion, boolean reuseNodeCache) throws MojoExecutionException {
		this.reuseNodeCache = reuseNodeCache;
		if (reuseNodeCache) {
			this.nodeInstallDir = new File(Server.getServersPath().toString(), "_openmrs_sdk_node_cache");
		}
		String nodeCacheSystemProperty = System.getProperty("nodeCacheDir");
		if (StringUtils.isNotBlank(nodeCacheSystemProperty)) {
			this.reuseNodeCache = true;
			this.nodeInstallDir = new File(nodeCacheSystemProperty);
		}
		if (this.nodeInstallDir != null && this.nodeInstallDir.mkdir()) {
			logger.info("Created node install directory: " + this.nodeInstallDir.getAbsolutePath());
		}
		if (this.nodeInstallDir == null) {
			try {
				this.nodeInstallDir = Files.createTempDirectory("openmrs-sdk-node").toFile();
				logger.info("Created node install temporary directory: " + nodeInstallDir.getAbsolutePath());
			}
			catch (IOException e) {
				throw new MojoExecutionException("Unable to create temporary node install directory: " + nodeInstallDir, e);
			}
		}

        List<MojoExecutor.Element> configuration = new ArrayList<>(3);
		configuration.add(element("nodeVersion", "v" + nodeVersion));
		configuration.add(element("npmVersion", npmVersion));
		configuration.add(element("installDirectory",  nodeInstallDir.getAbsolutePath()));
		// The frontend maven plugin fails without a lib directory within the installDirectory
		File libDir = new File(nodeInstallDir, "lib");
		if (libDir.mkdirs()) {
			logger.info("Created lib dir " + libDir.getAbsolutePath());
		}
		runFrontendMavenPlugin("install-node-and-npm", configuration);
	}

	@Override
	public void close() {
		if (!reuseNodeCache && nodeInstallDir != null && nodeInstallDir.exists()) {
			try {
				MoreFiles.deleteRecursively(nodeInstallDir.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
			}
			catch (IOException e) {
				logger.error("Unable to delete temporary node installation directory: " + nodeInstallDir, e);
			}
		}
	}

	public void runNpx(String arguments, String npmArguments) throws MojoExecutionException {
		final String npmExec;
		// it's a little weird to use a custom NPM cache for this; however, it seems to be necessary to get things working on Bamboo
		// hack added in December 2021; it's use probably should be re-evaluated at some point
		// additional hack: we do not use --cache on macs due to https://github.com/npm/cli/issues/3256
		MavenProject mavenProject = mavenEnvironment.getMavenProject();
		if (mavenProject != null && mavenProject.getBuild() != null && !SystemUtils.IS_OS_MAC_OSX) {
			npmExec = npmArguments + " --cache=" + nodeInstallDir.toPath().resolve("npm-cache").toAbsolutePath() + " exec -- " + arguments;
		} else {
			npmExec = npmArguments + " exec -- " + arguments;
		}

		List<MojoExecutor.Element> configuration = new ArrayList<>(3);
		configuration.add(element("arguments", npmExec));

		if (mavenProject != null && mavenProject.getBuild() != null) {
			configuration.add(element("installDirectory", nodeInstallDir.getAbsolutePath()));
		}

		runFrontendMavenPlugin("npm", configuration);
	}

	private void runFrontendMavenPlugin(String goal, List<MojoExecutor.Element> configuration)
			throws MojoExecutionException {
		// the proxy already works for installing node and npm; however, it does not work when running npm or npx
		if (goal != null && !goal.equalsIgnoreCase("install-node-and-npm")) {
			MavenSession mavenSession = mavenEnvironment.getMavenSession();
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
				 executionEnvironment(
						mavenEnvironment.getMavenProject(),
						mavenEnvironment.getMavenSession(),
						mavenEnvironment.getPluginManager()
				)
		);
	}
}
