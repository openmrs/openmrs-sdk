package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.ContentProperties;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DistroHelperTest {

    @Mock
    VersionsHelper versionsHelper;

    @Before
    public void setupMocks() {
        when(versionsHelper.getLatestSnapshotVersion((Artifact) any())).thenReturn("RESOLVED-LATEST-SNAPSHOT");
        when(versionsHelper.getLatestReleasedVersion((Artifact) any())).thenReturn("RESOLVED-LATEST-RELEASE");
    }

    @Test
    public void parseDistroArtifactShouldInferArtifactIdForRefapp() throws Exception{
        String distro = "referenceapplication:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is(Artifact.GROUP_DISTRO));
        assertThat(artifact.getArtifactId(), is(SDKConstants.REFAPP_2X_ARTIFACT_ID));
    }

    @Test
    public void parseDistroArtifactShouldSetDefaultGroupIdIfNotSpecified() throws Exception{
        String distro = "otherdistro:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is(Artifact.GROUP_DISTRO));
    }

    @Test(expected = MojoExecutionException.class)
    public void parseDistroArtifactShouldReturnNullIfInvalidFormat() throws Exception{
        String distro = "referenceapplication:2.3:fsf:444";
        DistroHelper.parseDistroArtifact(distro, null);
    }

    @Test
    public void parseDistroArtifactShouldCreateProperArtifact() throws Exception{
        String distro = "org.openmrs.distromock:refapp:2.3";
        Artifact artifact = DistroHelper.parseDistroArtifact(distro, null);

        assertThat(artifact.getGroupId(), is("org.openmrs.distromock"));
        assertThat(artifact.getArtifactId(), is("refapp"));
        assertThat(artifact.getVersion(), is("2.3"));
    }

    @Test
    public void parseDistroArtifact_shouldNormalizeArtifactBeforeReturning() throws MojoExecutionException {
        Artifact artifact = DistroHelper.parseDistroArtifact("org.openmrs.module:htmlformentry:2.0.0", versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry-omod"));
    }

    @Test
    public void normalizeArtifact_shouldAppendOmodToModuleArtifactIds() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("htmlformentry", "2.0.0", "org.openmrs.module"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry-omod"));
    }

    @Test
    public void normalizeArtifact_shouldNotAppendOmodToNonModuleArtifactIds() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("htmlformentry", "2.0.0", "org.pih.module"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("htmlformentry"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp2() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "2.6.1", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("referenceapplication-package"));
        assertThat(artifact.getType(), equalTo("jar"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp3Beta() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "3.0.0-alpha", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("referenceapplication-distro"));
        assertThat(artifact.getType(), equalTo("zip"));
    }

    @Test
    public void normalizeArtifact_shouldNormalizeRefApp3() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("referenceapplication", "3.0.0", "org.openmrs.distro"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("distro-emr-configuration"));
        assertThat(artifact.getGroupId(), equalTo("org.openmrs"));
        assertThat(artifact.getType(), equalTo("zip"));
    }

    @Test
    public void normalizeArtifact_shouldResolveLatestSnapshotVersion() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("a", "LATEST", "b"), versionsHelper);
        assertThat(artifact.getVersion(), equalTo("RESOLVED-LATEST-RELEASE"));
    }

    @Test
    public void normalizeArtifact_shouldResolveLatestReleaseVersion() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("a", "LATEST-SNAPSHOT", "b"), versionsHelper);
        assertThat(artifact.getVersion(), equalTo("RESOLVED-LATEST-SNAPSHOT"));
    }

    @Test
    public void normalizeArtifact_shouldNotChangeByDefault() {
        Artifact artifact = DistroHelper.normalizeArtifact(new Artifact("my-artifact", "my-version", "my-group", "my-type"), versionsHelper);
        assertThat(artifact.getArtifactId(), equalTo("my-artifact"));
        assertThat(artifact.getGroupId(), equalTo("my-group"));
        assertThat(artifact.getVersion(), equalTo("my-version"));
        assertThat(artifact.getType(), equalTo("my-type"));
    }

    @Test
    public void versionSatisfiesRange_shouldReturnVersionsThatSatisfyTheGivenRange() {
        DistroHelper distroHelper = new DistroHelper(new MavenEnvironment());
        assertTrue(distroHelper.versionSatisfiesRange(">=5.0.0", "5.0.0"));
        assertTrue(distroHelper.versionSatisfiesRange("<=5.0.0", "5.0.0"));
        assertTrue(distroHelper.versionSatisfiesRange("5.0.0", "5.0.0"));
        assertFalse(distroHelper.versionSatisfiesRange("<5.0.0", "5.0.0"));
        assertFalse(distroHelper.versionSatisfiesRange(">=5.0.0", "5.0.0-SNAPSHOT"));
        assertFalse(distroHelper.versionSatisfiesRange(">=5.0.0", "5.0.0-pre.505"));
        assertTrue(distroHelper.versionSatisfiesRange(">=5.0.0", "5.0.1-SNAPSHOT"));
        assertTrue(distroHelper.versionSatisfiesRange(">=5.0.0", "5.0.1-pre.505"));
    }

    @Test
    public void getMissingDependencies_shouldGetMissingDependencies() throws Exception {
        Properties content = new Properties();
        content.put("war.openmrs", ">=2.5.0");
        content.put("omod.spa", ">=1.0.0");
        content.put("owa.orderentry", ">=2.0.0");
        content.put("config.refapp", ">=1.1.0");
        content.put("content.hiv", ">=1.0.1");
        content.setProperty("spa.frontendModules.@pih/esm-refapp-navbar-app", ">=1.2.3");
        content.setProperty("spa.frontendModules.@openmrs/esm-home-app", ">=5.5.2-pre.505");
        content.setProperty("spa.frontendModules.@openmrs/esm-login-app", "next");
        Properties distro = new Properties();
        distro.put("content.hiv", "1.0.0");

        MavenEnvironment mavenEnvironment = mock(MavenEnvironment.class);
        ContentHelper contentHelper = mock(ContentHelper.class);
        DistroHelper distroHelper = new DistroHelper(mavenEnvironment);
        when(contentHelper.getContentProperties(any())).thenReturn(new ContentProperties(content));
        distroHelper.setContentHelper(contentHelper);

        List<MissingDependency> m = distroHelper.getMissingDependencies(new DistroProperties(distro));
        assertThat(m.size(), equalTo(8));
        String component = "org.openmrs.content:hiv";
        assertMissingDependency(m, component, "war", "org.openmrs.web:openmrs-webapp", ">=2.5.0", null);
        assertMissingDependency(m, component, "module", "org.openmrs.module:spa-omod", ">=1.0.0", null);
        assertMissingDependency(m, component, "owa", "org.openmrs.owa:orderentry", ">=2.0.0", null);
        assertMissingDependency(m, component, "config", "org.openmrs.distro:refapp", ">=1.1.0", null);
        assertMissingDependency(m, component, "content", "org.openmrs.content:hiv", ">=1.0.1", "1.0.0");
        assertMissingDependency(m, component, "esm", "@pih/esm-refapp-navbar-app", ">=1.2.3", null);
        assertMissingDependency(m, component, "esm", "@openmrs/esm-home-app", ">=5.5.2-pre.505", null);
        assertMissingDependency(m, component, "esm", "@openmrs/esm-login-app", "next", null);
    }

    @Test
    public void getMissingDependencies_shouldGetDependenciesThatNeedUpgrade() throws Exception {
        Properties content = new Properties();
        content.put("war.openmrs", ">=2.5.0");
        content.put("omod.spa", ">=1.0.0");
        content.put("owa.orderentry", ">=2.0.1-SNAPSHOT");
        content.put("config.refapp", ">=1.2.0");
        content.put("content.hiv", ">=1.0.2");
        content.setProperty("spa.frontendModules.@pih/esm-refapp-navbar-app", "next");
        content.setProperty("spa.frontendModules.@openmrs/esm-home-app", ">=5.5.2-pre.506");
        content.setProperty("spa.frontendModules.@openmrs/esm-login-app", ">=5.5.5");
        Properties distro = new Properties();
        distro.put("war.openmrs", "2.4.0");
        distro.put("omod.spa", "1.0.0-SNAPSHOT");
        distro.put("owa.orderentry", "2.0.0");
        distro.put("config.refapp", "1.1.0");
        distro.put("content.hiv", "1.0.1");
        distro.setProperty("spa.frontendModules.@pih/esm-refapp-navbar-app", "1.2.3");
        distro.setProperty("spa.frontendModules.@openmrs/esm-home-app", "5.5.2-pre.505");
        distro.setProperty("spa.frontendModules.@openmrs/esm-login-app", "next");

        MavenEnvironment mavenEnvironment = mock(MavenEnvironment.class);
        ContentHelper contentHelper = mock(ContentHelper.class);
        DistroHelper distroHelper = new DistroHelper(mavenEnvironment);
        when(contentHelper.getContentProperties(any())).thenReturn(new ContentProperties(content));
        distroHelper.setContentHelper(contentHelper);

        List<MissingDependency> m = distroHelper.getMissingDependencies(new DistroProperties(distro));
        assertThat(m.size(), equalTo(7));
        String component = "org.openmrs.content:hiv";
        assertMissingDependency(m, component, "war", "org.openmrs.web:openmrs-webapp", ">=2.5.0", "2.4.0");
        assertMissingDependency(m, component, "module", "org.openmrs.module:spa-omod", ">=1.0.0", "1.0.0-SNAPSHOT");
        assertMissingDependency(m, component, "owa", "org.openmrs.owa:orderentry", ">=2.0.1-SNAPSHOT", "2.0.0");
        assertMissingDependency(m, component, "config", "org.openmrs.distro:refapp", ">=1.2.0", "1.1.0");
        assertMissingDependency(m, component, "content", "org.openmrs.content:hiv", ">=1.0.2", "1.0.1");
        assertMissingDependency(m, component, "esm", "@pih/esm-refapp-navbar-app", "next", "1.2.3");
        assertMissingDependency(m, component, "esm", "@openmrs/esm-home-app", ">=5.5.2-pre.506", "5.5.2-pre.505");
    }

    @Test
    public void getMissingDependencies_shouldNotShowMissingIfMatch() throws Exception {
        Properties content = new Properties();
        content.put("war.openmrs", ">=2.5.0");
        content.put("omod.spa", ">=1.0.0");
        content.put("owa.orderentry", ">=2.0.1-SNAPSHOT");
        content.put("config.refapp", ">=1.2.0");
        content.put("content.hiv", ">=1.0.2");
        content.setProperty("spa.frontendModules.@pih/esm-refapp-navbar-app", "next");
        content.setProperty("spa.frontendModules.@openmrs/esm-home-app", ">=5.5.2-pre.506");
        content.setProperty("spa.frontendModules.@openmrs/esm-login-app", ">=5.5.5");
        Properties distro = new Properties();
        distro.put("war.openmrs", "2.5.1");
        distro.put("omod.spa", "1.2.0-SNAPSHOT");
        distro.put("owa.orderentry", "2.0.2");
        distro.put("config.refapp", "2.1.0");
        distro.put("content.hiv", "1.0.2");
        distro.setProperty("spa.frontendModules.@pih/esm-refapp-navbar-app", "next");
        distro.setProperty("spa.frontendModules.@openmrs/esm-home-app", "5.5.2-pre.506");
        distro.setProperty("spa.frontendModules.@openmrs/esm-login-app", "5.5.6-pre.6110");

        MavenEnvironment mavenEnvironment = mock(MavenEnvironment.class);
        ContentHelper contentHelper = mock(ContentHelper.class);
        DistroHelper distroHelper = new DistroHelper(mavenEnvironment);
        when(contentHelper.getContentProperties(any())).thenReturn(new ContentProperties(content));
        distroHelper.setContentHelper(contentHelper);

        List<MissingDependency> m = distroHelper.getMissingDependencies(new DistroProperties(distro));
        assertThat(m.size(), equalTo(0));
    }

    void assertMissingDependency(List<MissingDependency> dependencies, String dependentComponent, String requiredType, String requiredComponent, String requiredVersion, String currentVersion) {
        int numFound = 0;
        for (MissingDependency dependency : dependencies) {
            if (nullSafeEquals(dependency.getDependentComponent(), dependentComponent)) {
                if (nullSafeEquals(dependency.getRequiredType(), requiredType)) {
                    if (nullSafeEquals(dependency.getRequiredComponent(), requiredComponent)) {
                        if (nullSafeEquals(dependency.getRequiredVersion(), requiredVersion)) {
                            if (nullSafeEquals(dependency.getCurrentVersion(), currentVersion)) {
                                numFound++;
                            }
                        }
                    }
                }
            }
        }
        assertThat(numFound, equalTo(1));
    }

    boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
}
