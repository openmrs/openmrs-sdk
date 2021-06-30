package org.openmrs.maven.plugins;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class BuildIntegrationTest extends AbstractSdkIntegrationTest {

	private String serverId;

	@Before
	public void setup() throws Exception {
		testDirectory = ResourceExtractor.simpleExtractResources(getClass(), TEST_DIRECTORY + File.separator + "buildIT");
		verifier = new Verifier(testDirectory.getAbsolutePath());

		testFilesToPersist = Arrays.asList(Objects.requireNonNull(testDirectory.listFiles()));

		addTaskParam("openMRSPath", testDirectory.getAbsolutePath());

		serverId = setupTestServer();

		Server server = Server.loadServer(new File(testDirectory, serverId));
		File firstDir = new File(testDirectory, "module1");
		File secondDir = new File(testDirectory, "module2");
		server.addWatchedProject(Project.loadProject(firstDir));
		server.addWatchedProject(Project.loadProject(secondDir));
		server.save();

		clearParams();
	}

	@After
	public void deleteServer() throws Exception {
		deleteTestServer(serverId);
	}

	@Test
	public void build_shouldBuildAllWatchedProjects() throws Exception {
		assertFileNotPresent(serverId + File.separator + "module2-1.0-SNAPSHOT.omod");
		assertFileNotPresent(serverId + File.separator + "module1-1.0-SNAPSHOT.omod");

		addTaskParam("openMRSPath", testDirectory.getAbsolutePath());
		addTaskParam("serverId", serverId);

		executeTask("build");

		assertSuccess();
		assertModulesInstalled(serverId, "module2-1.0-SNAPSHOT.omod");
		assertModulesInstalled(serverId, "module1-1.0-SNAPSHOT.omod");
	}

	@Test
	public void build_shouldBuildOwaProject() throws Exception {

		addTaskParam("openMRSPath", testDirectory.getAbsolutePath());

		addTaskParam("nodeVersion", SDKConstants.NODE_VERSION);
		addTaskParam("npmVersion", SDKConstants.NPM_VERSION);

		addAnswer("n"); // Maven Project found in this directory, do You want to build it?

		addTaskParam(BATCH_ANSWERS, getAnswers());

		executeTask("build");

		verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
		assertFilePresent("dist");
	}

	@Test
	public void build_shouldDetectMavenProject() throws Exception {

		addTaskParam("openMRSPath", testDirectory.getAbsolutePath());
		addTaskParam("buildOwa", "false");
		addAnswer("y"); // Maven Project found in this directory, do You want to build it?
		addTaskParam(BATCH_ANSWERS, getAnswers());

		executeTask("build");

		//just check logs, because purpose of this test is to determine if sdk detects projects, not if build is ok
		assertSuccess();
	}
}
