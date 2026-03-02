package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.maven.plugins.model.Version;

import java.io.File;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyString;

public class PlatformJdkValidatorTest {

	@Mock
	private Wizard wizard;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File propertiesFile;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		propertiesFile = tempFolder.newFile("openmrs-server.properties");
	}

	// Platform 1.x

	@Test
	public void platform1x_shouldAcceptJdk7() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("1.11.5"), "1.7.0_80", wizard, propertiesFile);
	}

	@Test
	public void platform1x_shouldWarnOnJdk8ButNotFail() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("1.11.5"), "1.8.0_292", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform1x_shouldRejectJdk11() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("1.11.5"), "11.0.11", wizard, propertiesFile);
	}

	// Platform 2.0-2.3

	@Test
	public void platform2_0_shouldAcceptJdk8() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.3.0"), "1.8.0_292", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform2_0_shouldRejectJdk11() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.3.0"), "11.0.11", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform2_0_shouldRejectJdk7() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.3.0"), "1.7.0_80", wizard, propertiesFile);
	}

	// Platform 2.4-2.6

	@Test
	public void platform2_4_shouldAcceptJdk8() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.4.0"), "1.8.0_292", wizard, propertiesFile);
	}

	@Test
	public void platform2_5_shouldAcceptJdk11() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.5.0"), "11.0.11", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform2_6_shouldRejectJdk17() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.6.0"), "17.0.1", wizard, propertiesFile);
	}

	// Platform 2.7

	@Test
	public void platform2_7_shouldAcceptJdk17() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.7.0"), "17.0.1", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform2_7_shouldRejectJdk21() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.7.0"), "21.0.1", wizard, propertiesFile);
	}

	// Platform 2.8

	@Test
	public void platform2_8_shouldAcceptJdk21() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.8.0"), "21.0.1", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform2_8_shouldRejectJdk24() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("2.8.0"), "24.0.1", wizard, propertiesFile);
	}

	// Platform 3.x

	@Test
	public void platform3_shouldAcceptJdk21() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("3.0.0"), "21.0.1", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void platform3_shouldRejectJdk17() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("3.0.0"), "17.0.1", wizard, propertiesFile);
	}

	@Test(expected = MojoExecutionException.class)
	public void invalidPlatformVersion_shouldThrow() throws MojoExecutionException {
		PlatformJdkValidator.validate(new Version("4.0.0"), "21.0.1", wizard, propertiesFile);
	}
}
