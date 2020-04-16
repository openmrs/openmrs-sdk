package org.openmrs.maven.plugins;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @goal run-tomcat
 * @requiresProject false
 */
public class RunTomcat extends AbstractMojo {


	/**
	 * The project currently being build.
	 *
	 * @parameter  property="project"
	 */
	MavenProject mavenProject;

	/**
	 * The current Maven session.
	 *
	 * @parameter  property="session"
	 */
	MavenSession mavenSession;

	/**
	 * The Maven BuildPluginManager component.
	 *
	 * @component
	 * @required
	 */
	BuildPluginManager pluginManager;

	/**
	 * @parameter  property="serverId"
	 */
	private String serverId;

	/**
	 * @parameter  property="port"
	 */
	private Integer port;

	/**
	 * @parameter property="watchApi"
	 */
	private Boolean watchApi;

	/**
	 * @component
	 * @required
	 */
	private Wizard wizard;

	public RunTomcat() {
	}

	public RunTomcat(String serverId, Integer port, MavenSession mavenSession, MavenProject mavenProject, BuildPluginManager pluginManager, Wizard wizard) {
		this.serverId = serverId;
		this.port = port;
		this.wizard = wizard;
		this.mavenProject = mavenProject;
		this.mavenSession = mavenSession;
		this.pluginManager = pluginManager;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		wizard.showMessage("\nUsing JAVA_HOME: " + System.getProperty("java.home"));
		wizard.showMessage("Using MAVEN_OPTS: " + System.getenv("MAVEN_OPTS"));

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
        if (tempDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(tempDirectory);
            }
            catch (IOException e) {
                throw new MojoExecutionException("Unable to delete existing tmp directory at " + tempDirectory, e);
            }
        }
		tempDirectory.mkdirs();

		String warFile = "openmrs.war";
		File serverPath = server.getServerDirectory();
		for (File file : serverPath.listFiles()) {
			if ((file.getName().endsWith(".war"))) {
				warFile = file.getName();
				break;
			}
		}

		if(StringUtils.isNotBlank(server.getContainerId())){
			new DockerHelper(mavenProject, mavenSession, pluginManager, wizard).runDbContainer(
					server.getContainerId(),
					server.getDbUri(),
					server.getDbUser(),
					server.getDbPassword());
		}

		wizard.showMessage("Starting Tomcat...\n");

		Tomcat tomcat = new Tomcat();
		if (port == null) {
			port = 8080;
		}
		tomcat.setPort(port);
		tomcat.setBaseDir(tempDirectory.getAbsolutePath());
		tomcat.getHost().setAppBase(tempDirectory.getAbsolutePath());
		tomcat.getHost().setAutoDeploy(true);
		tomcat.getHost().setDeployOnStartup(true);
		tomcat.getConnector().setURIEncoding("UTF-8");
		Context context = tomcat.addWebapp(tomcat.getHost(), "/openmrs", new File(serverPath, warFile).getAbsolutePath());

		System.setProperty("OPENMRS_INSTALLATION_SCRIPT",
				new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath());
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", serverPath.getAbsolutePath() + File.separator);

		setServerCustomProperties(server);

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

	private void setServerCustomProperties(Server server) {
		HashMap<String, String> customProperties = server.getCustomProperties();
		for(String key: customProperties.keySet()){
			System.setProperty(key, customProperties.get(key));
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
