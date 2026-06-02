package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DistributionBuilder;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.APPEND;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_PROMPT;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_PROMPT;

/**
 * Create docker configuration for distributions.
 */
@Mojo(name = "build-distro", requiresProject = false)
public class BuildDistro extends AbstractTask {

	private static final String DEFAULT_SQL_DUMP = Server.CLASSPATH_SCRIPT_PREFIX + "openmrs-platform.sql";

	private static final String OPENMRS_WAR = "openmrs.war";

	private static final String OPENMRS_DISTRO_PROPERTIES = "openmrs-distro.properties";

	private static final String DOCKER_COMPOSE_PATH = "build-distro/docker-compose.yml";

	private static final String DOCKER_COMPOSE_OVERRIDE_PATH = "build-distro/docker-compose.override.yml";

	private static final String DOCKER_COMPOSE_PROD_PATH = "build-distro/docker-compose.prod.yml";

	private static final String README_PATH = "build-distro/README.md";

	private static final String DISTRIBUTION_VERSION_PROMPT = "You can build the following versions of distribution";

	private static final String DUMP_PREFIX = "CREATE DATABASE IF NOT EXISTS `openmrs`;\n\n USE `openmrs`;\n\n";

	private static final String DB_DUMP_PATH = "dbdump" + File.separator + "dump.sql";

	private static final String WAR_FILE_MODULES_DIRECTORY_NAME = "bundledModules";

	private static final String WEB = "web";

	private static final String DOCKER_COMPOSE_YML = "docker-compose.yml";

	private static final String DOCKER_COMPOSE_PROD_YML = "docker-compose.prod.yml";

	private static final String DOCKER_COMPOSE_OVERRIDE_YML = "docker-compose.override.yml";

	/**
	 * The Docker image namespace (i.e. the registry organisation) to use in the generated Dockerfile.
	 * Defaults to {@code openmrs}.
	 */
	static final String DOCKER_IMAGE_NAMESPACE = "docker.image.namespace";

	/**
	 * The Docker image repository to use in the generated Dockerfile.
	 * Defaults to {@code openmrs-core}.
	 */
	static final String DOCKER_IMAGE_REPOSITORY = "docker.image.repository";

	/**
	 * The full Docker image tag to use in the generated Dockerfile, e.g. {@code 2.7.0-amazoncorretto-11}.
	 * When set, {@link #DOCKER_IMAGE_OPENMRS_VERSION} and {@link #DOCKER_IMAGE_JAVA_VERSION} are ignored.
	 */
	static final String DOCKER_IMAGE_TAG = "docker.image.tag";

	/**
	 * An explicit Docker image tag OpenMRS version component to use, overriding the default registry probe.
	 * Ignored when {@link #DOCKER_IMAGE_TAG} is set.
	 * When not set, the SDK probes the registry (local daemon then Docker Hub) for the best available
	 * tag matching the exact platform version, then {@code major.minor.x}, then
	 * {@code major.minor.x-nightly}.
	 */
	static final String DOCKER_IMAGE_OPENMRS_VERSION = "docker.image.openmrsVersion";

	/**
	 * Optional Java variant suffix appended to {@link #DOCKER_IMAGE_OPENMRS_VERSION} to form the full tag,
	 * e.g. setting this to {@code amazoncorretto-11} with an OpenMRS version of {@code 2.7.0} produces the
	 * tag {@code 2.7.0-amazoncorretto-11}.
	 * Ignored when {@link #DOCKER_IMAGE_TAG} is set.
	 */
	static final String DOCKER_IMAGE_JAVA_VERSION = "docker.image.javaVersion";

	private static final Logger log = LoggerFactory.getLogger(BuildDistro.class);

	/**
	 * Path to the openmrs-distro.properties file.
	 */
	@Parameter(property = "distro")
	private String distro;

	/**
	 * Directory for generated files. (default to 'docker')
	 */
	@Parameter(property = "dir")
	private String dir;

	/**
	 * SQL script for database configuration.
	 */
	@Parameter(property = "dbSql")
	private String dbSql;

	/**
	 * Causes npm to completely ignore peerDependencies
	 */
	@Parameter(property = "ignorePeerDependencies", defaultValue = "true")
	private boolean ignorePeerDependencies;

