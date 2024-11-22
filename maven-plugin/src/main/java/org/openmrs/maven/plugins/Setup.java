package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.DistributionBuilder;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.ServerHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.maven.plugins.model.Artifact.GROUP_DISTRO;
import static org.openmrs.maven.plugins.utility.SDKConstants.PLATFORM_ARTIFACT_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_ARTIFACT_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_GROUP_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_2X_TYPE;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_ARTIFACT_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_GROUP_ID;
import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_TYPE;
import static org.openmrs.maven.plugins.utility.SDKConstants.SETUP_DEFAULT_PLATFORM_VERSION;


/**
 * Set up a new instance of OpenMRS server. It can be used for setting up a platform or a distribution. It prompts for any missing, but required parameters.
 */
@Mojo(name = "setup", requiresProject = false)
public class Setup extends AbstractServerTask {

	public static final String SETTING_UP_A_NEW_SERVER = "Setting up a new server...";

	public static final String SETUP_SERVERS_PROMPT = "You can setup the following servers";

	public static final String ENABLE_DEBUGGING_DEFAULT_MESSAGE =
			"If you want to enable remote debugging by default when running the server, "
					+ "\nspecify the %s here (e.g. 1044). Leave blank to disable debugging. \n(Do not do this on a production server)";

	private static final String O2_DISTRIBUTION = "2.x Distribution";

	private static final String PLATFORM = "Platform";

	private static final String O3_DISTRIBUTION = "O3 Distribution";

	private static final String CLASSPATH_SCRIPT_PREFIX = "classpath://";

	private static final String NO_DEBUGGING_DEFAULT_ANSWER = "no debugging";

	private static final int DEFAULT_PORT = 8080;

	/**
	 * DB Driver type
	 */
	@Parameter(property = "dbDriver")
	private String dbDriver;

	/**
	 * DB Uri
	 */
	@Parameter(property = "dbUri")
	private String dbUri;

	/**
	 * DB User
	 */
	@Parameter(property = "dbUser")
	private String dbUser;

	/**
	 * DB Pass
	 */
	@Parameter(property = "dbPassword")
	private String dbPassword;

	/**
	 * DB dump script to import
	 */
	@Parameter(property = "dbSql")
	private String dbSql;

	/**
	 * Docker host address
	 */
	@Parameter(property = "dockerHost")
	private String dockerHost;

	/**
	 * DB reset if exists
	 */
	@Parameter(property = "dbReset")
	private Boolean dbReset;

	/**
	 * Path to JDK Version
	 */
	@Parameter(property = "javaHome")
	private String javaHome;

	/**
	 * Path to installation.properties
	 */
	@Parameter(property = "file")
	private String file;

	/**
	 * Option to include demo data
	 */
	@Parameter(defaultValue = "false", property = "addDemoData")
	private boolean addDemoData;

	/**
	 * OpenMRS Distribution to setup in a format 'groupId:artifactId:version'. You can skip groupId, if it is 'org.openmrs.distro'.
	 */
	@Parameter(property = "distro")
	private String distro;

	/**
	 * OpenMRS Platform version to setup e.g. '1.11.5'.
	 */
	@Parameter(property = "platform")
	private String platform;

	@Parameter(property = "debug")
	private String debug;

	@Parameter(defaultValue = "false", property = "run")
	private boolean run;

	@Parameter(property = "appShellVersion")
	private String appShellVersion;

	@Parameter(property = "ignorePeerDependencies", defaultValue = "true")
	private boolean ignorePeerDependencies;

	@Parameter(property = "reuseNodeCache")
	public Boolean overrideReuseNodeCache;

	private ServerHelper serverHelper;

	public Setup() {
		super();
	}

	public Setup(AbstractServerTask other) {
		super(other);
	}

	public Setup(AbstractTask other) {
		super(other);
	}

	/**
	 * Gets the distro properties. The distro properties can come from a file specified by path
	 * or in the current directory or from a maven artifact.
	 * If no distro properties file can be found, `null` is returned. In this case, the distro
	 * file will be created after the database is initialized. Setup should proceed to install
	 * modules based on the OpenMRS WAR file for the given platform version.
	 * As of this writing, this function can return null only in platform mode.
	 *
	 * @param server An initialized Server instance
	 * @return distro properties instantiated by DistroHelper
	 * @throws MojoExecutionException
	 */
	private DistroProperties resolveDistroProperties(Server server) throws MojoExecutionException {
		DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());

