package org.openmrs.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.OpenMRSSDKRedirectServlet;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

@Mojo(name = "run-tomcat", requiresProject = false)
public class RunTomcat extends AbstractMojo {

	/**
	 * The project currently being build.
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject mavenProject;

	/**
	 * The current Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	MavenSession mavenSession;

	@Parameter(property = "serverId")
	private String serverId;

	@Parameter(property = "port")
	private Integer port;

	@Parameter(property = "watchApi")
	private Boolean watchApi;

	/**
	 * The Maven BuildPluginManager component.
	 */
	@Component
	BuildPluginManager pluginManager;

	@Component
	private Wizard wizard;

	public RunTomcat() {
	}

	public RunTomcat(String serverId, Integer port, MavenSession mavenSession, MavenProject mavenProject,
			BuildPluginManager pluginManager, Wizard wizard) {
		this.serverId = serverId;
		this.port = port;
		this.wizard = wizard;
		this.mavenProject = mavenProject;
		this.mavenSession = mavenSession;
		this.pluginManager = pluginManager;
	}

	@Override
	public void execute() throws MojoExecutionException {
		wizard.showMessage("\nUsing JAVA_HOME: " + System.getProperty("java.home"));
		wizard.showMessage("Using MAVEN_OPTS: " + System.getenv("MAVEN_OPTS"));

		serverId = wizard.promptForExistingServerIdIfMissing(serverId);

		Server server = Server.loadServer(serverId);
		String jdk = System.getProperty("java.version");

		Version platformVersion = new Version(server.getPlatformVersion());
		if (platformVersion.getMajorVersion() == 1) {
			if ((jdk.startsWith("1.8"))) {
				wizard.showMessage(
						"Please note that it is not recommended to run OpenMRS platform " + server.getPlatformVersion()
								+ " on JDK 8.\n");
			}
		} else if (platformVersion.getMajorVersion() == 2) {
			if (!jdk.startsWith("1.8") && platformVersion.getMinorVersion() < 4) {
				wizard.showJdkErrorMessage(jdk, server.getPlatformVersion(), "JDK 1.8",
						server.getPropertiesFile().getPath());
				throw new MojoExecutionException(String.format("The JDK %s is not compatible with OpenMRS Platform %s. ",
						jdk, server.getPlatformVersion()));
			}
		} else {
			throw new MojoExecutionException("Invalid server platform version: " + platformVersion);
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
		Optional<File> war = Arrays.stream(Objects.requireNonNull(serverPath.listFiles())).filter(file -> file.getName().endsWith(".war")).findFirst();
		if(war.isPresent()) {
			warFile = war.get().getName();
		}

		if (StringUtils.isNotBlank(server.getContainerId())) {
			new DockerHelper(mavenProject, mavenSession, pluginManager, wizard).runDbContainer(
					server.getContainerId(),
					server.getDbUri(),
					server.getDbUser(),
					server.getDbPassword());
		}

		wizard.showMessage("Starting Tomcat...\n");

		Tomcat tomcat = new Tomcat();

		//Tomcat needs a directory for temp files. This should be the first
		//method called, else it will default to $PWD/tomcat.$PORT
		tomcat.setBaseDir(tempDirectory.getAbsolutePath());

		if (port == null) {
			port = 8080;
		}
		
		Context root = tomcat.addContext("", tempDirectory.getAbsolutePath());
		tomcat.addServlet("", "openmrsRedirectServlet", new OpenMRSSDKRedirectServlet());
		root.addServletMapping("/*", "openmrsRedirectServlet");
		tomcat.setPort(port);
		tomcat.getHost().setAppBase(tempDirectory.getAbsolutePath());
		tomcat.getHost().setAutoDeploy(true);
		tomcat.getHost().setDeployOnStartup(true);
		tomcat.getConnector().setURIEncoding("UTF-8");
		Context context = tomcat.addWebapp(tomcat.getHost(), "/openmrs", new File(serverPath, warFile).getAbsolutePath());
		context.addLifecycleListener(new OpenmrsStartupListener(wizard, port));

		System.setProperty("OPENMRS_INSTALLATION_SCRIPT",
				new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath());
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", serverPath.getAbsolutePath() + File.separator);

		setServerCustomProperties(server);

		setSystemPropertiesForWatchedProjects(server);

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(newTomcatClassLoader());
			WebappLoader tomcatLoader = new WebappLoader();
			context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
			context.setLoader(tomcatLoader);

			tomcat.start();
			tomcat.getServer().await();
		}
		catch (LifecycleException e) {
			throw new MojoExecutionException("Tomcat failed to start", e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void setServerCustomProperties(Server server) {
		Map<String, String> customProperties = server.getCustomProperties();
		customProperties.keySet().forEach(key -> System.setProperty(key, customProperties.get(key)));
	}

	protected ClassRealm newTomcatClassLoader() throws MojoExecutionException {
		try {
			ClassWorld world = new ClassWorld();

			return world.newRealm("tomcat", Thread.currentThread().getContextClassLoader());
		}
		catch (DuplicateRealmException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void setSystemPropertiesForWatchedProjects(Server server) {
		List<Project> watchedProjects = new ArrayList<>(server.getWatchedProjects());
		if (!watchedProjects.isEmpty()) {
			if (isWatchApi()) {
				wizard.showMessage("Hot redeployment of API classes and UI framework changes enabled for:");
			} else {
				wizard.showMessage("Hot redeployment of UI framework changes enabled for:");
			}
			List<String> list = new ArrayList<>();
			IntStream.range(0, watchedProjects.size()).forEach(i -> {
				Project project = watchedProjects.get(i);
				System.setProperty("uiFramework.development." + project.getArtifactId(), project.getPath());

				if (isWatchApi()) {
					System.setProperty(project.getArtifactId() + ".development.directory", project.getPath());
				}
				list.add(String.format("%d) %s:%s at %s", i, project.getGroupId(), project.getArtifactId(),
						project.getPath()));
			});
			wizard.showMessage(StringUtils.join(list.iterator(), "\n"));
			wizard.showMessage("");
		}
	}

	private boolean isWatchApi() {
		return Boolean.TRUE.equals(watchApi);
	}

	public static final class OpenmrsStartupListener implements LifecycleListener {

		private final Wizard wizard;

		private final int port;

		OpenmrsStartupListener(Wizard wizard, int port) {
			this.wizard = wizard;
			this.port = port;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (!Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
				return;
			}

			wizard.showMessage(
					String.format("OpenMRS is ready for you at http://localhost%s/openmrs/", port == 80 ? "" : ":" + port));
		}
	}
}
