package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

public class BuildIT extends AbstractSdkIT {

	private String serverId;

	void addTestResources() throws Exception {
		FileUtils.copyDirectory(getTestFile(TEST_DIRECTORY, "buildIT"), testDirectory);
	}

	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		serverId = setupTestServer();
		Server server = Server.loadServer(testDirectoryPath.resolve(serverId));
		File firstDir = new File(testDirectory, "module1");
		File secondDir = new File(testDirectory, "module2");
		server.addWatchedProject(Project.loadProject(firstDir));
		server.addWatchedProject(Project.loadProject(secondDir));
		server.save();
		clearParams();
	}

	@Test
	public void build_shouldBuildAllWatchedProjects() throws Exception {
		assertFileNotPresent(serverId, "module2-1.0-SNAPSHOT.omod");
		assertFileNotPresent(serverId, "module1-1.0-SNAPSHOT.omod");

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
