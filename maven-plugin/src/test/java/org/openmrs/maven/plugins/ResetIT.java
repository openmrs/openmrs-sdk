package org.openmrs.maven.plugins;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class ResetIT extends AbstractSdkIT {

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
