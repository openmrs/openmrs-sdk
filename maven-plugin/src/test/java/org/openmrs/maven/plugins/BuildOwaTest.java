package org.openmrs.maven.plugins;

import com.google.gson.Gson;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.NodeDistro;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.utility.OwaHelper.SemVersion;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
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
    public void setUp() throws Exception {
        npmInstallArgs = new ArrayList<>();
        npmInstallArgs.add("install");
        npmInstallArgs.add("--no-optional");

        npmRunBuildArgs = new ArrayList<>();
        npmRunBuildArgs.add("run");
        npmRunBuildArgs.add("build");

        build.wizard = wizard;
        build.owaHelper = owaHelper;
        owaHelper.setWizard(wizard);
    }

    @Test
    public void buildOwa_shouldReadNpmAndNodeVersionPropertyFromPackageJson() {
        final String packageJsonPath = "/package.json";
        final String projectNpmVersion = "3.10.8";
        final String projectNodeVersion = "7.0.0";

        String npmVersion = owaHelper.getPackageJson(packageJsonPath).getEngines().get(OwaHelper.NPM_VERSION_KEY);
        String nodeVersion = owaHelper.getPackageJson(packageJsonPath).getEngines().get(OwaHelper.NODE_VERSION_KEY);

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

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(nullable(SemVersion.class), nullable(SemVersion.class), nullable(String.class));
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        verify(build.owaHelper).installLocalNodeAndNpm(SemVersion.valueOf(batchNodeVersion), SemVersion.valueOf(batchNpmVersion), null);
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

        Mockito.doNothing().when(build.owaHelper).runSystemNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test(expected = MojoExecutionException.class)
    public void buildOwa_shouldFailIfNpmIsNotDefinedButNodeIsDefined() throws Exception {
        build.nodeVersion = null;
        build.npmVersion = "3.10.10";

        build.buildNpmProject();
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

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(nullable(SemVersion.class), nullable(SemVersion.class), nullable(String.class));
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        // Should use versions specified in batch mode
        verify(build.owaHelper).installLocalNodeAndNpm(SemVersion.valueOf(batchNodeVersion), SemVersion.valueOf(batchNpmVersion), null);
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
        doReturn(SemVersion.valueOf(projectNodeVersion)).when(build.owaHelper).getProjectNodeFromPackageJson();
        doReturn(SemVersion.valueOf(projectNpmVersion)).when(build.owaHelper).getProjectNpmFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(nullable(SemVersion.class), nullable(SemVersion.class), nullable(String.class));
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        // Should use versions specified in batch mode
        verify(build.owaHelper).installLocalNodeAndNpm(SemVersion.valueOf(projectNodeVersion), SemVersion.valueOf(projectNpmVersion), null);
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
        doReturn(SemVersion.valueOf(projectNodeVersion)).when(build.owaHelper).getProjectNodeFromPackageJson();
        doReturn(SemVersion.valueOf(projectNpmVersion)).when(build.owaHelper).getProjectNpmFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).runSystemNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        // Should use system instances of npm and nodejs
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runSystemNpmCommandWithArgs(npmRunBuildArgs);
    }

    @Test
    public void buildOwa_parseMostSatisfyingVersionFromSemVerExpression() throws Exception {
        build.npmVersion = null;
        build.nodeVersion = null;

        when(build.owaHelper.getSystemNodeVersion()).thenReturn(null);
        when(build.owaHelper.getSystemNpmVersion()).thenReturn(null);

        doReturn(SemVersion.valueOf("7.*")).when(build.owaHelper).getProjectNodeFromPackageJson();
        doReturn(SemVersion.valueOf("3.*")).when(build.owaHelper).getProjectNpmFromPackageJson();

        doReturn(getNodeDistVersionsViaLocalResources()).when(build.owaHelper).getNodeDistros();

        Mockito.doNothing().when(build.owaHelper).runInstallLocalNodeAndNpm(nullable(String.class), nullable(String.class), nullable(String.class));
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        verify(build.owaHelper).runInstallLocalNodeAndNpm("7.4.0", "3.10.10", null);
    }

    private List<NodeDistro> getNodeDistVersionsViaLocalResources() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/nodeDistVersions.json")) {
            if (in != null) {
                Reader rd = new InputStreamReader(in);
                NodeDistro[] result = new Gson().fromJson(rd, NodeDistro[].class);
                return Arrays.asList(result);
            }
        }

        return Collections.emptyList();
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
        doReturn(SemVersion.valueOf(projectNodeVersion)).when(build.owaHelper).getProjectNodeFromPackageJson();
        doReturn(SemVersion.valueOf(projectNpmVersion)).when(build.owaHelper).getProjectNpmFromPackageJson();

        Mockito.doNothing().when(build.owaHelper).installLocalNodeAndNpm(nullable(SemVersion.class), nullable(SemVersion.class), nullable(String.class));
        Mockito.doNothing().when(build.owaHelper).runLocalNpmCommandWithArgs(ArgumentMatchers.<String>anyList());

        build.buildNpmProject();

        verify(build.owaHelper).installLocalNodeAndNpm(SemVersion.valueOf(SDKConstants.NODE_VERSION), SemVersion.valueOf(SDKConstants.NPM_VERSION), null);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmInstallArgs);
        verify(build.owaHelper).runLocalNpmCommandWithArgs(npmRunBuildArgs);
    }

}
