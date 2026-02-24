package org.openmrs.maven.plugins.cargo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DockerHelper;
import org.openmrs.maven.plugins.utility.MavenEnvironment;
import org.openmrs.maven.plugins.utility.PlatformJdkValidator;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

/**
 * Runs an OpenMRS WAR inside an embedded servlet container (Tomcat or Jetty)
 * managed by the Cargo container abstraction library.
 *
 * This class is not a Maven Mojo itself; thin per-platform-version Mojo classes
 * delegate to it after ensuring the right container embed JARs are on the classpath.
 */
public class CargoContainerRunner {

	private final String containerId;
	private final String serverId;
	private final Integer port;
	private final Boolean watchApi;
	private final MavenEnvironment mavenEnvironment;
	private final Wizard wizard;

	public CargoContainerRunner(String containerId, String serverId, Integer port, Boolean watchApi,
			MavenEnvironment mavenEnvironment, Wizard wizard) {
		this.containerId = containerId;
		this.serverId = serverId;
		this.port = port;
		this.watchApi = watchApi;
		this.mavenEnvironment = mavenEnvironment;
		this.wizard = wizard;
	}

	public void run() throws MojoExecutionException {
		wizard.showMessage("\nUsing JAVA_HOME: " + System.getProperty("java.home"));
		wizard.showMessage("Using MAVEN_OPTS: " + System.getenv("MAVEN_OPTS"));

		String resolvedServerId = wizard.promptForExistingServerIdIfMissing(serverId);
		Server server = Server.loadServer(resolvedServerId);

		String jdk = System.getProperty("java.version");
		Version platformVersion = new Version(server.getPlatformVersion());
		PlatformJdkValidator.validate(platformVersion, jdk, wizard, server.getPropertiesFile());

		File tempDirectory = server.getServerTmpDirectory();
		if (tempDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(tempDirectory);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to delete existing tmp directory at " + tempDirectory, e);
			}
		}
		tempDirectory.mkdirs();

		String warFile = findWarFile(server.getServerDirectory());

		if (StringUtils.isNotBlank(server.getContainerId())) {
			new DockerHelper(mavenEnvironment).runDbContainer(
					server.getContainerId(),
					server.getDbUri(),
					server.getDbUser(),
					server.getDbPassword());
		}

		setSystemProperties(server);
		setWatchedProjectProperties(server);

		int effectivePort = port != null ? port : 8080;

		wizard.showMessage("Starting " + containerId + " container...\n");

		File redirectDir;
		try {
			redirectDir = createRedirectWebapp(tempDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to create redirect webapp", e);
		}

		// Cargo's setupConfigurationDir() (called during container.start()) requires
		// its config directory to be empty or previously Cargo-created. Since we put
		// the redirect webapp in tempDirectory above, give Cargo its own subdirectory.
		File cargoConfigDir = new File(tempDirectory, "cargo");
		cargoConfigDir.mkdirs();

		try {
			LocalConfiguration config = (LocalConfiguration) new DefaultConfigurationFactory()
					.createConfiguration(containerId, ContainerType.EMBEDDED,
							ConfigurationType.STANDALONE, cargoConfigDir.getAbsolutePath());
			config.setProperty(ServletPropertySet.PORT, String.valueOf(effectivePort));

			// Deploy a redirect page at the root context so / redirects to /openmrs/
			WAR redirectWar = new WAR(redirectDir.getAbsolutePath());
			redirectWar.setContext("/");
			config.addDeployable(redirectWar);

			// Deploy the OpenMRS WAR at /openmrs
			WAR openmrsWar = new WAR(new File(server.getServerDirectory(), warFile).getAbsolutePath());
			openmrsWar.setContext("openmrs");
			config.addDeployable(openmrsWar);

			EmbeddedLocalContainer container = (EmbeddedLocalContainer) new DefaultContainerFactory()
					.createContainer(containerId, ContainerType.EMBEDDED, config);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					container.stop();
				} catch (Exception e) {
					// best-effort shutdown
				}
				try {
					FileUtils.deleteDirectory(tempDirectory);
				} catch (Exception e) {
					// best-effort cleanup
				}
			}));

			container.start();

			wizard.showMessage(String.format("OpenMRS is ready for you at http://localhost%s/openmrs/",
					effectivePort == 80 ? "" : ":" + effectivePort));

			// Cargo's embedded start() returns after the container is up.
			// Block the main thread so the JVM stays alive until Ctrl+C / kill.
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to start container " + containerId, e);
		}
	}

	private void setSystemProperties(Server server) {
		File serverPath = server.getServerDirectory();
		System.setProperty("OPENMRS_INSTALLATION_SCRIPT",
				new File(serverPath, SDKConstants.OPENMRS_SERVER_PROPERTIES).getAbsolutePath());
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY",
				serverPath.getAbsolutePath() + File.separator);

		Map<String, String> customProperties = server.getCustomProperties();
		for (Map.Entry<String, String> entry : customProperties.entrySet()) {
			System.setProperty(entry.getKey(), entry.getValue());
		}
	}

	private void setWatchedProjectProperties(Server server) {
		Set<Project> watchedProjects = server.getWatchedProjects();
		if (watchedProjects.isEmpty()) {
			return;
		}

		boolean isWatchApi = Boolean.TRUE.equals(watchApi);
		if (isWatchApi) {
			wizard.showMessage("Hot redeployment of API classes and UI framework changes enabled for:");
		} else {
			wizard.showMessage("Hot redeployment of UI framework changes enabled for:");
		}

		int i = 1;
		for (Project project : watchedProjects) {
			System.setProperty("uiFramework.development." + project.getArtifactId(), project.getPath());
			if (isWatchApi) {
				System.setProperty(project.getArtifactId() + ".development.directory", project.getPath());
			}
			wizard.showMessage(String.format("%d) %s:%s at %s", i, project.getGroupId(),
					project.getArtifactId(), project.getPath()));
			i++;
		}
		wizard.showMessage("");
	}

	private String findWarFile(File serverDirectory) {
		String warFile = "openmrs.war";
		File[] files = serverDirectory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getName().endsWith(".war")) {
					warFile = file.getName();
					break;
				}
			}
		}
		return warFile;
	}

	/**
	 * Creates a minimal webapp directory containing an index.html that redirects to /openmrs/.
	 * Deployed as an exploded WAR to the root context so http://localhost:port/ redirects
	 * to http://localhost:port/openmrs/.
	 */
	private File createRedirectWebapp(File parentDir) throws IOException {
		File webappDir = new File(parentDir, "redirect-webapp");
		webappDir.mkdirs();

		File webInf = new File(webappDir, "WEB-INF");
		webInf.mkdirs();

		String indexHtml = "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "  <meta http-equiv=\"refresh\" content=\"0;url=/openmrs/\">\n"
				+ "  <title>Redirecting...</title>\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "  <p>Redirecting to <a href=\"/openmrs/\">/openmrs/</a>...</p>\n"
				+ "  <script>window.location.replace('/openmrs/' + window.location.search + window.location.hash);</script>\n"
				+ "</body>\n"
				+ "</html>\n";

		Files.write(new File(webappDir, "index.html").toPath(),
				indexHtml.getBytes(StandardCharsets.UTF_8));

		String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<web-app>\n"
				+ "  <welcome-file-list>\n"
				+ "    <welcome-file>index.html</welcome-file>\n"
				+ "  </welcome-file-list>\n"
				+ "</web-app>\n";

		Files.write(new File(webInf, "web.xml").toPath(),
				webXml.getBytes(StandardCharsets.UTF_8));

		return webappDir;
	}
}
