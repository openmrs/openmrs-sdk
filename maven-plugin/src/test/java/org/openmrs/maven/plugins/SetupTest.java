package org.openmrs.maven.plugins;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


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
}
