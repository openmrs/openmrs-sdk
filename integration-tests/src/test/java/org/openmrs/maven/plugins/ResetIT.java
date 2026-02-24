package org.openmrs.maven.plugins;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class ResetIT extends AbstractSdkIT {

	private static Path templateRefApp22;

	@BeforeClass
	public static void setupTemplate() throws Exception {
		Path testBaseDir = resolveTestBaseDir();
		File nodeCacheDir = testBaseDir.resolve("node-cache").toFile();
		Path testResourceDir = testBaseDir.resolve("test-resources");
		templateRefApp22 = getOrCreateTemplateServer("referenceapplication:2.2", testBaseDir, nodeCacheDir, testResourceDir);
	}

	@Test
	public void reset_shouldResetExistingServer() throws Exception {
		// create the server
		String serverId = copyServerFromTemplate(templateRefApp22);

		// now reset the server
		addTaskParam("serverId", serverId);

		addAnswer("8080");
		addAnswer("1044");

		executeTask("reset");

		// verify server is reset
		assertSuccess();
		assertServerInstalled(serverId);
		assertFilePresent(serverId, "openmrs-1.11.2.war");
		assertFilePresent(serverId, "modules");
		assertModulesInstalled(serverId, "uicommons-1.6.omod", "uiframework-3.3.1.omod");
	}

}
