package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.maven.plugins.Build.NODE_VERSION_KEY;
import static org.openmrs.maven.plugins.Build.NPM_VERSION_KEY;

@RunWith(MockitoJUnitRunner.class)
public class BuildOwaTest {

    @Spy
    private Build build;

    @Mock
    private Wizard wizard;

    private List<String> npmInstallArgs;
    private List<String> npmRunBuildArgs;

    @Before
    public void setUp() {
        npmInstallArgs = new ArrayList<>();
        npmInstallArgs.add("install");

        npmRunBuildArgs = new ArrayList<>();
        npmRunBuildArgs.add("run");
        npmRunBuildArgs.add("build");

        build.wizard = wizard;
    }

    @Test
    public void buildOwa_shouldReadNpmAndNodeVersionPropertyFromPackageJson() {
        final String packageJsonPath = "src/test/java/resources/package.json";
        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "7.0.0";

        String npmVersion = Build.getPropertyValueFromPropertiesJsonFile(packageJsonPath, NPM_VERSION_KEY);
        String nodeVersion = Build.getPropertyValueFromPropertiesJsonFile(packageJsonPath, NODE_VERSION_KEY);

        assertThat(npmVersion, is(projectNpmVersion));
        assertThat(nodeVersion, is(projectNodeVersion));
    }

    @Test
    public void buildOwa_shouldBuildOwaWithNpmAndNodeDefinedInBatchMode() throws Exception {
        final String batchNodeVersion = "v7.3.0";
        final String batchNpmVersion = "3.10.10";

        // npm and nodejs defined in batch mode
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System without npm and nodejs
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, null);
        systemVersions.put(NPM_VERSION_KEY, null);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build).installLocalNodeAndNpm(batchNodeVersion, batchNpmVersion);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_shouldUseSystemNpmAndNodeWhenBatchModeVersionsAreSameAsSystem() throws Exception {
        final String batchNodeVersion = "v7.3.0";
        final String batchNpmVersion = "3.10.10";

        // npm and nodejs defined in batch mode
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System npm and nodejs versions same as versions in batch mode
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        Mockito.doNothing().when(build).runSystemNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_shouldIgnoreVersionsWhenNodeOrNpmIsNotDefined() throws Exception {
        final String batchNpmVersion = "3.10.10";
        final String systemNodeVersion = "v7.0.0";
        final String projectNpmVersion = "3.10.8";

        // Batch mode with only npmVersion defined
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, null);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System with only node version defined
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, systemNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, null);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        // Project with only node version defined
        Map<String, String> projectVersions = new HashMap<>();
        projectVersions.put(NODE_VERSION_KEY, null);
        projectVersions.put(NPM_VERSION_KEY, projectNpmVersion);
        doReturn(projectVersions).when(build).getProjectNpmAndNodeVersionFromPackageJson();

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build).installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_shouldBuildOwaWithoutNodeVersionPrefixConvention() throws Exception {
        // No "v" prefix before node version
        final String batchNodeVersion = "7.3.0";
        final String batchNpmVersion = "3.10.10";

        // npm and nodejs defined in batch mode
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System without npm and nodejs
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, null);
        systemVersions.put(NPM_VERSION_KEY, null);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build).installLocalNodeAndNpm(batchNodeVersion, batchNpmVersion);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_order_batchModeIs1stPriority() throws Exception {
        final String batchNpmVersion = "3.10.10";
        final String batchNodeVersion = "v7.3.0";

        final String systemNpmVersion = "3.10.9";
        final String systemNodeVersion = "v7.2.0";

        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "v7.1.0";

        // Batch args has both version specified
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System has both version specified
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, systemNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, systemNpmVersion);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        // Project has both version specified
        Map<String, String> projectVersions = new HashMap<>();
        projectVersions.put(NODE_VERSION_KEY, projectNodeVersion);
        projectVersions.put(NPM_VERSION_KEY, projectNpmVersion);
        doReturn(projectVersions).when(build).getProjectNpmAndNodeVersionFromPackageJson();

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use versions specified in batch mode
        verify(build).installLocalNodeAndNpm(batchNodeVersion, batchNpmVersion);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_order_SystemIs2ndPriority() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = "3.10.9";
        final String systemNodeVersion = "v7.2.0";

        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "v7.1.0";

        // Batch args not defined
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System has both version specified
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, systemNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, systemNpmVersion);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        // Project has both version specified
        Map<String, String> projectVersions = new HashMap<>();
        projectVersions.put(NODE_VERSION_KEY, projectNodeVersion);
        projectVersions.put(NPM_VERSION_KEY, projectNpmVersion);
        doReturn(projectVersions).when(build).getProjectNpmAndNodeVersionFromPackageJson();

        Mockito.doNothing().when(build).runSystemNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use system instances of npm and nodejs
        verify(build).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_order_ProjectVersionsAre3rdPriority() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = null;
        final String systemNodeVersion = null;

        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "v7.1.0";

        // Batch args not defined
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System doesn't have npm and node installed
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, systemNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, systemNpmVersion);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        // Project has both version specified
        Map<String, String> projectVersions = new HashMap<>();
        projectVersions.put(NODE_VERSION_KEY, projectNodeVersion);
        projectVersions.put(NPM_VERSION_KEY, projectNpmVersion);
        doReturn(projectVersions).when(build).getProjectNpmAndNodeVersionFromPackageJson();

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use versions specified in batch mode
        verify(build).installLocalNodeAndNpm(projectNodeVersion, projectNpmVersion);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_shouldUseDefaultNpmAndNodeWhenVersionsAreNotDefined() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = null;
        final String systemNodeVersion = null;

        final String projectNpmVersion = null;
        final String projectNodeVersion = null;

        // Batch args not defined
        Map<String, String> batchVersions = new HashMap<>();
        batchVersions.put(NODE_VERSION_KEY, batchNodeVersion);
        batchVersions.put(NPM_VERSION_KEY, batchNpmVersion);
        when(build.getBatchNodeAndNpmVersion()).thenReturn(batchVersions);

        // System doesn't have npm and node installed
        Map<String, String> systemVersions = new HashMap<>();
        systemVersions.put(NODE_VERSION_KEY, systemNodeVersion);
        systemVersions.put(NPM_VERSION_KEY, systemNpmVersion);
        when(build.getSystemNodeAndNpmVersion()).thenReturn(systemVersions);

        // Project has no npm and node versions defined
        Map<String, String> projectVersions = new HashMap<>();
        projectVersions.put(NODE_VERSION_KEY, projectNodeVersion);
        projectVersions.put(NPM_VERSION_KEY, projectNpmVersion);
        doReturn(projectVersions).when(build).getProjectNpmAndNodeVersionFromPackageJson();

        Mockito.doNothing().when(build).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build).installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
        verify(build).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

}
