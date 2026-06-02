package org.openmrs.maven.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

/**
 * Manages a docker compose stack for E2E integration tests.
 *
 * <p>Before starting the stack, allocates a free host port and rewrites the generated
 * {@code docker-compose.yml} to use it to prevent port conflicts between sequential test runs.
 *
 * <p>{@link ComposeContainer} handles container lifecycle (Ryuk reaper guarantees cleanup).
 * The web container's stdout streams to a dedicated logger named {@code docker.{projectName}}.
 *
 * <p>{@link #start()} blocks until the login-page footer confirms the correct version is
 * running: {@code <span id="codeVersion">Version: {warVersion}}.  Persistent HTTP 500
 * responses (3 in a row) cause an immediate failure with a diagnostic body snippet rather
 * than hanging until the timeout.
 */
public class DockerComposeHelper implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(DockerComposeHelper.class);

	private static final String COMPOSE_FILE         = "docker-compose.yml";
	private static final String DEFAULT_PORT_BINDING = "\"8080:8080\"";

	private final ComposeContainer compose;
	private final int port;
	private final Duration startupTimeout;
	private final String warVersion;

	public DockerComposeHelper(File composeDir, String projectName, Duration startupTimeout, String warVersion) throws IOException {
		this.port           = findFreePort();
		this.startupTimeout = startupTimeout;
		this.warVersion     = warVersion;
		rewriteHostPort(composeDir, port);

		Logger containerLog = LoggerFactory.getLogger("docker." + projectName);
		compose = new ComposeContainer(projectName, new File(composeDir, COMPOSE_FILE))
				.withLogConsumer("web", new Slf4jLogConsumer(containerLog));
	}

	/**
	 * Starts the stack and waits for OpenMRS to initialize.
	 */
	public void start() throws InterruptedException {
		compose.start();
		waitForInitialization();
	}

	@Override
	public void close() {
		compose.stop();
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private void waitForInitialization() throws InterruptedException {
		String urlStr        = "http://localhost:" + port + "/openmrs/";
		String successMarker = "<span id=\"codeVersion\">Version: " + warVersion;
		Instant deadline     = Instant.now().plus(startupTimeout);
		int consecutiveServerErrors = 0;

		log.info("Waiting for OpenMRS {} to initialize at {} (timeout: {} min)",
				warVersion, urlStr, startupTimeout.toMinutes());

		while (Instant.now().isBefore(deadline)) {
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
				conn.setConnectTimeout(5_000);
				conn.setReadTimeout(15_000);
				conn.setInstanceFollowRedirects(true);
				int status = conn.getResponseCode();
				String body = readBody(conn);

				if (body.contains(successMarker)) {
					log.info("OpenMRS {} initialized (HTTP {}).\nFull response body:\n{}", warVersion, status, body);
					return;
				}

				if (status >= 500) {
					consecutiveServerErrors++;
					log.warn("HTTP {} from OpenMRS {} ({} consecutive). Response snippet:\n{}",
							status, warVersion, consecutiveServerErrors,
							body.substring(0, Math.min(1000, body.length())));
					if (consecutiveServerErrors >= 3) {
						throw new IllegalStateException(
								"OpenMRS " + warVersion + " is returning HTTP " + status
										+ " on every request — likely a deployment error. "
										+ "See WARN log above for the response body.");
					}
				} else {
					consecutiveServerErrors = 0;
					log.debug("Not yet initialized (HTTP {}). Response snippet: {}",
							status, body.substring(0, Math.min(300, body.length()))
									.replaceAll("\\s+", " ").trim());
				}
			}
			catch (IOException e) {
				consecutiveServerErrors = 0;
				log.debug("HTTP check failed (server not yet reachable): {}", e.getMessage());
			}
			Thread.sleep(15_000);
		}

		throw new IllegalStateException(
				"OpenMRS at " + urlStr + " did not reach an initialized state within "
						+ startupTimeout.toMinutes() + " minutes");
	}

	private static String readBody(HttpURLConnection conn) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream(),
				StandardCharsets.UTF_8))) {
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