		if (distro != null) {
			DistroProperties distroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
			if (distroProperties == null) {
				throw new MojoExecutionException("Distro " + distro + "could not be retrieved");
			}
			return distroProperties;
		}

		if (platform != null) {
			return resolveDistroPropertiesForPlatform(server, platform);
		}

		List<String> options = new ArrayList<>();
		DistroProperties distroProperties = distroHelper.getDistroPropertiesFromDir();
		if (distroProperties != null) {
			options.add(distroProperties.getName() + " " + distroProperties.getVersion() + " from current directory");
		}
		options.add(O3_DISTRIBUTION);
		options.add(O2_DISTRIBUTION);
		options.add(PLATFORM);
		String choice = wizard.promptForMissingValueWithOptions(SETUP_SERVERS_PROMPT, null, null, options);

		if (PLATFORM.equals(choice)) {
			return resolveDistroPropertiesForPlatform(server, platform);
		}

		if (O2_DISTRIBUTION.equals(choice)) {
			wizard.promptForRefAppVersionIfMissing(server, versionsHelper);
			Distribution distribution = builder.buildFromArtifact(new Artifact(REFAPP_2X_ARTIFACT_ID, server.getVersion(), REFAPP_2X_GROUP_ID, REFAPP_2X_TYPE));
			return distribution.getEffectiveProperties();
		}

		if (O3_DISTRIBUTION.equals(choice)) {
			wizard.promptForO3RefAppVersionIfMissing(server, versionsHelper);
			Distribution distribution = builder.buildFromArtifact(new Artifact(REFAPP_3X_ARTIFACT_ID, server.getVersion(), REFAPP_3X_GROUP_ID, REFAPP_3X_TYPE));
			return distribution.getEffectiveProperties();
		}

