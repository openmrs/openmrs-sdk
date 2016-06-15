package org.openmrs.maven.plugins;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @goal run-tomcat
 * @requiresProject false
 */
public class RunTomcat extends AbstractMojo {

	/**
	 * @parameter expression="${serverId}"
	 */
	private String serverId;

	/**
	 * @parameter expression="${port}"
	 */
	private Integer port;

	/**
	 * @component
	 * @required
	 */
	private Wizard wizard;

	public RunTomcat() {
	}

	public RunTomcat(String serverId, Integer port, Wizard wizard) {
		this.serverId = serverId;
		this.port = port;
		this.wizard = wizard;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		serverId = wizard.promptForExistingServerIdIfMissing(serverId);
		Server server = Server.loadServer(serverId);
		File tempDirectory = server.getServerTmpDirectory();
		tempDirectory.mkdirs();

		String warFile = "openmrs.war";
		File serverPath = server.getServerDirectory();
		for (File file : serverPath.listFiles()) {
			if ((file.getName().endsWith(".war"))) {
				warFile = file.getName();
				break;
			}
		}

		Tomcat tomcat = new Tomcat();
		if (port == null) {
			port = 8080;
		}
		tomcat.setPort(port);
		tomcat.setBaseDir(tempDirectory.getAbsolutePath());
		tomcat.getHost().setAppBase(tempDirectory.getAbsolutePath());
		tomcat.getHost().setAutoDeploy(true);
		tomcat.getHost().setDeployOnStartup(true);
		Context context = tomcat.addWebapp(tomcat.getHost(), "/openmrs", new File(serverPath, warFile).getAbsolutePath());

		System.setProperty("OPENMRS_INSTALLATION_SCRIPT",
				new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath());
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", serverPath.getAbsolutePath() + File.separator);

		setSystemPropertiesForWatchedProjects(serverPath);

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(newTomcatClassLoader());
			WebappLoader tomcatLoader = new WebappLoader(Thread.currentThread().getContextClassLoader());
			context.setLoader(tomcatLoader);

			tomcat.start();
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			throw new MojoExecutionException("Tomcat failed to start", e);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	protected ClassRealm newTomcatClassLoader() throws MojoExecutionException {
		try {
			ClassWorld world = new ClassWorld();
			ClassRealm root = world.newRealm("tomcat", Thread.currentThread().getContextClassLoader());

			return root;
		} catch (DuplicateRealmException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void setSystemPropertiesForWatchedProjects(File serverPath) throws MojoExecutionException {
		Server serverConfig = Server.loadServer(serverPath);
		Set<Project> watchedProjects = serverConfig.getWatchedProjects();
		if (!watchedProjects.isEmpty()) {
			getLog().info(" ");
			getLog().info("Hot redeployment enabled for: ");
			int i = 1;
			for (Project project : watchedProjects) {
				System.setProperty("uiFramework.development." + project.getArtifactId(), project.getPath());
				System.setProperty(project.getArtifactId() + ".development.directory", project.getPath());
				getLog().info(
						String.format("%d) %s:%s at %s", i, project.getGroupId(), project.getArtifactId(), project.getPath()));
				i++;
			}
			getLog().info(" ");
		}
	}

}
