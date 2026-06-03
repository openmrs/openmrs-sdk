package org.openmrs.maven.plugins;

import org.openmrs.maven.plugins.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Manages a docker compose stack for E2E integration tests.
 *
 * <p>Before starting the stack, allocates a free host port and rewrites the generated
 * {@code docker-compose.yml} to use it to prevent port conflicts between sequential test runs.
 *
 * <p>{@link ComposeContainer} handles container lifecycle (Ryuk reaper guarantees cleanup).
 * The web container's stdout streams to a dedicated logger named {@code docker.{projectName}}.
 *
 * <p>{@link #start()} blocks until two conditions are both met:
 * <ol>
 *   <li>{@code GET /openmrs/ws/rest/v1/systeminformation} returns HTTP 200 with
 *       {@code Content-Type: application/json} — confirming OpenMRS is initialised,
 *       webservices.rest has started, and authentication works.  The startup wizard
 *       returns text/html for the same URL while the database is being set up, so the
 *       content-type is the reliable readiness signal.</li>
 *   <li>{@code GET /openmrs/ws/rest/v1/module/addresshierarchy?v=full} returns
 *       {@code "started":true} — confirming the module loaded and started, not merely
 *       that its file is present on disk.</li>
 * </ol>
 *
 * <p>Persistent HTTP 5xx responses (3 in a row) or the string
 * {@code "Unable to start OpenMRS"} in the container log both cause an immediate failure.
 */
public class DockerComposeHelper implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(DockerComposeHelper.class);

	private static final String COMPOSE_FILE          = "docker-compose.yml";
	private static final String COMPOSE_OVERRIDE_FILE = "docker-compose.override.yml";
	private static final String DEFAULT_PORT_BINDING  = "\"8080:8080\"";

	private final ComposeContainer compose;
	private final int port;
	private final Duration startupTimeout;
	private final String warVersion;

	// Set by the log consumer when OpenMRS logs a fatal startup failure.
	private volatile String fatalStartupError = null;

	public DockerComposeHelper(File composeDir, String projectName, Duration startupTimeout, String warVersion) throws IOException {
		this.port           = findFreePort();
		this.startupTimeout = startupTimeout;
		this.warVersion     = warVersion;
		rewriteHostPort(composeDir, port);

		Logger containerLog = LoggerFactory.getLogger("docker." + projectName);

		// If -DdockerDebug is set, also load docker-compose.override.yml which mounts
		// web/log4j.properties into the container for DEBUG-level Spring/OpenMRS logging.
		File overrideFile = new File(composeDir, COMPOSE_OVERRIDE_FILE);
		boolean useOverride = System.getProperty("dockerDebug") != null && overrideFile.exists();
		if (useOverride) {
			log.info("dockerDebug enabled — loading {} for debug logging", COMPOSE_OVERRIDE_FILE);
		}

		compose = new ComposeContainer(projectName,
				useOverride
						? java.util.Arrays.asList(new File(composeDir, COMPOSE_FILE), overrideFile)
						: java.util.Arrays.asList(new File(composeDir, COMPOSE_FILE)))
				.withLogConsumer("web", frame -> {
					String line = frame.getUtf8String().trim();
					new Slf4jLogConsumer(containerLog).accept(frame);
					if (line.contains("Unable to start OpenMRS")) {
						fatalStartupError = line;
					} else if (line.contains("Restarting Tomcat with runtime properties present")) {
						// Two-step startup: the first Tomcat run was stopped, which interrupts
						// its background startOpenmrs() thread and produces a benign
						// "Unable to start OpenMRS" message as Hibernate sessions are torn down.
						// Clear it here so the second startup is judged on its own merits.
						fatalStartupError = null;
					}
				});
	}

	/**
	 * Starts the stack, waits for the systeminformation REST endpoint to confirm OpenMRS
	 * is fully initialised, then verifies addresshierarchy started successfully.
	 */
	public void start() throws InterruptedException {
		compose.start();

		// The default admin password is determined by the Liquibase initial data.
		// OpenMRS 1.x–2.3.x initial data sets admin to "test";
		// OpenMRS 2.4.x+ initial data sets admin to "Admin123".
		String password = adminPassword(warVersion);
		String auth = "Basic " + Base64.getEncoder()
				.encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8));

		String sysInfo = waitForSystemInformation(auth);
		assertModuleStarted("addresshierarchy", auth, sysInfo);
	}

	/** Returns the host port the OpenMRS web service is mapped to. */
	public int getPort() {
		return port;
	}

	@Override
	public void close() {
		compose.stop();
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	/**
	 * Polls {@code GET /openmrs/ws/rest/v1/systeminformation} until it returns HTTP 200
	 * with {@code Content-Type: application/json}.
	 *
	 * <p>During database initialisation the OpenMRS StartupFilter intercepts all requests
	 * and returns HTTP 200 with text/html (the wizard page) — so the content-type is the
	 * correct readiness signal, not just the status code.
	 *
	 * <p>Fails immediately on 3 consecutive 5xx responses or a fatal startup error in logs.
	 */
	private String waitForSystemInformation(String auth) throws InterruptedException {
		String url = "http://localhost:" + port + "/openmrs/ws/rest/v1/systeminformation";
		Instant deadline = Instant.now().plus(startupTimeout);
		int consecutiveServerErrors = 0;

		log.info("Waiting for systeminformation at {} (timeout: {} min)", url, startupTimeout.toMinutes());

		while (Instant.now().isBefore(deadline)) {
			if (fatalStartupError != null) {
				throw new IllegalStateException(
						"OpenMRS " + warVersion + " failed to start: " + fatalStartupError);
			}
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setConnectTimeout(5_000);
				conn.setReadTimeout(10_000);
				conn.setRequestProperty("Authorization", auth);
				int status = conn.getResponseCode();
				String body = readBody(conn, status);
				String contentType = conn.getHeaderField("Content-Type");
				log.debug("Poll: HTTP {} | Content-Type: {}", status, contentType);

				if (status == 200 && contentType != null && contentType.contains("application/json")) {
					log.info("================================================================================");
					log.info("  System information for OpenMRS {} (REST endpoint ready)", warVersion);
					log.info("================================================================================");
					log.info("{}", body);
					return body;
				}

				if (status >= 500) {
					consecutiveServerErrors++;
					log.warn("HTTP {} from systeminformation ({} consecutive):\n{}",
							status, consecutiveServerErrors,
							body.substring(0, Math.min(500, body.length())));
					if (consecutiveServerErrors >= 3) {
						throw new IllegalStateException(
								"systeminformation returning HTTP " + status + " consistently — "
										+ "likely a deployment error. See WARN log above.");
					}
				} else {
					consecutiveServerErrors = 0;
					log.debug("Not ready yet (HTTP {}).", status);
				}
			}
			catch (IOException e) {
				consecutiveServerErrors = 0;
				log.debug("Not yet reachable: {}", e.getMessage());
			}
			Thread.sleep(15_000);
		}

		throw new IllegalStateException(
				"systeminformation endpoint did not become available for OpenMRS " + warVersion
						+ " within " + startupTimeout.toMinutes() + " minutes.");
	}

	/**
	 * Asserts that the given module started successfully via
	 * {@code /openmrs/ws/rest/v1/module/{moduleId}?v=full}.
	 * A module can be present on disk but fail to start (e.g. wrong bytecode for the JVM);
	 * this check distinguishes started from merely present using the {@code started} field.
	 */
	private void assertModuleStarted(String moduleId, String auth, String sysInfo) {
		String url = "http://localhost:" + port + "/openmrs/ws/rest/v1/module/" + moduleId + "?v=full";
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(5_000);
			conn.setReadTimeout(10_000);
			conn.setRequestProperty("Authorization", auth);
			int status = conn.getResponseCode();
			String body = readBody(conn, status);

			if (status != 200) {
				throw new AssertionError(
						"Module '" + moduleId + "' not found via REST in OpenMRS " + warVersion
								+ " — HTTP " + status + ". The module may not have loaded at all.\n"
								+ "Response: " + body + "\nSystem info:\n" + sysInfo);
			}

			if (!body.contains("\"started\":true") && !body.contains("\"started\": true")) {
				throw new AssertionError(
						"Module '" + moduleId + "' is present but did not start in OpenMRS "
								+ warVersion + ". Check startupErrorMessage:\n" + body);
			}

			log.info("Module '{}' confirmed started (HTTP 200, started:true).", moduleId);
		}
		catch (IOException e) {
			throw new AssertionError(
					"Could not reach module endpoint to verify '" + moduleId + "': " + e.getMessage(), e);
		}
	}

	private static String readBody(HttpURLConnection conn, int status) throws IOException {
		InputStream stream = status < 400 ? conn.getInputStream() : conn.getErrorStream();
		if (stream == null) {
			return "";
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
			return sb.toString();
		}
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		}
	}

	/**
	 * Returns the default admin password for a fresh OpenMRS install of the given version.
	 * The password is set by the Liquibase initial data and changed at OpenMRS 2.4.x.
	 */
	private static String adminPassword(String warVersion) {
		Version v = new Version(warVersion);
		boolean pre24 = v.getMajorVersion() == 1
				|| (v.getMajorVersion() == 2 && v.getMinorVersion() < 4);
		return pre24 ? "test" : "Admin123";
	}

	private static void rewriteHostPort(File composeDir, int port) throws IOException {
		File composeFile = new File(composeDir, COMPOSE_FILE);
		String content = new String(Files.readAllBytes(composeFile.toPath()), StandardCharsets.UTF_8);
		Files.write(composeFile.toPath(),
				content.replace(DEFAULT_PORT_BINDING, "\"" + port + ":8080\"")
						.getBytes(StandardCharsets.UTF_8));
	}
}
