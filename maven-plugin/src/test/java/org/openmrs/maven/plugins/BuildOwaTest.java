package org.openmrs.maven.plugins;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.NodeDistVersion;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildOwaTest {

    @Spy
    private Build build;

    @Spy
    private OwaHelper owaHelper;

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
        build.owaHelper = owaHelper;
    }

    @Test
    public void buildOwa_shouldReadNpmAndNodeVersionPropertyFromPackageJson() {
        final String packageJsonPath = "src/test/java/resources/package.json";
        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "7.0.0";

        String npmVersion = OwaHelper.getPackageJsonFromJsonFile(packageJsonPath).getEngines().get(OwaHelper.NPM_VERSION_KEY);
        String nodeVersion = OwaHelper.getPackageJsonFromJsonFile(packageJsonPath).getEngines().get(OwaHelper.NODE_VERSION_KEY);

        assertThat(npmVersion, is(projectNpmVersion));
        assertThat(nodeVersion, is(projectNodeVersion));
    }

    @Test
    public void buildOwa_shouldBuildOwaWithNpmAndNodeDefinedInBatchMode() throws Exception {
        final String batchNodeVersion = "7.3.0";
        final String batchNpmVersion = "3.10.10";

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System without npm and nodejs
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(null);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(null);

        // Project without npm and nodejs
        doReturn(null).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(null).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build.owaHelper).installLocalNodeAndNpm(batchNodeVersion, batchNpmVersion);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }
    @Test
    public void buildOwa_shouldUseSystemNpmAndNodeWhenBatchModeVersionsAreSameAsSystem() throws Exception {
        final String batchNodeVersion = "7.3.0";
        final String batchNpmVersion = "3.10.10";

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System npm and nodejs versions same as versions in batch mode
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(batchNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(batchNpmVersion);

        // Project without npm and nodejs
        doReturn(null).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(null).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).runSystemNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_shouldIgnoreVersionsWhenNodeOrNpmIsNotDefined() throws Exception {
        final String batchNpmVersion = "3.10.10";
        final String systemNodeVersion = "7.0.0";
        final String projectNpmVersion = "3.10.8";

        // Batch mode with only npmVersion defined

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = null;

        // System with only node version defined
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(null);

        // Project with only node version defined
        doReturn(null).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build.owaHelper).installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_order_batchModeIs1stPriority() throws Exception {
        final String batchNpmVersion = "3.10.10";
        final String batchNodeVersion = "7.3.0";

        final String systemNpmVersion = "3.10.9";
        final String systemNodeVersion = "7.2.0";

        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "7.1.0";

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System has both version specified
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNpmVersion);

        // Project has both version specified
        doReturn(projectNodeVersion).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use versions specified in batch mode
        verify(build.owaHelper).installLocalNodeAndNpm(batchNodeVersion, batchNpmVersion);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_order_ProjectVersionsAre2ndPriority() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = null;
        final String systemNodeVersion = null;

        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "7.1.0";

        // Batch args not defined
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System doesn't have npm and node installed
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(systemNpmVersion);

        // Project has both version specified
        doReturn(projectNodeVersion).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use versions specified in batch mode
        verify(build.owaHelper).installLocalNodeAndNpm(projectNodeVersion, projectNpmVersion);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_useSystemNodeAndNpmIfFitsProjectVersion() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = "3.10.9";
        final String systemNodeVersion = "7.2.0";

        final String projectNpmVersion = "^3.10.8";
        final String projectNodeVersion = "^7.1.0";

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System has both version specified
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(systemNpmVersion);

        // Project has both version specified
        doReturn(projectNodeVersion).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).runSystemNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        // Should use system instances of npm and nodejs
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_parseMostSatisfyingVersionFromSemVerExpression() throws Exception {
        final String batchNpmVersion = null;
        final String batchNodeVersion = null;

        final String systemNpmVersion = null;
        final String systemNodeVersion = null;

        final String projectNpmVersion = "3.*";
        final String projectNodeVersion = "7.*";

        final String parsedProjectNpmVersion = "3.10.10";
        final String parsedProjectNodeVersion = "7.3.0";

        // npm and nodejs defined in batch mode
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System has both version specified
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(systemNpmVersion);

        // Project has both version specified
        doReturn(projectNodeVersion).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        doReturn(getNodeDistVersionsViaLocalResources()).when(build.owaHelper).getNodeDistVersions();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(parsedProjectNodeVersion, parsedProjectNpmVersion);
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();
    }

    private List<NodeDistVersion> getNodeDistVersionsViaLocalResources() throws Exception {
        final String nodeDistVersionsPath = "src/test/java/resources/nodeDistVersions.json";
        Gson gson = new Gson();
        BufferedReader rd = new BufferedReader(new FileReader(nodeDistVersionsPath));
        NodeDistVersion[] result = gson.fromJson(rd, NodeDistVersion[].class);
        return new ArrayList<>(Arrays.asList(result));
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
        build.npmVersion = batchNpmVersion;
        build.nodeVersion = batchNodeVersion;

        // System doesn't have npm and node installed
        when(build.owaHelper.getSystemNodeVersion()).thenReturn(systemNodeVersion);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(systemNpmVersion);

        // Project has no npm and node versions defined
        doReturn(projectNodeVersion).when(build.owaHelper).getProjectNodeVersionFromPackageJson();
        doReturn(projectNpmVersion).when(build.owaHelper).getProjectNpmVersionFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(anyString(), anyString());
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(anyList());

        build.buildOwaProject();

        verify(build.owaHelper).installLocalNodeAndNpm(SDKConstants.NODE_VERSION, SDKConstants.NPM_VERSION);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

}
