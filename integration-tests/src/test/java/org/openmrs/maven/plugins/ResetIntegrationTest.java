package org.openmrs.maven.plugins;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ResetIntegrationTest extends AbstractSdkIntegrationTest {

	@Test
	public void reset_shouldResetExistingServer() throws Exception {
		// create the server
		String serverId = setupTestServer();

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
