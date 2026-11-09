package org.openmrs.maven.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Wizard;

@RunWith(MockitoJUnitRunner.Strict.class)
public class SetupAdminPasswordTest {

	@Mock
	private Wizard wizard;

	private Setup setup;

	@Before
	public void setUp() {
		setup = new Setup();
		setup.wizard = wizard;
	}

	private void setAdminPasswordField(String password) throws Exception {
		Field field = Setup.class.getDeclaredField("adminPassword");
		field.setAccessible(true);
		field.set(setup, password);
	}

	private void invokeSetAdminPassword(Server server) throws Exception {
		Method method = Setup.class.getDeclaredMethod("setAdminPassword", Server.class);
		method.setAccessible(true);
		try {
			method.invoke(setup, server);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof MojoExecutionException) {
				throw (MojoExecutionException) e.getCause();
			}
			throw e;
		}
	}

	@Test
	public void setAdminPassword_shouldSetPasswordWhenProvidedViaCli() throws Exception {
		setAdminPasswordField("SdkTestAdm1n!23");
		Server server = new Server.ServerBuilder().build();

		invokeSetAdminPassword(server);

		assertThat(server.getParam(Server.PROPERTY_ADMIN_USER_PASSWORD), is("SdkTestAdm1n!23"));
	}

	@Test
	public void setAdminPassword_shouldPromptAndSetPasswordWhenNotProvided() throws Exception {
		when(wizard.promptForPasswordIfMissing(isNull(), anyString())).thenReturn("SdkTestAdm1n!23");
		Server server = new Server.ServerBuilder().build();

		invokeSetAdminPassword(server);

		assertThat(server.getParam(Server.PROPERTY_ADMIN_USER_PASSWORD), is("SdkTestAdm1n!23"));
	}

	@Test
	public void setAdminPassword_shouldRejectWeakPasswordViaCliAndReprompt() throws Exception {
		setAdminPasswordField("admin123");
		when(wizard.promptForPasswordIfMissing(isNull(), anyString())).thenReturn("SdkTestAdm1n!23");
		Server server = new Server.ServerBuilder().build();

		invokeSetAdminPassword(server);

		verify(wizard).showMessage(startsWith("The admin password needs at least 8 characters"));
		assertThat(server.getParam(Server.PROPERTY_ADMIN_USER_PASSWORD), is("SdkTestAdm1n!23"));
	}

	@Test
	public void setAdminPassword_shouldRepromptOnWeakAnswerThenAcceptValid() throws Exception {
		when(wizard.promptForPasswordIfMissing(isNull(), anyString()))
				.thenReturn("weak")
				.thenReturn("SdkTestAdm1n!23");
		Server server = new Server.ServerBuilder().build();

		invokeSetAdminPassword(server);

		verify(wizard).showMessage(startsWith("The admin password needs at least 8 characters"));
		assertThat(server.getParam(Server.PROPERTY_ADMIN_USER_PASSWORD), is("SdkTestAdm1n!23"));
	}

	@Test
	public void setAdminPassword_shouldKeepPasswordAlreadyOnServerWithoutPrompting() throws Exception {
		Server server = new Server.ServerBuilder().build();
		server.setParam(Server.PROPERTY_ADMIN_USER_PASSWORD, "SdkTestAdm1n!23");

		invokeSetAdminPassword(server);

		assertThat(server.getParam(Server.PROPERTY_ADMIN_USER_PASSWORD), is("SdkTestAdm1n!23"));
	}
}
