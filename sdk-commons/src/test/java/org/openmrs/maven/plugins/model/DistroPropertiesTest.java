package org.openmrs.maven.plugins.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DistroPropertiesTest {

    public DistroProperties getDistro(){
        Properties properties = new Properties();
        properties.setProperty("omod.idgen", "2.3");
        properties.setProperty("name", "TEST");
        properties.setProperty("omod.legacyui", "2.3-SNAP");
        properties.setProperty("omod.metadatasharing", "${metadatasharingVersion}");
        properties.setProperty("omod.metadatamapping", "${metadatamappingVersion}");
        properties.setProperty("omod.appui", "${appuiVersion}");
        properties.setProperty("version", "${project.parent.version}");
        properties.setProperty("war.openmrs", "${project.version}");
        return new DistroProperties(properties);
    }

    public Properties getProjectProperties(){
        Properties properties = new Properties();
        properties.setProperty("metadatasharingVersion","1234");
        properties.setProperty("metadatamappingVersion","1.5.6");
        properties.setProperty("appuiVersion","1.6.7");
        properties.setProperty("legacyuiVersion","3.4.5");
        properties.setProperty("owaVersion","1.7.8");
        properties.setProperty("project.version", "222");
        properties.setProperty("project.parent.version", "1");
        return properties;
    }

    @Test
    public void resolvePlaceholders_shouldReplaceVersions() throws MojoExecutionException {
        DistroProperties distro = getDistro();
        distro.resolvePlaceholders(getProjectProperties());
        List<Artifact> distroArtifacts = distro.getModuleArtifacts();
        assertThat(distroArtifacts, hasSize(5));
        assertThat(findArtifactByArtifactId(distroArtifacts, "appui-omod"), hasVersion("1.6.7"));
        assertThat(distro.getVersion(), is("1"));
        assertThat(distro.getPlatformVersion(), is("222"));
    }

    @Test
    public void shouldGetContentPropertiesWithDefaults() {
        Properties properties = new Properties();
        properties.setProperty("content.hiv", "1.0.0");
        properties.setProperty("content.tb", "2.3.4-SNAPSHOT");
        DistroProperties distro = new DistroProperties(properties);
        List<ContentPackage> contentPackages = distro.getContentPackages();
        assertThat(contentPackages, hasSize(2));
        ContentPackage hivPackage = findContentPackageByArtifactId(contentPackages, "hiv");
        assertThat(hivPackage, notNullValue());
        assertThat(hivPackage.getArtifactId(), equalTo("hiv"));
        assertThat(hivPackage.getVersion(), equalTo("1.0.0"));
        assertThat(hivPackage.getGroupId(), equalTo(Artifact.GROUP_CONTENT));
        assertThat(hivPackage.getType(), equalTo(Artifact.TYPE_ZIP));
        assertThat(hivPackage.getNamespace(), equalTo("hiv"));
        ContentPackage tbPackage = findContentPackageByArtifactId(contentPackages, "tb");
        assertThat(tbPackage, notNullValue());
        assertThat(tbPackage.getArtifactId(), equalTo("tb"));
        assertThat(tbPackage.getVersion(), equalTo("2.3.4-SNAPSHOT"));
        assertThat(tbPackage.getGroupId(), equalTo(Artifact.GROUP_CONTENT));
        assertThat(tbPackage.getType(), equalTo(Artifact.TYPE_ZIP));
        assertThat(tbPackage.getNamespace(), equalTo("tb"));
    }

    @Test
    public void shouldGetContentPropertiesWithOverrides() {
        Properties properties = new Properties();
        properties.setProperty("content.hiv", "1.0.0");
        properties.setProperty("content.hiv.groupId", "org.openmrs.new");
        properties.setProperty("content.hiv.type", "gzip");
        properties.setProperty("content.hiv.namespace", "");
        DistroProperties distro = new DistroProperties(properties);
        List<ContentPackage> contentPackages = distro.getContentPackages();
        assertThat(contentPackages, hasSize(1));
        ContentPackage hivPackage = contentPackages.get(0);
        assertThat(hivPackage, notNullValue());
        assertThat(hivPackage.getArtifactId(), equalTo("hiv"));
        assertThat(hivPackage.getVersion(), equalTo("1.0.0"));
        assertThat(hivPackage.getGroupId(), equalTo("org.openmrs.new"));
        assertThat(hivPackage.getType(), equalTo("gzip"));
        assertThat(hivPackage.getNamespace(), equalTo(""));
    }

    @Test(expected = MojoExecutionException.class)
    public void resolvePlaceholders_shouldFailIfNoPropertyForPlaceholderFound() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("omod.fails", "${failsVersion}");
        DistroProperties distro = new DistroProperties(properties);
        distro.resolvePlaceholders(getProjectProperties());
    }

    @Test
    public void getParentDistroArtifact_shouldGetFromDistroWithImplicitArtifact() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("distro.pihemr", "2.1.0");
        properties.setProperty("distro.pihemr.groupId", "org.pih.openmrs");
        properties.setProperty("distro.pihemr.type", "zip");
        DistroProperties distro = new DistroProperties(properties);
        Artifact distroArtifact = distro.getParentDistroArtifact();
        assertThat(distroArtifact, notNullValue());
        assertThat(distroArtifact.getArtifactId(), equalTo("pihemr"));
        assertThat(distroArtifact.getVersion(), equalTo("2.1.0"));
        assertThat(distroArtifact.getGroupId(), equalTo("org.pih.openmrs"));
        assertThat(distroArtifact.getType(), equalTo("zip"));
    }

    @Test
    public void getParentDistroArtifact_shouldGetFromDistroWithExplicitArtifact() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("distro.foo", "2.1.0");
        properties.setProperty("distro.foo.artifactId", "pihemr");
        properties.setProperty("distro.foo.groupId", "org.pih.openmrs");
        properties.setProperty("distro.foo.type", "zip");
        DistroProperties distro = new DistroProperties(properties);
        Artifact distroArtifact = distro.getParentDistroArtifact();
        assertThat(distroArtifact, notNullValue());
        assertThat(distroArtifact.getArtifactId(), equalTo("pihemr"));
        assertThat(distroArtifact.getVersion(), equalTo("2.1.0"));
        assertThat(distroArtifact.getGroupId(), equalTo("org.pih.openmrs"));
        assertThat(distroArtifact.getType(), equalTo("zip"));
    }

    @Test
    public void getParentDistroArtifact_shouldGetFromParent() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("parent.version", "2.1.0");
        properties.setProperty("parent.artifactId", "pihemr");
        properties.setProperty("parent.groupId", "org.pih.openmrs");
        properties.setProperty("parent.type", "jar");
        DistroProperties distro = new DistroProperties(properties);
        Artifact distroArtifact = distro.getParentDistroArtifact();
        assertThat(distroArtifact, notNullValue());
        assertThat(distroArtifact.getArtifactId(), equalTo("pihemr"));
        assertThat(distroArtifact.getVersion(), equalTo("2.1.0"));
        assertThat(distroArtifact.getGroupId(), equalTo("org.pih.openmrs"));
        assertThat(distroArtifact.getType(), equalTo("jar"));
    }

    @Test
    public void getParentDistroArtifact_shouldGetFromParentAndDefaultToZipType() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("parent.version", "2.1.0");
        properties.setProperty("parent.artifactId", "pihemr");
        properties.setProperty("parent.groupId", "org.pih.openmrs");
        DistroProperties distro = new DistroProperties(properties);
        Artifact distroArtifact = distro.getParentDistroArtifact();
        assertThat(distroArtifact, notNullValue());
        assertThat(distroArtifact.getArtifactId(), equalTo("pihemr"));
        assertThat(distroArtifact.getVersion(), equalTo("2.1.0"));
        assertThat(distroArtifact.getGroupId(), equalTo("org.pih.openmrs"));
        assertThat(distroArtifact.getType(), equalTo("zip"));
    }

    @Test
    public void getParentDistroArtifact_shouldNormalizeArtifact() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("parent.version", "2.1.0");
        properties.setProperty("parent.artifactId", "referenceapplication");
        properties.setProperty("parent.groupId", "org.openmrs.distro");
        DistroProperties distro = new DistroProperties(properties);
        Artifact distroArtifact = distro.getParentDistroArtifact();
        assertThat(distroArtifact, notNullValue());
        assertThat(distroArtifact.getArtifactId(), equalTo("referenceapplication-package"));
        assertThat(distroArtifact.getVersion(), equalTo("2.1.0"));
        assertThat(distroArtifact.getGroupId(), equalTo("org.openmrs.distro"));
        assertThat(distroArtifact.getType(), equalTo("jar"));
    }

    @Test
    public void getPropertiesWithPrefixRemoved_shouldGetProperties() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("content.hiv.var1", "val1");
        properties.setProperty("content.hiv.var2", "val2");
        properties.setProperty("content.tb", "val3");
        DistroProperties distro = new DistroProperties(properties);
        Map<String, String> m = distro.getPropertiesWithPrefixRemoved("content.hiv.");
        assertThat(m, notNullValue());
        assertThat(m.size(), equalTo(2));
        assertThat(m.get("var1"), equalTo("val1"));
        assertThat(m.get("var2"), equalTo("val2"));
    }

    private static Artifact findArtifactByArtifactId(List<Artifact> artifacts, String artifactId){
        for(Artifact artifact : artifacts){
            if(artifact.getArtifactId().equals(artifactId)){
                return artifact;
            }
        }
        return null;
    }

    private static ContentPackage findContentPackageByArtifactId(List<ContentPackage> contentPackages, String artifactId) {
        for (ContentPackage contentPackage : contentPackages) {
            if (contentPackage.getArtifact().getArtifactId().equals(artifactId)){
                return contentPackage;
            }
        }
        throw new RuntimeException("No package found with artifactId " + artifactId);
    }

    public static Matcher<Artifact> hasVersion(final String version) {
        return new FeatureMatcher<Artifact, String>(is(version), "version", "artifact version") {
            @Override
            protected String featureValueOf(Artifact actual) {
                return actual.getVersion();
            }
        };
    }
}