	/**
	 * Override the reuseNodeCache property that controls whether the SDK reuse the NPM cache after the setup
	 */
	@Parameter(property = "reuseNodeCache")
	public Boolean overrideReuseNodeCache;

	/**
	 * Instead of creating a `modules` folder in the distro directory, will put modules inside
	 * the war file in `/webapp/src/main/webapp/WEB-INF/bundledModules`
	 */
	@Parameter(defaultValue = "false", property = "bundled")
	boolean bundled;

	/**
	 * Flag to indicate whether to delete the target directory or not.
	 */
	@Parameter(defaultValue = "false", property = "reset")
	private boolean reset;

	@Parameter(property = "appShellVersion")
	private String appShellVersion;

	/**
	 * Skip generating the default docker-compose.yml, docker-compose.override.yml, and
	 * docker-compose.prod.yml files.  Use this when you want to supply your own compose
	 * files, e.g. via the maven-resources-plugin, rather than using the SDK defaults.
	 */
	@Parameter(defaultValue = "false", property = "skipDockerCompose")
	private boolean skipDockerCompose;

	/**
	 * Skip generating the default Dockerfile.  Use this when you want to supply your own
	 * Dockerfile, e.g. via the maven-resources-plugin, rather than using the SDK defaults.
	 */
	@Parameter(defaultValue = "false", property = "skipDockerfile")
	private boolean skipDockerfile;

	/**
	 * Skip generating the default README.md.
	 */
	@Parameter(defaultValue = "false", property = "skipReadme")
	private boolean skipReadme;

	@Override
	public void executeTask() throws MojoExecutionException, MojoFailureException {
		File buildDirectory = getBuildDirectory();

		File userDir = new File(System.getProperty("user.dir"));

		DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
		Distribution distribution = null;

		if (distro == null) {
			File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);
			if (distroFile.exists()) {
				wizard.showMessage("Building distribution from the distro file at " + distroFile + "...\n");
				distribution = builder.buildFromFile(distroFile);
			}
			else if (Project.hasProject(userDir)) {
				wizard.showMessage("Building distribution from the source at " + userDir + "...\n");
				Project config = Project.loadProject(userDir);
				new Build(this).buildProject(config);
				String coordinates = config.getGroupId() + ":" + config.getArtifactId() + ":" + config.getVersion();
				Artifact distroArtifact = DistroHelper.parseDistroArtifact(coordinates, versionsHelper);
				distribution = builder.buildFromArtifact(distroArtifact);
			}
		}
		else if (StringUtils.isNotBlank(distro)) {
			distribution = distroHelper.resolveDistributionForStringSpecifier(distro, versionsHelper);
		}

		if (distribution == null) {

			List<String> options = new ArrayList<>();
			options.add(REFAPP_2X_PROMPT);
			options.add(REFAPP_3X_PROMPT);

			Artifact artifact = null;
			String choice = wizard.promptForMissingValueWithOptions("You can setup following servers", null, null, options);
			switch (choice) {
				case REFAPP_2X_PROMPT:
					artifact = wizard.promptForRefApp2xArtifact(versionsHelper);
					break;
				case REFAPP_3X_PROMPT:
					artifact = wizard.promptForRefApp3xArtifact(versionsHelper);
			}
			distribution = builder.buildFromArtifact(artifact);
		}

		if (distribution == null) {
			throw new MojoExecutionException("The distro you specified, '" + distro + "' could not be retrieved");
		}

		String distroName = buildDistro(buildDirectory, distribution);

