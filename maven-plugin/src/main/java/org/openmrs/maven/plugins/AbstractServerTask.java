package org.openmrs.maven.plugins;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Server;

public abstract class AbstractServerTask extends AbstractTask {

	/**
	 * The identifier for the server to work with, if any
	 */
	@Parameter(property = "serverId")
	String serverId;

	public AbstractServerTask() { super(); }

	public AbstractServerTask(AbstractTask other) {
		super(other);
	}

	public AbstractServerTask(AbstractServerTask other) {
		super(other);
		this.serverId = other.serverId;
	}

	@Override
	public void initTask() {
		super.initTask();

		// FIXME this is an ugly hack to ensure that we load the serverId if it's set
		if (serverId == null && mavenSession != null) {
			if (mavenSession.getUserProperties().containsKey("serverId")) {
				serverId = mavenSession.getUserProperties().getProperty("serverId");
			} else if (mavenSession.getSystemProperties().containsKey("serverId")) {
				serverId = mavenSession.getSystemProperties().getProperty("serverId");
			}
		}
	}

	public Server getServer() throws MojoExecutionException {
		if (serverId == null) {
			File currentProperties = Server.checkCurrentDirForServer();
			if (currentProperties != null) {
				serverId = currentProperties.getName();
			}
		}

		serverId = wizard.promptForExistingServerIdIfMissing(serverId);
		return loadServer();
	}

	/**
	 * Loads the actual server instance
	 *
	 * @return the configured server
	 * @throws MojoExecutionException if an exception occurs while loading the server
	 */
	protected Server loadServer() throws MojoExecutionException {
		return Server.loadServer(serverId);
	}

	protected Server loadValidatedServer(String serverId) throws MojoExecutionException {
		File serversPath = Server.getServersPathFile();
		File serverPath = new File(serversPath, serverId);
		new ServerUpgrader(this).validateServerMetadata(serverPath);
		return Server.loadServer(serverPath);
	}
}
