package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openmrs.maven.plugins.model.Version;

import java.awt.Desktop;
import java.net.URI;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end tests for the {@code build-distro} goal, one test method per supported
 * OpenMRS platform version line (1.9.x through 2.8.x).
 *
 * <p>Each test:
 * <ol>
 *   <li>Reads {@code openmrs-distro-e2e-{version}.properties} from
 *       {@code src/test/resources/integration-test/}.  Each file pins the latest release
 *       of that version line and lists two cross-version components — addresshierarchy
 *       and the orderentry OWA — plus legacyui for 2.x so that all versions produce the
 *       same login page.</li>
 *   <li>Runs the SDK {@code build-distro} goal to download artifacts and generate Docker
 *       artefacts in {@code target/docker/}.</li>
 *   <li>Asserts that the expected Dockerfile base image and DB image appear in the
 *       generated files, verifying the three code paths in {@link BuildDistro}:
 *       {@code Dockerfile-jre7} (1.x), {@code Dockerfile-jre8} (2.0–2.4), and dynamic
 *       FROM line (2.5+).</li>
 *   <li>Uses Testcontainers to start the generated docker compose stack, then polls
 *       {@code /openmrs/} until the login-page footer contains the exact version string
 *       (e.g. {@code "1.9.9"} or {@code "2.6.16"}), confirming that the database was
 *       initialised, all Liquibase changesets ran, and the correct version is running.</li>
 * </ol>
 *
 * <p>Tests require a running Docker daemon and are excluded from the standard
 * {@code integration-tests} Maven profile.  Run all versions with:
 * <pre>
 *   mvn install -DskipTests
 *   mvn verify -Pdocker-e2e-tests -pl integration-tests
 * </pre>
 * Run a single version with:
 * <pre>
 *   mvn verify -Pdocker-e2e-tests -pl integration-tests -Dit.test="BuildDistroE2EIT#platform_2_6_x"
 * </pre>
 *
 * <p>To reproduce a scenario manually:
 * <ol>
 *   <li>Copy the matching {@code openmrs-distro-e2e-{version}.properties} to an empty
 *       directory as {@code openmrs-distro.properties}.</li>
 *   <li>{@code mvn openmrs-sdk:build-distro -Ddistro=openmrs-distro.properties -Ddir=docker}</li>
 *   <li>{@code docker compose -f docker/docker-compose.yml up --build}</li>
 *   <li>Browse to {@code http://localhost:8080/openmrs}.</li>
 * </ol>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BuildDistroE2EIT extends AbstractSdkIT {

	private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(20);
	private static final String   COMPOSE_OUT_DIR  = "docker";

	private DockerComposeHelper dockerHelper;

	// -----------------------------------------------------------------------
	// Class-level Docker guard — skips the entire suite when daemon is absent
	// -----------------------------------------------------------------------

	@BeforeClass
	public static void requireDockerDaemon() {
		DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
		try (DockerClient client = DockerClientBuilder.getInstance(config)
				.withDockerHttpClient(new ApacheDockerHttpClient.Builder()
						.dockerHost(config.getDockerHost())
						.sslConfig(config.getSSLConfig())
						.build())
				.build()) {
			client.infoCmd().exec();
		}
		catch (Exception e) {
			Assume.assumeTrue("Docker daemon is not available: " + e.getMessage(), false);
		}
	}

	// -----------------------------------------------------------------------
	// Tear-down: stop the stack before the parent cleans the test directory
	// -----------------------------------------------------------------------

	@After
	@Override
	public void teardown() {
		if (dockerHelper != null) {
			try {
				dockerHelper.close();
			}
			catch (Exception e) {
				log.warn("Docker cleanup failed", e);
			}
			dockerHelper = null;
		}
		super.teardown();
	}

	// -----------------------------------------------------------------------
	// One test method per version line
	// -----------------------------------------------------------------------

	// Core release versions confirmed from github.com/openmrs/openmrs-core tags.
	// See the matching openmrs-distro-e2e-{version}.properties file for the full
	// module list used by each test.

	// 1.x — Dockerfile-jre7 (FROM tomcat:7-jre7), DB: mysql:5.6
	@Test public void platform_1_09_x() throws Exception { buildAndRun("1.9.12");  }
	@Test public void platform_1_10_x() throws Exception { buildAndRun("1.10.6");  }
	@Test public void platform_1_11_x() throws Exception { buildAndRun("1.11.9");  }
	@Test public void platform_1_12_x() throws Exception { buildAndRun("1.12.1");  }

	// 2.0–2.4 — Dockerfile-jre8 (FROM tomcat:7-jdk8), DB: mysql:5.6
	@Test public void platform_2_0_x()  throws Exception { buildAndRun("2.0.8");   }
	@Test public void platform_2_1_x()  throws Exception { buildAndRun("2.1.7");   }
	@Test public void platform_2_2_x()  throws Exception { buildAndRun("2.2.1");   }
	@Test public void platform_2_3_x()  throws Exception { buildAndRun("2.3.6");   }
	@Test public void platform_2_4_x()  throws Exception { buildAndRun("2.4.7");   }

	// 2.5+ — dynamic FROM openmrs/openmrs-core:{major}.{minor}.x, DB: mariadb:10.11.7
	@Test public void platform_2_5_x()  throws Exception { buildAndRun("2.5.15");  }
	@Test public void platform_2_6_x()  throws Exception { buildAndRun("2.6.16");  }
	@Test public void platform_2_7_x()  throws Exception { buildAndRun("2.7.9");   }
	@Test public void platform_2_8_x()  throws Exception { buildAndRun("2.8.6");   }

	// -----------------------------------------------------------------------
	// Shared implementation
	// -----------------------------------------------------------------------

	private void buildAndRun(String warVersion) throws Exception {
		long testStart = System.currentTimeMillis();
		log.info("================================================================================");
		log.info("  START: OpenMRS {}", warVersion);
		log.info("================================================================================");
		// Step 1: run build-distro using the explicit properties file for this version.
		includeDistroPropertiesFile("openmrs-distro-e2e-" + warVersion + ".properties");
		addTaskParam("dir", COMPOSE_OUT_DIR);
		addTaskParam("ignorePeerDependencies", "false");
		executeTask("build-distro");
		assertSuccess();
		log.info("  build-distro completed in {}", elapsed(testStart));

		// Step 2: verify the generated Docker artefacts match the expected code path.
		assertFilePresent(COMPOSE_OUT_DIR, "docker-compose.yml");
		assertFilePresent(COMPOSE_OUT_DIR, "web", "Dockerfile");

		Version version = new Version(warVersion);
		if (version.getMajorVersion() >= 2) {
			assertFilePresent(COMPOSE_OUT_DIR, "web", "openmrs_core", "openmrs.war");
		} else {
			assertFilePresent(COMPOSE_OUT_DIR, "web", "openmrs.war");
		}

		if (isPlatform2point5AndAbove(version)) {
			assertFileContains("FROM openmrs/openmrs-core:", COMPOSE_OUT_DIR, "web", "Dockerfile");
			assertFileContains("mariadb:10.11.7", COMPOSE_OUT_DIR, "docker-compose.yml");
		} else {
			String expectedBase = version.getMajorVersion() == 1 ? "FROM tomcat:7-jre7" : "FROM tomcat:7-jdk8";
			assertFileContains(expectedBase, COMPOSE_OUT_DIR, "web", "Dockerfile");
			assertFileContains("mysql:5.6", COMPOSE_OUT_DIR, "docker-compose.yml");
			assertFilePresent(COMPOSE_OUT_DIR, "web", "startup.sh");
		}

		// Step 3: start the stack and wait for the login-page footer to confirm the
		// correct version is running.  DockerComposeHelper logs the full response body
		// when the check passes so the result is easy to inspect.
		long dockerStart = System.currentTimeMillis();
		dockerHelper = new DockerComposeHelper(
				getTestFile(COMPOSE_OUT_DIR),
				"openmrs-e2e-" + warVersion.replace(".", "-"),
				STARTUP_TIMEOUT,
				warVersion);
		dockerHelper.start();

		// If -Dbrowser is set, open the running instance in the default browser and pause
		// so you can navigate around before the container is torn down.
		// Usage: mvn verify -Pdocker-e2e-tests ... -Dit.test="...#platform_1_09_x" -Dbrowser
		if (System.getProperty("browser") != null) {
			String url = "http://localhost:" + dockerHelper.getPort() + "/openmrs/";
			log.info("================================================================================");
			log.info("  BROWSER MODE: OpenMRS {} is running at {}", warVersion, url);
			log.info("  Press ENTER in this terminal to stop the container and end the test...");
			log.info("================================================================================");
			try {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().browse(new URI(url));
				}
			}
			catch (Exception e) {
				log.warn("Could not open browser automatically: {}", e.getMessage());
			}
			// System.console() is reliably connected to the terminal when Maven forks a JVM;
			// System.in may have buffered content or be closed in some Maven configurations.
			try {
				java.io.Console console = System.console();
				if (console != null) {
					console.readLine();
				} else {
					new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
				}
			}
			catch (Exception ignored) {}
		}

		log.info("================================================================================");
		log.info("  PASS: OpenMRS {}  (docker startup: {}  total: {})",
				warVersion, elapsed(dockerStart), elapsed(testStart));
		log.info("================================================================================");
	}

	private static String elapsed(long startMillis) {
		long seconds = (System.currentTimeMillis() - startMillis) / 1000;
		return String.format("%dm %02ds", seconds / 60, seconds % 60);
	}

	// -----------------------------------------------------------------------
	// Helper
	// -----------------------------------------------------------------------

	private boolean isPlatform2point5AndAbove(Version v) {
		return v.getMajorVersion() > 2 || (v.getMajorVersion() == 2 && v.getMinorVersion() >= 5);
	}
}
