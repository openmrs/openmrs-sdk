package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class ServerUpgraderTest {
	ServerUpgrader serverUpgrader;

	@Before
	public void setUp(){
		serverUpgrader = new ServerUpgrader(new AbstractTask() {
			@Override
			public void execute() throws MojoExecutionException, MojoFailureException {

			}
		});
	}

	@Test
	public void testParseUserModules() throws Exception{
		Server server = new Server.ServerBuilder().build();
		String testUserModules = "org.openmrs.module/owa/1.4-SNAPSHOT," +
				"org.openmrs.module/uicommons/1.7,org.openmrs.module/webservices.rest/2.15-SNAPSHOT";
		Artifact owaModule = new Artifact("owa", "1.4-SNAPSHOT", "org.openmrs.module");

		server.setParam(Server.PROPERTY_USER_MODULES, testUserModules);
		List<Artifact> artifacts = serverUpgrader.parseUserModules(server);
		assertThat(artifacts.size(), is(3));
		checkIfAnyArtifactMatches(artifacts, owaModule);
	}

	@Test(expected = MojoExecutionException.class)
	public void testParseUserModulesShouldThrowParseExc() throws Exception{
		Server server = new Server.ServerBuilder().build();
		String brokenUserModules = "org.openmrs.module/owa/1.4-SNAPSHOT," +
				"org.openmrs.module/uico7,org.openmrs.module/webservices.rest/2.15-SNAPSHOT";
		server.setParam(Server.PROPERTY_USER_MODULES, brokenUserModules);
		List<Artifact> artifacts = serverUpgrader.parseUserModules(server);
	}

	private void checkIfAnyArtifactMatches(List<Artifact> artifacts, Artifact searched) {
		boolean foundMatch = false;
		for(Artifact artifact : artifacts){
			boolean eqArtifactId = artifact.getArtifactId().equals(searched.getArtifactId());
			boolean eqGroupId = artifact.getGroupId().equals(searched.getGroupId());
			boolean eqVersion = artifact.getVersion().equals(searched.getVersion());
			foundMatch = eqArtifactId&&eqGroupId&&eqVersion;
			if(foundMatch) {
				break;
			}
		}
		assertThat(foundMatch, is(true));
	}
}
