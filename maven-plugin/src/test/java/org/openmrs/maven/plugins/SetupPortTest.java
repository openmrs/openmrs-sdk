package org.openmrs.maven.plugins;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.ServerHelper;
import org.openmrs.maven.plugins.utility.Wizard;

@RunWith(MockitoJUnitRunner.Strict.class)
public class SetupPortTest {

	@Mock
	private Wizard wizard;

	@Mock
	private Server server;

	private Setup setup;

	private ServerHelper serverHelper;

	@Before
	public void setUp() throws Exception {
		setup = new Setup();
		setup.wizard = wizard;

		serverHelper = new ServerHelper(wizard);
		Field serverHelperField = Setup.class.getDeclaredField("serverHelper");
		serverHelperField.setAccessible(true);
		serverHelperField.set(setup, serverHelper);
	}

	private void setServerPort(String port) throws Exception {
		Field field = Setup.class.getDeclaredField("serverPort");
		field.setAccessible(true);
		field.set(setup, port);
	}

	private void invokeSetServerPort() throws Exception {
		Method method = Setup.class.getDeclaredMethod("setServerPort", Server.class);
		method.setAccessible(true);
		try {
			method.invoke(setup, server);
		} catch (java.lang.reflect.InvocationTargetException e) {
			if (e.getCause() instanceof MojoExecutionException) {
				throw (MojoExecutionException) e.getCause();
			}
			throw e;
		}
	}

	@Test
	public void setServerPort_shouldSetPortWhenProvidedViaCli() throws Exception {
		setServerPort("9090");

		invokeSetServerPort();

		verify(server).setPort("9090");
	}

	@Test
	public void setServerPort_shouldPromptAndSetPortWhenNotProvided() throws Exception {
		when(wizard.promptForValueIfMissingWithDefault(anyString(), any(), eq("serverPort"), eq("8080")))
				.thenReturn("8080");

		invokeSetServerPort();

		verify(server).setPort("8080");
	}

	@Test
	public void setServerPort_shouldRejectNonNumericPortViaCliAndReprompt() throws Exception {
		setServerPort("abc");
		when(wizard.promptForValueIfMissingWithDefault(anyString(), any(), eq("serverPort"), eq("8080")))
				.thenReturn("8080");

		invokeSetServerPort();

		verify(wizard).showMessage("Port must be numeric and less or equal 65535.");
		verify(server).setPort("8080");
	}

	@Test
	public void setServerPort_shouldRejectOutOfRangePortViaCliAndReprompt() throws Exception {
		setServerPort("99999");
		when(wizard.promptForValueIfMissingWithDefault(anyString(), any(), eq("serverPort"), eq("8080")))
				.thenReturn("8080");

		invokeSetServerPort();

		verify(wizard).showMessage("Port must be numeric and less or equal 65535.");
		verify(server).setPort("8080");
	}

	@Test
	public void setServerPort_shouldRepromptOnInvalidThenAcceptValid() throws Exception {
		when(wizard.promptForValueIfMissingWithDefault(anyString(), any(), eq("serverPort"), eq("8080")))
				.thenReturn("bad")
				.thenReturn("3000");

		invokeSetServerPort();

		verify(wizard).showMessage("Port must be numeric and less or equal 65535.");
		verify(server).setPort("3000");
	}
}
