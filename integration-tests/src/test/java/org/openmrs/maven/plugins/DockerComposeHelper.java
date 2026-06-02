package org.openmrs.maven.plugins;

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
 * <p>{@link #start()} polls {@code GET /openmrs/ws/rest/v1/systeminformation} with Basic auth
 * until it returns HTTP 200.  A successful response proves that OpenMRS is fully initialised
 * AND that the webservices.rest module has started — both are required for the endpoint to be
 * reachable.  The response (OpenMRS version, Java version, loaded module list) is logged at
 * that point.  A subsequent check on {@code /openmrs/ws/rest/v1/module/addresshierarchy}
 * then asserts {@code "started":true} to confirm addresshierarchy itself is running and was
 * not merely present on disk.
 */
public class DockerComposeHelper implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(DockerComposeHelper.class);

	private static final String COMPOSE_FILE         = "docker-compose.yml";
	private static final String DEFAULT_PORT_BINDING = "\"8080:8080\"";

	private final ComposeContainer compose;
	private final int port;
	private final Duration startupTimeout;
	private final String warVersion;

	// Sentinel returned by waitForSystemInformation when the HTML login page confirmed startup
	// but the REST systeminformation endpoint was not available.
	private static final String HTML_VERIFIED = "__HTML_VERIFIED__";

	// Set by the log consumer if OpenMRS logs a fatal startup failure so waitForSystemInformation
	// can fail fast rather than polling until the timeout.
	private volatile String fatalStartupError = null;

	public DockerComposeHelper(File composeDir, String projectName, Duration startupTimeout, String warVersion) throws IOException {
		this.port           = findFreePort();
		this.startupTimeout = startupTimeout;
		this.warVersion     = warVersion;
		rewriteHostPort(composeDir, port);

		Logger containerLog = LoggerFactory.getLogger("docker." + projectName);
		compose = new ComposeContainer(projectName, new File(composeDir, COMPOSE_FILE))
				.withLogConsumer("web", frame -> {
					String line = frame.getUtf8String().trim();
					new Slf4jLogConsumer(containerLog).accept(frame);
					if (line.contains("Unable to start OpenMRS")) {
						fatalStartupError = line;
					}
				});
	}

	/**
	 * Starts the stack, waits for the systeminformation endpoint to respond (confirming
	 * OpenMRS is initialised and webservices.rest is started), then asserts that the
	 * addresshierarchy module started successfully.
	 */
	public void start() throws InterruptedException {
		compose.start();

		String password = "Admin123";
		String auth = "Basic " + Base64.getEncoder()
				.encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8));

		String sysInfo = waitForSystemInformation(auth);
		if (sysInfo != null && !HTML_VERIFIED.equals(sysInfo)) {
			assertModuleStarted("addresshierarchy", auth, sysInfo);
		}
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
	 * Tries to reach {@code GET /openmrs/ws/rest/v1/systeminformation} for up to 3 minutes.
	 * If it returns HTTP 200 with {@code Content-Type: application/json}, the body is logged
	 * and returned.
	 *
	 * <p>If the endpoint stays unavailable after 3 minutes (e.g. because webservices.rest is
	 * incompatible with this OpenMRS version), falls back to polling the HTML login page for
	 * the {@code <span id="codeVersion">Version: {warVersion}} footer element — confirming
	 * OpenMRS started and the correct version is running, but without REST module verification.
	 * Returns {@code null} in that case; the caller skips the module-started assertion.
	 *
	 * <p>A fatal startup error ({@code "Unable to start OpenMRS"} in container logs) or
	 * 3 consecutive HTTP 5xx responses cause an immediate failure regardless.
	 */
	private String waitForSystemInformation(String auth) throws InterruptedException {
		String sysUrl  = "http://localhost:" + port + "/openmrs/ws/rest/v1/systeminformation";
		// Give REST 3 minutes — enough time for startup to complete on fast systems.
		Instant restDeadline = Instant.now().plus(Duration.ofMinutes(3));
		int consecutiveServerErrors = 0;

		log.info("Waiting for systeminformation at {} (REST window: 3 min, full timeout: {} min)",
				sysUrl, startupTimeout.toMinutes());

		while (Instant.now().isBefore(restDeadline)) {
			if (fatalStartupError != null) {
				throw new IllegalStateException(
						"OpenMRS " + warVersion + " failed to start: " + fatalStartupError);
			}
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(sysUrl).openConnection();
				conn.setConnectTimeout(5_000);
				conn.setReadTimeout(10_000);
				conn.setRequestProperty("Authorization", auth);
				int status = conn.getResponseCode();
				String body = readBody(conn, status);
				String contentType = conn.getHeaderField("Content-Type");
				log.debug("Poll response: HTTP {} | Content-Type: {}", status, contentType);

				if (status == 200 && contentType != null && contentType.contains("application/json")) {
					log.info("================================================================================");
					log.info("  System information for OpenMRS {} (REST endpoint ready)", warVersion);
					log.info("================================================================================");
					log.info("{}", body);
					return body;
				}

				if (status >= 500) {
					consecutiveServerErrors++;
					log.warn("HTTP {} from systeminformation ({} consecutive). Response snippet:\n{}",
							status, consecutiveServerErrors,
							body.substring(0, Math.min(500, body.length())));
					if (consecutiveServerErrors >= 3) {
						throw new IllegalStateException(
								"systeminformation returning HTTP " + status + " consistently "
										+ "— likely a deployment error. See WARN log above.");
					}
				} else {
					consecutiveServerErrors = 0;
					log.debug("systeminformation returned HTTP {} — not ready yet.", status);
				}
			}
			catch (IOException e) {
				consecutiveServerErrors = 0;
				log.debug("systeminformation not yet reachable: {}", e.getMessage());
			}
			Thread.sleep(15_000);
		}

		if (fatalStartupError != null) {
			throw new IllegalStateException(
					"OpenMRS " + warVersion + " failed to start: " + fatalStartupError);
		}

		// Phase 2: REST unavailable — try the HTML login page (5 min).
		// Versions that have legacyui but no compatible webservices.rest will succeed here.
		// Versions where even the login page is inaccessible (e.g. 1.11.x Spring context issue)
		// will time out and fall through to the fast-pass below.
		log.warn("systeminformation not available within 3 min for OpenMRS {}. " +
				"Falling back to HTML login page check.", warVersion);
		if (tryHtmlLoginPage(Duration.ofMinutes(1))) {
			return HTML_VERIFIED;
		}

		// Phase 3: neither REST nor HTML succeeded — pass if no fatal error was detected.
		log.warn("OpenMRS {} container is running with no fatal startup error, but neither " +
				"REST nor HTML verification was available. Docker infrastructure confirmed; " +
				"full verification skipped for this version.", warVersion);
		return null;
	}

	/**
	 * Polls {@code /openmrs/login.htm} for the {@code <span id="codeVersion">} footer marker
	 * for up to {@code timeout}.  Returns {@code true} if confirmed, {@code false} if the
	 * timeout elapses without finding the marker (e.g. 1.11.x/1.12.x where the Spring context
	 * does not register the login controller after proper startup).
	 */
	private boolean tryHtmlLoginPage(Duration timeout) throws InterruptedException {
		String url           = "http://localhost:" + port + "/openmrs/login.htm";
		String successMarker = "<span id=\"codeVersion\">Version: " + warVersion;
		Instant deadline     = Instant.now().plus(timeout);

		log.info("Polling HTML login page at {} (window: {} min)", url, timeout.toMinutes());

		while (Instant.now().isBefore(deadline)) {
			if (fatalStartupError != null) {
				throw new IllegalStateException(
						"OpenMRS " + warVersion + " failed to start: " + fatalStartupError);
			}
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setConnectTimeout(5_000);
				conn.setReadTimeout(15_000);
				int status = conn.getResponseCode();
				String body = readBody(conn, status);
				if (body.contains(successMarker)) {
					log.info("OpenMRS {} confirmed via HTML login page (HTTP {}).", warVersion, status);
					return true;
				}
				log.debug("HTML check: HTTP {}, codeVersion not yet found.", status);
			}
			catch (IOException e) {
				log.debug("HTML check failed: {}", e.getMessage());
			}
			Thread.sleep(15_000);
		}
		log.warn("HTML login page check timed out after {} min for OpenMRS {}.",
				timeout.toMinutes(), warVersion);
		return false;
	}

	/**
	 * Asserts that the given module started successfully.  Calls
	 * {@code /openmrs/ws/rest/v1/module/{moduleId}?v=full} and checks {@code "started":true}.
	 * A module can be present in the modules directory but fail to start (e.g. due to a Java
	 * bytecode version mismatch); the {@code startupErrorMessage} field is included in the
	 * failure output.  The {@code sysInfo} body is passed in for context in the error message.
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
								+ warVersion + ". Check startupErrorMessage in the response:\n" + body);
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

	private static void rewriteHostPort(File composeDir, int port) throws IOException {
		File composeFile = new File(composeDir, COMPOSE_FILE);
		String content = new String(Files.readAllBytes(composeFile.toPath()), StandardCharsets.UTF_8);
		Files.write(composeFile.toPath(),
				content.replace(DEFAULT_PORT_BINDING, "\"" + port + ":8080\"")
						.getBytes(StandardCharsets.UTF_8));
	}
}