		// If here, it is because the option to set up from current directory was chosen, and these properties were already loaded, just return them
		return distroProperties;
	}

	private DistroProperties resolveDistroPropertiesForPlatform(Server server, String version) throws MojoExecutionException {
		Artifact platformArtifact = new Artifact(PLATFORM_ARTIFACT_ID, SETUP_DEFAULT_PLATFORM_VERSION, GROUP_DISTRO);
		version = wizard.promptForPlatformVersionIfMissing(version, versionsHelper.getSuggestedVersions(platformArtifact, 6));
		platformArtifact = DistroHelper.parseDistroArtifact(GROUP_DISTRO + ":" + PLATFORM_ARTIFACT_ID + ":" + version, versionsHelper);
		server.setPlatformVersion(platformArtifact.getVersion());
		try {
			DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
			Distribution distribution = builder.buildFromArtifact(platformArtifact);
			if (distribution != null) {
				return distribution.getEffectiveProperties();
			}
		}
		catch (MojoExecutionException ignored) {}
		return null;
	}

	/**
	 * Sets up a server based on an initialized Server instance and the provided
	 * distroProperties. Installs modules and other artifacts. Sets up the database.
	 * Writes openmrs-server.properties.
	 *
	 * @param server           An initialized server instance
	 * @param distroProperties Allowed to be null, only if this is a platform install
	 * @throws MojoExecutionException
	 */
	public void setup(Server server, DistroProperties distroProperties) throws MojoExecutionException {
		if (distroProperties != null) {

			// This is saving or prompting for any property values within the distro properties with a "property." prefix
			distroHelper.savePropertiesToServer(distroProperties, server);

			setServerVersionsFromDistroProperties(server, distroProperties);
			distroHelper.parseContentProperties(distroProperties);
			moduleInstaller.installModulesForDistro(server, distroProperties);

			ContentHelper.downloadAndMoveContentBackendConfig(server.getServerDirectory(), distroProperties, moduleInstaller, wizard);						

			if (spaInstaller != null) {
				spaInstaller.installFromDistroProperties(server.getServerDirectory(), distroProperties, ignorePeerDependencies, overrideReuseNodeCache);
			}

			installOWAs(server, distroProperties);
			configurationInstaller.installToServer(server, distroProperties);
		} else {
			moduleInstaller.installDefaultModules(server);
		}

		serverHelper = new ServerHelper(wizard);

		setServerPort(server);
		setDebugPort(server);

		setupDatabase(server, distroProperties);

		// If there's no distro at this point, we create a minimal one here,
		// *after* having initialized server.isH2Supported in `setupDatabase` above.
		if (distroProperties == null) {
			distroProperties = distroHelper.createDistroForPlatform(server);
		}
		distroProperties.saveTo(server.getServerDirectory());

		setJdk(server);

		server.setValuesFromDistroPropertiesModules(
				distroProperties.getWarArtifacts(),
				distroProperties.getModuleArtifacts(), distroProperties);
		server.setUnspecifiedToDefault();
		server.save();
	}

	private void setJdk(Server server) throws MojoExecutionException {
		String platformVersion = server.getPlatformVersion();
		Version version = new Version(platformVersion);
		if (platformVersion.startsWith("1.")) {
			wizard.showMessage("Note: JDK 1.7 is needed for platform version " + platformVersion + ".");
		} else if (version.getMajorVersion() == 2 && version.getMinorVersion() < 4) {
			wizard.showMessage("Note: JDK 1.8 is needed for platform version " + platformVersion + ".");
		} else {
			wizard.showMessage("Note: JDK 1.8 or above is needed for platform version " + platformVersion + ".");
		}

		wizard.promptForJavaHomeIfMissing(server);
	}

	private void installOWAs(Server server, DistroProperties distroProperties) throws MojoExecutionException {
		if (distroProperties != null) {
			File owasDir = new File(server.getServerDirectory(), "owa");
			owasDir.mkdir();
			downloadOWAs(server.getServerDirectory(), distroProperties, owasDir);
		}
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

	private void wipeDatabase(Server server) throws MojoExecutionException {
		String uri = getUriWithoutDb(server);
		try (DBConnector connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName())) {
			connector.dropDatabase();
			if (server.isMySqlDb() || server.isPostgreSqlDb()) {
				connector.checkAndCreate(server);
				wizard.showMessage("Connected to the database.");
			}
			wizard.showMessage("Database " + server.getDbName() + " has been wiped.");
		}
		catch (SQLException e) {
			throw new MojoExecutionException("Failed to drop " + server.getDbName() + " database");
		}
	}

	private void setServerPort(Server server) throws MojoExecutionException {
		String message = "What port would you like your server to use?";
		String port = wizard.promptForValueIfMissingWithDefault(
				message,
				server.getParam("tomcat.port"),
				"port number",
				String.valueOf(Setup.DEFAULT_PORT));
		if (!StringUtils.isNumeric(port) || !this.serverHelper.isPort(Integer.parseInt(port))) {
			wizard.showMessage("Port must be numeric and less or equal 65535.");
			this.setServerPort(server);
			return;
		}
		server.setPort(port);
	}

	private void setDebugPort(Server server) throws MojoExecutionException {
		if (StringUtils.isBlank(debug) || wizard.checkYes(debug)) {
			while (!NO_DEBUGGING_DEFAULT_ANSWER.equals(debug) && !StringUtils.isNumeric(debug)) {
				debug = wizard.promptForValueIfMissingWithDefault(
						ENABLE_DEBUGGING_DEFAULT_MESSAGE,
						server.getDebugPort(),
						"port number",
						NO_DEBUGGING_DEFAULT_ANSWER);
				if (!StringUtils.isNumeric(debug) && !NO_DEBUGGING_DEFAULT_ANSWER.equals(debug)) {
					wizard.showMessage("\nPort number must be numeric.");
				} else if (!NO_DEBUGGING_DEFAULT_ANSWER.equals(debug)) {
					server.setDebugPort(debug);
				}
			}
		}
	}

	private void setServerVersionsFromDistroProperties(Server server, DistroProperties distroProperties) {
		if (server.getPlatformVersion() == null) {
			server.setPlatformVersion(distroProperties.getPlatformVersion());
		}
		if (server.getVersion() == null) {
			server.setVersion(distroProperties.getVersion());
		}
	}

	private void setupDatabase(Server server, DistroProperties distroProperties) throws MojoExecutionException {
		if (server.getDbDriver() == null) {
			boolean isH2Supported = true;
			if (distroProperties != null) {
				isH2Supported = distroProperties.isH2Supported();
			}

			wizard.promptForDb(server, dockerHelper, isH2Supported, dbDriver, dockerHost);
		}

		if (server.getDbDriver() != null) {
			setupDatabaseForServer(server);
		}
	}

	private void setupDatabaseForServer(Server server) throws MojoExecutionException {
		if (server.getDbName() == null) {
			server.setDbName(determineDbName(server.getDbUri(), server.getServerId()));
		}

		if (server.isMySqlDb() || server.isPostgreSqlDb()) {
			String uri = getUriWithoutDb(server);
			try (DBConnector connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName())) {
				connector.checkAndCreate(server);
				wizard.showMessage("Connected to the database.");
			}
			catch (SQLException e) {
				throw new MojoExecutionException("Failed to connect to the specified database " + server.getDbUri(), e);
			}

			if (hasDbTables(server)) {
				if (dbReset == null) {
					dbReset = !wizard.promptYesNo(
							"Would you like to setup the server using existing data (if not, all data will be lost)?");
				}

				if (dbReset) {
					wipeDatabase(server);
				} else {
					server.setParam("create_tables", "false");
				}
			}

			if (dbReset == null) {
				dbReset = true;
			}

			if (!"null".equals(dbSql) && dbReset) {
				if (dbSql != null) {
					importDb(server, dbSql);
					resetSearchIndex(server);
				} else if (!server.isMySqlDb() && !server.isPostgreSqlDb()) {
					moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
					wizard.showMessage("The specified database " + server.getDbName()
							+ " does not exist and it will be created when OpenMRS starts.");
				}
			} else if (!dbReset) {
				resetSearchIndex(server);
			}
		} else {
			moduleInstaller.installModule(SDKConstants.H2_ARTIFACT, server.getServerDirectory().getPath());
			wizard.showMessage(
					"The specified database " + server.getDbName() + " does not exist and it will be created for you.");
		}
	}

	private boolean hasDbTables(Server server) throws MojoExecutionException {
		String uri = getUriWithoutDb(server);
		try (DBConnector connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName())) {
			DatabaseMetaData md = connector.getConnection().getMetaData();

			try (ResultSet rs = md.getTables(server.getDbName(), null, null, new String[] { "TABLE" })) {
				return rs.next();
			}
		}
		catch (SQLException e) {
			throw new MojoExecutionException("Failed to fetch table list from \"" + server.getDbName() + "\" database. " + e.getMessage(), e);
		}
	}

	private void resetSearchIndex(Server server) throws MojoExecutionException {
		String uri = server.getDbUri();

		try (DBConnector connector = new DBConnector(uri, server.getDbUser(), server.getDbPassword(), server.getDbName());
			PreparedStatement ps = connector.getConnection().prepareStatement(SDKConstants.RESET_SEARCH_INDEX_SQL)) {
			ps.execute();
			wizard.showMessage("The search index has been reset.");
		}
		catch (SQLException e) {
			throw new MojoExecutionException("Failed to reset search index " + e.getMessage(), e);
		}
	}

	private void importDb(Server server, String sqlScriptPath) throws MojoExecutionException {
		wizard.showMessage("Importing an initial database from " + sqlScriptPath + "...");
		String uri = server.getDbUri().replace("@DBNAME@", server.getDbName());

		InputStream sqlStream;
		if (sqlScriptPath.startsWith(Server.CLASSPATH_SCRIPT_PREFIX)) {
			String sqlScript = sqlScriptPath.replace(Server.CLASSPATH_SCRIPT_PREFIX, "");
			sqlStream = (Setup.class.getClassLoader().getResourceAsStream(sqlScript));
			if (sqlStream == null) {
				Artifact distroArtifact = new Artifact(server.getDistroArtifactId(), server.getVersion(),
						server.getDistroGroupId(), "jar");
				File extractedSqlFile = distroHelper
						.extractFileFromDistro(server.getServerDirectory(), distroArtifact, sqlScript);
				extractedSqlFile.deleteOnExit();
				try {
					sqlStream = new FileInputStream(extractedSqlFile);
				}
				catch (FileNotFoundException e) {
					throw new MojoExecutionException("Error during opening sql dump script file", e);
				}
			}
		} else {
			File scriptFile = new File(sqlScriptPath);
			try {
				sqlStream = new FileInputStream(scriptFile);
			}
			catch (FileNotFoundException e) {
				throw new MojoExecutionException("SQL import script could not be found at \"" +
						scriptFile.getAbsolutePath() + "\" " + e.getMessage() , e);
			}
		}

		try (InputStreamReader sqlReader = new InputStreamReader(sqlStream);
		     Connection connection = DriverManager.getConnection(uri, server.getDbUser(), server.getDbPassword())) {
			ScriptRunner scriptRunner = new ScriptRunner(connection);
			//we don't want to display ~5000 lines of queries to user if there is no error
			scriptRunner.setLogWriter(new PrintWriter(new NullOutputStream()));
			scriptRunner.setStopOnError(true);
			scriptRunner.runScript(sqlReader);

			wizard.showMessage("Database imported successfully.");
			server.setParam("create_tables", "false");
		}
		catch (Exception e) {
			getLog().error(e.getMessage());
			throw new MojoExecutionException("Failed to import database", e);
		}
	}

	public String determineDbName(String uri, String serverId) throws MojoExecutionException {
		String dbName = String.format(SDKConstants.DB_NAME_TEMPLATE, serverId);

		if (!uri.contains("@DBNAME@")) {
			//determine db name from uri
			try {
				URI parsedUri;
				if (uri.startsWith("jdbc:")) {
					parsedUri = new URI(uri.substring(5));
				} else {
					parsedUri = new URI(uri);
				}

				dbName = parsedUri.getPath();

				if (dbName == null || dbName.isEmpty() || dbName.equals("/")) {
					throw new MojoExecutionException("No database name is given in the URI: " + dbName);
				}

				dbName = dbName.substring(1);

				if (!dbName.substring(1).matches("^[A-Za-z0-9_\\-]+$")) {
					throw new MojoExecutionException(
							"The database name is not in the correct format (it should only have alphanumeric, dash and underscore signs): "
									+
									dbName);
				}
			}
			catch (URISyntaxException e) {
				throw new MojoExecutionException("Could not parse uri: " + uri, e);
			}
		}

		return dbName;
	}

	public void executeTask() throws MojoExecutionException, MojoFailureException {
		wizard.showMessage(SETTING_UP_A_NEW_SERVER);

		Server.ServerBuilder serverBuilder;
		if (file != null) {
			serverBuilder = new Server.ServerBuilder(Server.loadServer(Paths.get(file)));
		} else {
			serverBuilder = new Server.ServerBuilder();
		}

		Server server = serverBuilder
				.setServerId(serverId)
				.setDbDriver(dbDriver)
				.setDbUri(dbUri)
				.setDbUser(dbUser)
				.setDbPassword(dbPassword)
				.setInteractiveMode(testMode)
				.setJavaHome(javaHome)
				.setDebugPort(debug)
				.build();

		wizard.promptForNewServerIfMissing(server);

		File serverDir = Server.getServersPath().resolve(server.getServerId()).toFile();
		if (serverDir.isDirectory()) {
			throw new MojoExecutionException(
					"Cannot create server: directory with name " + serverDir.getName() + " already exists");
		}
		else if (serverDir.getAbsolutePath().contains(" ")) {
			throw new MojoExecutionException("Cannot create server: The server path " + serverDir.getAbsolutePath() +
					" contains a space. Please make sure your server path does not include any spaces.");
		}

		server.setServerDirectory(serverDir);
		serverDir.mkdir();

		try {
			DistroProperties distroProperties = resolveDistroProperties(server);
			setup(server, distroProperties);
		}
		catch (Exception e) {
			FileUtils.deleteQuietly(server.getServerDirectory());
			throw new MojoExecutionException("Failed to setup server", e);
		}

		getLog().info("Server configured successfully, path: " + serverDir);

		if (run) {
			new Run(this, server.getServerId()).execute();
		}
	}

	private String getUriWithoutDb(Server server) {
		String uri = server.getDbUri();
		uri = uri.substring(0, uri.lastIndexOf("/") + 1);
		return uri;
	}
}
