package org.openmrs.maven.plugins;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.maven.plugins.model.Server;


public class SetupTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void determineDbName_shouldReturnDefaultNameIfVariableWasUsed() throws MojoExecutionException {
		Setup setupPlatform = new Setup();
		String dbName = setupPlatform.determineDbName("jdbc:mysql://localhost:3016/@DBNAME@?someParam=value", "my_server");
		assertThat(dbName, is(equalTo("openmrs-my_server")));
	}

	@Test
	public void determineDbName_shouldReturnDefaultNameForH2Uri() throws MojoExecutionException {
		Setup setupPlatform = new Setup();
		assertThat(setupPlatform.determineDbName("jdbc:h2:mem:test", "my_server"), equalTo("openmrs-my_server"));
		assertThat(setupPlatform.determineDbName("jdbc:h2:/tmp/data/db;DB_CLOSE_DELAY=-1", "my_server"), equalTo("openmrs-my_server"));
	}

	@Test
	public void determineDbName_shouldReturnNameFromUriWithParams() throws MojoExecutionException {
		Setup setupPlatform = new Setup();
		String dbName = setupPlatform.determineDbName("jdbc:mysql://localhost:3016/open?someParam=value", "my_server");
		assertThat(dbName, is(equalTo("open")));
	}

	@Test
	public void determineDbName_shouldReturnNameFromUriWithoutParams() throws MojoExecutionException {
		Setup setupPlatform = new Setup();

		String dbName = setupPlatform.determineDbName("jdbc:mysql://localhost:3016/open", "my_server");

		assertThat(dbName, is(equalTo("open")));
	}

	@Test
	public void determineDbName_shouldFailIfNoNameInUri() throws MojoExecutionException {
		Setup setupPlatform = new Setup();

		expectedException.expectMessage("No database name is given in the URI:");

		setupPlatform.determineDbName("jdbc:mysql://localhost:3016/", "my_server");
	}

	@Test
	public void determineDbName_shouldFailIfBadNameInUri() throws MojoExecutionException {
		Setup setupPlatform = new Setup();

		expectedException.expectMessage("The database name is not in the correct format (");

		setupPlatform.determineDbName("jdbc:mysql://localhost:3016/This%20Should%20Not%20Work", "my_server");
	}

	@Test
	public void normalizeDbDriver_shouldConvertH2AliasToDriverClass() {
		assertThat(Setup.normalizeDbDriver("h2"), equalTo("org.h2.Driver"));
		assertThat(Setup.normalizeDbDriver("H2"), equalTo("org.h2.Driver"));
	}

	@Test
	public void normalizeDbDriver_shouldLeaveOtherValuesUnchanged() {
		assertThat(Setup.normalizeDbDriver("org.h2.Driver"), equalTo("org.h2.Driver"));
		assertThat(Setup.normalizeDbDriver("org.postgresql.Driver"), equalTo("org.postgresql.Driver"));
		assertNull(Setup.normalizeDbDriver(null));
	}

	@Test
	public void needsDbPrompt_shouldBeTrueWithoutDriver() {
		assertTrue(Setup.needsDbPrompt(new Server.ServerBuilder().build()));
	}

	@Test
	public void needsDbPrompt_shouldBeTrueForH2DriverWithoutUri() {
		Server server = new Server.ServerBuilder().setDbDriver("org.h2.Driver").build();
		assertTrue(Setup.needsDbPrompt(server));
	}

	@Test
	public void needsDbPrompt_shouldBeFalseForH2DriverWithUri() {
		Server server = new Server.ServerBuilder().setDbDriver("org.h2.Driver").setDbUri("jdbc:h2:mem:test").build();
		assertFalse(Setup.needsDbPrompt(server));
	}

	@Test
	public void needsDbPrompt_shouldBeFalseForMySqlDriverWithoutUri() {
		Server server = new Server.ServerBuilder().setDbDriver("com.mysql.cj.jdbc.Driver").build();
		assertFalse(Setup.needsDbPrompt(server));
	}
}
