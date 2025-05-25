package org.openmrs.maven.plugins;

import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.maven.plugins.utility.Wizard;

public class CreateProjectTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private CreateProject createProject;
    private Wizard wizard;

    @Before
    public void setup() {
        createProject = new CreateProject();
        wizard = mock(Wizard.class);
        createProject.setWizard(wizard);
    }

    @Test
    public void compareVersions_shouldReturnNegativeWhenFirstVersionIsLower() {
        assertThat(createProject.compareVersions("2.3.0", "2.4.0"), is(lessThan(0)));
        assertThat(createProject.compareVersions("2.3.9", "2.4.0"), is(lessThan(0)));
        assertThat(createProject.compareVersions("1.9.9", "2.0.0"), is(lessThan(0)));
    }

    @Test
    public void compareVersions_shouldReturnZeroWhenVersionsAreEqual() {
        assertThat(createProject.compareVersions("2.4.0", "2.4.0"), is(0));
        assertThat(createProject.compareVersions("1.0.0", "1.0.0"), is(0));
    }

    @Test
    public void compareVersions_shouldReturnPositiveWhenFirstVersionIsHigher() {
        assertThat(createProject.compareVersions("2.4.0", "2.3.0"), is(greaterThan(0)));
        assertThat(createProject.compareVersions("2.5.0", "2.4.9"), is(greaterThan(0)));
        assertThat(createProject.compareVersions("3.0.0", "2.9.9"), is(greaterThan(0)));
    }

    @Test
    public void compareVersions_shouldHandleDifferentVersionLengths() {
        assertThat(createProject.compareVersions("2.4", "2.4.0"), is(0));
        assertThat(createProject.compareVersions("2.4.0", "2.4"), is(0));
        assertThat(createProject.compareVersions("2.4.0.0", "2.4.0"), is(0));
    }

    @Test
    public void choosePlatformVersion_shouldAcceptValidVersion() throws MojoExecutionException {
        when(wizard.promptForValueIfMissingWithDefault(anyString(), eq(null), anyString(), anyString()))
            .thenReturn("2.4.0");

        createProject.setProjectType("platform-module");
        createProject.choosePlatformVersion();

        verify(wizard, times(1)).promptForValueIfMissingWithDefault(anyString(), eq(null), anyString(), anyString());
    }

    @Test
    public void choosePlatformVersion_shouldRejectInvalidVersionAndRetry() throws MojoExecutionException {
        when(wizard.promptForValueIfMissingWithDefault(anyString(), eq(null), anyString(), anyString()))
            .thenReturn("2.3.0")
            .thenReturn("2.4.0");

        createProject.setProjectType("platform-module");
        createProject.choosePlatformVersion();

        verify(wizard, times(2)).promptForValueIfMissingWithDefault(anyString(), eq(null), anyString(), anyString());
        verify(wizard, times(1)).showMessage(contains("Platform version must be at least 2.4.0"));
    }
}
