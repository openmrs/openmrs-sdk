package org.openmrs.maven.plugins.utility;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Server;

public class DefaultWizardRepromptTest {

	private DefaultWizard wizard;

	private Server server;

	@Before
	public void setUp() throws IOException, MojoExecutionException {
		wizard = spy(new DefaultWizard());
		doReturn("newuser").when(wizard).promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUser"), anyString());
		doReturn("newpass").when(wizard).promptForPasswordIfMissingWithDefault(anyString(), isNull(), eq("dbPassword"), anyString());

		Properties props = new Properties();
		props.put(Server.PROPERTY_SERVER_ID, "testserver");
		props.put(Server.PROPERTY_DB_URI, "jdbc:mysql://localhost:3306/@DBNAME@");
		props.put(Server.PROPERTY_DB_DRIVER, SDKConstants.DRIVER_MYSQL_OLD);
		server = new Server(null, props);
	}

	@Test
	public void promptForDbCredentialsAgain_shouldPassNullToForceReprompt() throws MojoExecutionException {
		wizard.promptForDbCredentialsAgain(server);

		verify(wizard).promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUser"), eq("root"));
		verify(wizard).promptForPasswordIfMissingWithDefault(anyString(), isNull(), eq("dbPassword"), eq(""));
	}

	@Test
	public void promptForDbCredentialsAgain_shouldUpdateServerCredentials() throws MojoExecutionException {
		wizard.promptForDbCredentialsAgain(server);

		org.junit.Assert.assertEquals("newuser", server.getDbUser());
		org.junit.Assert.assertEquals("newpass", server.getDbPassword());
	}

	@Test
	public void promptForNewUriAndCredentials_shouldPassNullToForceReprompt() throws MojoExecutionException {
		doReturn("jdbc:mysql://newhost:3306/@DBNAME@").when(wizard)
				.promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUri"), anyString());

		wizard.promptForNewUriAndCredentials(server);

		verify(wizard).promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUri"), eq(SDKConstants.URI_MYSQL));
		verify(wizard).promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUser"), eq("root"));
		verify(wizard).promptForPasswordIfMissingWithDefault(anyString(), isNull(), eq("dbPassword"), eq(""));
	}

	@Test
	public void promptForNewUriAndCredentials_shouldUpdateServerUriAndCredentials() throws MojoExecutionException {
		doReturn("jdbc:mysql://newhost:3306/@DBNAME@").when(wizard)
				.promptForValueIfMissingWithDefault(anyString(), isNull(), eq("dbUri"), anyString());

		wizard.promptForNewUriAndCredentials(server);

		org.junit.Assert.assertTrue(server.getDbUri().contains("newhost:3306"));
		org.junit.Assert.assertEquals("newuser", server.getDbUser());
		org.junit.Assert.assertEquals("newpass", server.getDbPassword());
	}
}
