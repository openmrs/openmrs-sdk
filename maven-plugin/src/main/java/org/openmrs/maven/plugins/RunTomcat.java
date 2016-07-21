package org.openmrs.maven.plugins;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
	 * @parameter expression="${watchApi}"
	 */
	private Boolean watchApi;

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
		System.out.println("\nUsing JAVA_HOME: " + System.getProperty("java.home") + "\n");
		System.out.println("Using MAVEN_OPTS: " + System.getenv("MAVEN_OPTS") + "\n");

		serverId = wizard.promptForExistingServerIdIfMissing(serverId);

		Server server = Server.loadServer(serverId);
		String jdk = System.getProperty("java.version");

		Version platformVersion = new Version(server.getPlatformVersion());
		if(platformVersion.getMajorVersion() == 1){
			if((jdk.startsWith("1.8"))){
				wizard.showMessage("Please note that it is not recommended to run OpenMRS platform "+server.getPlatformVersion()+" on JDK 8.\n");
			}
		} else if (platformVersion.getMajorVersion() == 2){
			if(!(jdk.startsWith("1.8"))){
				wizard.showJdkErrorMessage(jdk, server.getPlatformVersion(), "JDK 1.8", server.getPropertiesFile().getPath());
				throw new MojoExecutionException(String.format("The JDK %s is not compatible with OpenMRS Platform %s. ",
						"JDK 1.8", server.getPlatformVersion()));
			}
		} else {
			throw new MojoExecutionException("Invalid server platform version: "+platformVersion.toString());
		}

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
			if (isWatchApi()) {
				wizard.showMessage("Hot redeployment of API classes and UI framework changes enabled for:");
			} else {
				wizard.showMessage("Hot redeployment of UI framework changes enabled for:");
			}
			int i = 1;
			List<String> list = new ArrayList<String>();
			for (Project project : watchedProjects) {
				System.setProperty("uiFramework.development." + project.getArtifactId(), project.getPath());

				if (isWatchApi()) {
					System.setProperty(project.getArtifactId() + ".development.directory", project.getPath());
				}
				list.add(String.format("%d) %s:%s at %s", i, project.getGroupId(), project.getArtifactId(), project.getPath()));
				i++;
			}
			wizard.showMessage(StringUtils.join(list.iterator(), "\n"));
			wizard.showMessage("");
		}
	}

	private boolean isWatchApi() {
		return Boolean.TRUE.equals(watchApi);
	}

}