		wizard.showMessage(
				"The '" + distroName + "' distribution created! To start up the server run 'docker-compose up' from "
						+ buildDirectory.getAbsolutePath() + "\n");
	}

	private File getBuildDirectory() throws MojoExecutionException {
		final File targetDir;
		if (StringUtils.isBlank(dir)) {
			String directory = wizard.promptForValueIfMissingWithDefault(
					"Specify build directory for generated files (-Ddir, default: 'docker')", dir, "dir", "docker");
			targetDir = new File(directory);
		} else {
			targetDir = new File(dir);
		}

		if (targetDir.exists()) {
			if (targetDir.isDirectory()) {
				if (!reset) {
					if (isDockerComposeCreated(targetDir)) {
						wizard.showMessage("The directory at '" + targetDir.getAbsolutePath()
								+ "' contains docker config. Only modules and openmrs.war will be overriden");
						deleteDistroFiles(new File(targetDir, WEB));
					} else if (targetDir.list().length != 0) {
						wizard.showMessage("The directory at '" + targetDir.getAbsolutePath()
								+ "' is not empty. All its content will be lost.");
						boolean chooseDifferent = wizard.promptYesNo("Would you like to choose a different directory?");
						if (chooseDifferent) {
							return getBuildDirectory();
						} else {
							deleteDirectory(targetDir);
						}
					}
				} else {
					deleteDirectory(targetDir);
				}
			} else {
				wizard.showMessage("The specified path '" + dir + "' is not a directory.");
				return getBuildDirectory();
			}
		} else {
			targetDir.mkdirs();
		}

		dir = targetDir.getAbsolutePath();

		return targetDir;
	}

	private void deleteDistroFiles(File targetDir) {
		try {
			FileUtils.deleteDirectory(new File(targetDir, "modules"));
			new File(targetDir, OPENMRS_WAR).delete();
			new File(targetDir, OPENMRS_DISTRO_PROPERTIES).delete();
		}
		catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void deleteDirectory(File targetDir) throws MojoExecutionException {
		try {
			FileUtils.cleanDirectory(targetDir);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not clean up directory \"" + targetDir.getAbsolutePath() + "\" " + e.getMessage(), e);
		}
	}

	private String buildDistro(File targetDirectory, Distribution distribution) throws MojoExecutionException {

		DistroProperties distroProperties = distribution.getEffectiveProperties();

		Version platformVersion = new Version(distroProperties.getPlatformVersion());
		int majorVersion = platformVersion.getMajorVersion();

		// First do content package validation
		distroHelper.validateDistribution(distroProperties);

		InputStream dbDumpStream;
		wizard.showMessage("Downloading modules...\n");

		String distroName = adjustImageName(distroProperties.getName());

		File web = new File(targetDirectory, WEB);
		web.mkdirs();

		moduleInstaller.installModules(distroProperties.getWarArtifacts(), web.getAbsolutePath());
		renameWebApp(web);

		if (bundled) {
			try {
				ZipFile warfile = new ZipFile(new File(web, OPENMRS_WAR));
				File tempDir = new File(web, "WEB-INF");
				tempDir.mkdir();
				moduleInstaller.installModules(distroProperties.getModuleArtifacts(), new File(tempDir, WAR_FILE_MODULES_DIRECTORY_NAME).getAbsolutePath());

				File owasDir = new File(tempDir, "bundledOwas");
				owasDir.mkdir();
				downloadOWAs(targetDirectory, distroProperties, owasDir);
				spaInstaller.installFromDistroProperties(tempDir, distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
				File frontendDir = new File(tempDir, "frontend");
				if (frontendDir.exists()) {
					frontendDir.renameTo(new File(tempDir, "bundledFrontend"));
				}
				warfile.addFolder(tempDir, new ZipParameters());

				// TODO: If the bundled war should have config and content, then add those here.
				try {
					FileUtils.deleteDirectory(tempDir);
				}
				catch (IOException e) {
					throw new MojoExecutionException("Failed to remove " + tempDir.getName() + " file " + e.getMessage(), e);
				}
			}
			catch (ZipException e) {
				throw new MojoExecutionException("Failed to bundle modules into *.war file " + e.getMessage(), e);
			}
		}
		else {
			File modulesDir = new File(web, "modules");
			modulesDir.mkdir();
			moduleInstaller.installModules(distroProperties.getModuleArtifacts(), modulesDir.getAbsolutePath());

			File frontendDir = new File(web, "frontend");
			frontendDir.mkdir();

			File configDir = new File(web, SDKConstants.OPENMRS_SERVER_CONFIGURATION);
			configDir.mkdir();
			configurationInstaller.installToDirectory(configDir, distroProperties);
			contentHelper.installBackendConfig(distroProperties, configDir);
			spaInstaller.installFromDistroProperties(web, distroProperties, ignorePeerDependencies, overrideReuseNodeCache);

			File owasDir = new File(web, "owa");
			owasDir.mkdir();
			downloadOWAs(targetDirectory, distroProperties, owasDir);
		}

		if (majorVersion >= 2) {
			File openmrsCoreDir = new File(web, "openmrs_core");
			openmrsCoreDir.mkdir();
			new File(web, "openmrs.war").renameTo(new File(openmrsCoreDir, "openmrs.war"));
			new File(web, "modules").renameTo(new File(web, "openmrs_modules"));
			new File(web, "frontend").renameTo(new File(web, "openmrs_spa"));
			new File(web, "configuration").renameTo(new File(web, "openmrs_config"));
			new File(web, "owa").renameTo(new File(web, "openmrs_owas"));
		}

		if (!skipDockerCompose) {
			wizard.showMessage("Creating Docker Compose configuration...\n");
			writeDockerCompose(targetDirectory);
			// .env and log4j configs belong to the compose setup: .env supplies variable
			// substitution for the compose files; the log4j files are mounted by the override.
			copyBuildDistroResource(".env", new File(targetDirectory, ".env"));
			if (!isPlatform2point5AndAbove(platformVersion)) {
				appendToEnvFile(new File(targetDirectory, ".env"), "OMRS_DB_IMAGE", "mysql:5.6");
			}
			copyBuildDistroResource("log4j.properties", new File(web, "log4j.properties"));
			copyBuildDistroResource("log4j2.xml", new File(web, "log4j2.xml"));
		}
		if (!skipReadme) {
			writeReadme(targetDirectory);
		}
		if (!skipDockerfile) {
			// startup.sh, setenv.sh, and wait-for-it.sh are COPY'd into the image by the
			// Dockerfile, so they belong to the Dockerfile group.
			if (!isPlatform2point5AndAbove(platformVersion)) {
				copyBuildDistroResource("setenv.sh", new File(web, "setenv.sh"));
				copyBuildDistroResource("startup.sh", new File(web, "startup.sh"));
				copyBuildDistroResource("wait-for-it.sh", new File(web, "wait-for-it.sh"));
			}
			copyDockerfile(web, distroProperties);
		}
		distroProperties.saveTo(web);

		dbDumpStream = getSqlDumpStream(StringUtils.isNotBlank(dbSql) ? dbSql : distroProperties.getSqlScriptPath(),
				targetDirectory, distribution.getArtifact());
		if (dbDumpStream != null) {
			copyDbDump(targetDirectory, dbDumpStream);
		}
		//clean up extracted sql file
		cleanupSqlFiles(targetDirectory);

		return distroName;
	}

	private void downloadOWAs(File targetDirectory, DistroProperties distroProperties, File owasDir)
			throws MojoExecutionException {
		List<Artifact> owas = distroProperties.getOwaArtifacts();
		if (!owas.isEmpty()) {
			wizard.showMessage("Downloading OWAs...\n");
			for (Artifact owa : owas) {
				wizard.showMessage("Downloading OWA: " + owa);
				owaHelper.downloadOwa(owasDir, owa, moduleInstaller);
			}
		}
	}

	private boolean isDockerComposeCreated(File targetDir) {
		File dockerComposeOverride = new File(targetDir, DOCKER_COMPOSE_OVERRIDE_YML);
		File dockerCompose = new File(targetDir, DOCKER_COMPOSE_YML);
		File dockerComposeProd = new File(targetDir, DOCKER_COMPOSE_PROD_YML);
		return dockerCompose.exists() && dockerComposeOverride.exists() && dockerComposeProd.exists();
	}

	void copyDockerfile(File targetDirectory, DistroProperties distroProperties) throws MojoExecutionException {
		Version platformVersion = new Version(distroProperties.getPlatformVersion());
		int majorVersion = platformVersion.getMajorVersion();

		String namespace = distroProperties.getParam(DOCKER_IMAGE_NAMESPACE, null);
		String repository = distroProperties.getParam(DOCKER_IMAGE_REPOSITORY, null);

		// For versions < 2.5, use the built-in Dockerfiles if no specific image information is configured
		boolean isLowerThan2point5 = !isPlatform2point5AndAbove(platformVersion);
		if (isLowerThan2point5 && namespace == null && repository == null) {
			String dockerFile = "Dockerfile-jre" + (majorVersion == 1 ? "7" : "8") + (bundled ? "-bundled" : "");
			copyBuildDistroResource(dockerFile, new File(targetDirectory, "Dockerfile"));
		}
		else {
			namespace = StringUtils.defaultIfBlank(namespace, "openmrs");
			repository = StringUtils.defaultIfBlank(repository, "openmrs-core");
			String dockerImageTag = distroProperties.getParam(DOCKER_IMAGE_TAG);
			if (StringUtils.isBlank(dockerImageTag)) {
				String imageOpenmrsVersion = distroProperties.getParam(DOCKER_IMAGE_OPENMRS_VERSION, null);
				if (imageOpenmrsVersion == null) {
					// No explicit version — probe the registry for the best available tag for the war.
					// SNAPSHOT versions and versions without a dedicated Docker image automatically
					// fall back to the .x rolling tag or the .x-nightly tag, whichever exists first.
					dockerImageTag = resolveDockerImageTag(namespace, repository, distroProperties.getPlatformVersion());
				} else {
					dockerImageTag = imageOpenmrsVersion;
				}
				String javaVersion = distroProperties.getParam(DOCKER_IMAGE_JAVA_VERSION, null);
				if (StringUtils.isNotBlank(javaVersion)) {
					dockerImageTag += "-" + javaVersion;
				}
			}
			File dockerFile = new File(targetDirectory, "Dockerfile");
			List<String> lines = new ArrayList<>();
			lines.add("# Docker configuration automatically generated by openmrs SDK");
			lines.add("");
			lines.add("FROM " + namespace + "/" + repository + ":" + dockerImageTag);
			lines.add("");
			lines.add("COPY openmrs_core/openmrs.war /openmrs/distribution/openmrs_core/");
			lines.add("COPY openmrs-distro.properties /openmrs/distribution/");
			if (!bundled) {
				lines.add("COPY openmrs_modules /openmrs/distribution/openmrs_modules");
				lines.add("COPY openmrs_owas /openmrs/distribution/openmrs_owas");
				lines.add("COPY openmrs_config /openmrs/distribution/openmrs_config");
				lines.add("COPY openmrs_spa /openmrs/distribution/openmrs_spa");
			}
			lines.add("");
			for (String extraProperty : distroProperties.getPropertiesNames()) {
				String propertyValue = distroProperties.getPropertyValue(extraProperty);
				if (StringUtils.isBlank(propertyValue)) {
					propertyValue = distroProperties.getPropertyDefault(extraProperty);
				}
				if (StringUtils.isNotBlank(propertyValue)) {
					lines.add("ENV OMRS_EXTRA_" + extraProperty.replace(".", "_") + "=\"" + propertyValue + "\"");
				}
			}
			try {
				FileUtils.writeLines(dockerFile, "UTF-8", lines);
			}
			catch (IOException e) {
				throw new MojoExecutionException("Failed to write Dockerfile: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Resolves the best available Docker image tag for the given platform version.
	 * Candidates are evaluated in preference order — each candidate is checked
	 * everywhere (local daemon first, then Docker Hub) before the next is considered:
	 * <ol>
	 *   <li>Exact release version (e.g. {@code 2.6.16}) — local, then Hub</li>
	 *   <li>Minor-version rolling tag (e.g. {@code 2.6.x}) — local, then Hub</li>
	 *   <li>Nightly rolling tag (e.g. {@code 2.6.x-nightly}) — local, then Hub</li>
	 * </ol>
	 * Throws if none of the candidates are found anywhere, with a message directing the
	 * user to set {@link #DOCKER_IMAGE_TAG} or {@link #DOCKER_IMAGE_OPENMRS_VERSION} explicitly.
	 */
	String resolveDockerImageTag(String namespace, String repository, String platformVersion) throws MojoExecutionException {
		// SNAPSHOT versions never have a dedicated Docker image — strip the suffix and resolve
		// against the release version so we fall through to the .x or .x-nightly rolling tag.
		String releaseVersion = StringUtils.removeEnd(platformVersion, "-SNAPSHOT");
		Version version = new Version(releaseVersion);
		String rollingTag = version.getMajorVersion() + "." + version.getMinorVersion() + ".x";
		String[] candidates = { releaseVersion, rollingTag, rollingTag + "-nightly" };

		for (String candidate : candidates) {
			String image = namespace + "/" + repository + ":" + candidate;
			if (dockerImageExistsLocally(namespace, repository, candidate)) {
				log.info("Using local Docker image {}", image);
				return candidate;
			}
			if (dockerImageExistsOnHub(namespace, repository, candidate)) {
				log.info("Using Docker Hub image {}", image);
				return candidate;
			}
		}

		throw new MojoExecutionException(
				"Could not find a Docker image for " + namespace + "/" + repository
						+ " matching platform version " + platformVersion
						+ ". Checked tags: " + String.join(", ", candidates)
						+ ". Set the " + DOCKER_IMAGE_TAG + " or " + DOCKER_IMAGE_OPENMRS_VERSION
						+ " property in your distro properties to specify a tag explicitly.");
	}

	/**
	 * Returns {@code true} if the given image tag is already present in the local Docker daemon.
	 * Uses the docker-java API directly rather than shelling out to the {@code docker} CLI.
	 * Protected to allow overriding in tests.
	 */
	protected boolean dockerImageExistsLocally(String namespace, String repository, String tag) {
		DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
		try (DockerClient client = DockerClientBuilder.getInstance(config)
				.withDockerHttpClient(new ApacheDockerHttpClient.Builder()
						.dockerHost(config.getDockerHost())
						.sslConfig(config.getSSLConfig())
						.build())
				.build()) {
			client.inspectImageCmd(namespace + "/" + repository + ":" + tag).exec();
			return true;
		}
		catch (NotFoundException e) {
			return false;
		}
		catch (Exception e) {
			log.debug("Could not check local Docker daemon for image {}/{}:{} — {}", namespace, repository, tag, e.getMessage());
			return false;
		}
	}

	/**
	 * Returns {@code true} if the given image tag exists in the Docker Hub registry.
	 * Protected to allow overriding in tests without making real network calls.
	 */
	protected boolean dockerImageExistsOnHub(String namespace, String repository, String tag) {
		try {
			URL url = new URL("https://hub.docker.com/v2/repositories/" + namespace + "/" + repository + "/tags/" + tag + "/");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5_000);
			conn.setReadTimeout(5_000);
			conn.setRequestMethod("GET");
			try {
				return conn.getResponseCode() == 200;
			} finally {
				conn.disconnect();
			}
		}
		catch (Exception e) {
			log.warn("Could not check Docker Hub for {}/{}:{} — {}", namespace, repository, tag, e.getMessage());
			return false;
		}
	}

	private static void appendToEnvFile(File envFile, String key, String value) throws MojoExecutionException {
		try {
			Files.write(envFile.toPath(), ("\n" + key + "=" + value + "\n").getBytes(StandardCharsets.UTF_8), APPEND);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to write " + key + " to " + envFile + ": " + e.getMessage(), e);
		}
	}

	private boolean isPlatform2point5AndAbove(Version platformVersion) {
		return isAtOrAbovePlatformVersion(platformVersion, 2, 5);
	}

	private boolean isAtOrAbovePlatformVersion(Version platformVersion, int majorVersion, int minorVersion) {
		return platformVersion.getMajorVersion() > majorVersion
				|| (platformVersion.getMajorVersion() == majorVersion && platformVersion.getMinorVersion() >= minorVersion);
	}

	/**
	 * name of sql dump file is unknown, so wipe all files with 'sql' extension
	 */
	private void cleanupSqlFiles(File targetDirectory) {
		File[] sqlFiles = targetDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".sql");
			}
		});
		for (File sql : sqlFiles) {
			FileUtils.deleteQuietly(sql);
		}
	}

	private void writeDockerCompose(File targetDirectory) throws MojoExecutionException {
		writeTemplatedFile(targetDirectory, DOCKER_COMPOSE_PATH, DOCKER_COMPOSE_YML);
		writeTemplatedFile(targetDirectory, DOCKER_COMPOSE_OVERRIDE_PATH, DOCKER_COMPOSE_OVERRIDE_YML);
		writeTemplatedFile(targetDirectory, DOCKER_COMPOSE_PROD_PATH, DOCKER_COMPOSE_PROD_YML);
	}

	private void writeReadme(File targetDirectory) throws MojoExecutionException {
		writeTemplatedFile(targetDirectory, README_PATH, "README.md");
	}

	private void writeTemplatedFile(File targetDirectory, String path, String filename) throws MojoExecutionException {
		URL composeUrl = getClass().getClassLoader().getResource(path);
		if (composeUrl == null) {
			throw new MojoExecutionException("Failed to find file '" + path + "' in classpath");
		}
		File compose = new File(targetDirectory, filename);
		if (!compose.exists()) {
			try (InputStream inputStream = composeUrl.openStream(); FileWriter composeWriter = new FileWriter(compose)) {
				String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
				composeWriter.write(content);
			}
			catch (IOException e) {
				throw new MojoExecutionException("Failed to write " + filename + " file " + e.getMessage(), e);
			}
		}
	}

	private String adjustImageName(String part) {
		return part != null ? part.replaceAll("\\s+", "").toLowerCase() : "";
	}

	private void copyDbDump(File targetDirectory, InputStream stream) throws MojoExecutionException {
		File dbDump = new File(targetDirectory, DB_DUMP_PATH);
		try {
			dbDump.getParentFile().mkdirs();
			dbDump.createNewFile();
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to create SQL dump file " + e.getMessage(), e);
		}

		try (FileWriter writer = new FileWriter(dbDump);
			 BufferedInputStream bis = new BufferedInputStream(stream)) {
			writer.write(DUMP_PREFIX);

			int c;
			while ((c = bis.read()) != -1) {
				writer.write(c);
			}

			writer.write("\n" + SDKConstants.RESET_SEARCH_INDEX_SQL + "\n");
			writer.flush();
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to create dump file " + e.getMessage(), e);
		}
		finally {
			IOUtils.closeQuietly(stream);
		}
	}

	private InputStream getSqlDumpStream(String sqlScriptPath, File targetDirectory, Artifact distroArtifact) throws MojoExecutionException {
		InputStream stream = null;

		if (sqlScriptPath == null) {
			return null;
		}

		try {
			if (sqlScriptPath.startsWith(Server.CLASSPATH_SCRIPT_PREFIX)) {
				String sqlScript = sqlScriptPath.replace(Server.CLASSPATH_SCRIPT_PREFIX, "");
				URL resourceUrl = getClass().getClassLoader().getResource(sqlScript);
				if (resourceUrl != null) {
					stream = resourceUrl.openStream();
				} else {
					if (distroArtifact != null && distroArtifact.isValid()) {
						File extractedSqlFile = distroHelper
								.extractFileFromDistro(targetDirectory, distroArtifact, sqlScript);
						stream = Files.newInputStream(extractedSqlFile.toPath());
					}
				}
			} else {
				File scriptFile = new File(sqlScriptPath);
				if (scriptFile.exists()) {
					stream = Files.newInputStream(scriptFile.toPath());
				} else {
					throw new MojoExecutionException("Specified script \"" + scriptFile.getAbsolutePath() + "\" does not exist.");
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to open stream to sql dump script " + e.getMessage(), e);
		}
		return stream;
	}

	private void copyBuildDistroResource(String resource, File target) throws MojoExecutionException {
		URL resourceUrl = getClass().getClassLoader().getResource("build-distro/web/" + resource);
		if (resourceUrl != null && !target.exists()) {
			try {
				FileUtils.copyURLToFile(resourceUrl, target);
			}
			catch (IOException e) {
				throw new MojoExecutionException(
						"Failed to copy file from classpath: " + resourceUrl + " to " + target.getAbsolutePath() + e.getMessage(), e);
			}
		}
	}

	private void renameWebApp(File targetDirectory) throws MojoExecutionException {
		File[] warFiles = targetDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".war");
			}
		});

		if (warFiles != null) {
			for (File file : warFiles) {
				wizard.showMessage("file:" + file.getAbsolutePath());
			}

			wizard.showMessage("target:" + targetDirectory);

			if (warFiles.length == 1) {
				boolean renameSuccess = warFiles[0].renameTo(new File(targetDirectory, OPENMRS_WAR));
				if (!renameSuccess) {
					throw new MojoExecutionException("Failed to rename openmrs '.war' file");
				}
			} else {
				throw new MojoExecutionException("Distro should contain only single war file");
			}
		}
	}
}
