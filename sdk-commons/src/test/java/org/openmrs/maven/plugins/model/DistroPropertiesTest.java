package org.openmrs.maven.plugins.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.List;
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
        List<Artifact> contentArtifacts = distro.getContentArtifacts();
        assertThat(contentArtifacts, hasSize(2));
        Artifact hiv = findArtifactByArtifactId(contentArtifacts, "hiv");
        assertThat(hiv, notNullValue());
        assertThat(hiv, hasVersion("1.0.0"));
        assertThat(hiv.getGroupId(), equalTo(Artifact.GROUP_CONTENT));
        assertThat(hiv.getType(), equalTo(Artifact.TYPE_ZIP));
        Artifact tb = findArtifactByArtifactId(contentArtifacts, "tb");
        assertThat(tb, notNullValue());
        assertThat(tb, hasVersion("2.3.4-SNAPSHOT"));
        assertThat(tb.getGroupId(), equalTo(Artifact.GROUP_CONTENT));
        assertThat(tb.getType(), equalTo(Artifact.TYPE_ZIP));
    }

    @Test
    public void shouldGetContentPropertiesWithOverrides() {
        Properties properties = new Properties();
        properties.setProperty("content.hiv", "1.0.0");
        properties.setProperty("content.hiv.groupId", "org.openmrs.new");
        properties.setProperty("content.hiv.type", "gzip");
        DistroProperties distro = new DistroProperties(properties);
        List<Artifact> contentArtifacts = distro.getContentArtifacts();
        assertThat(contentArtifacts, hasSize(1));
        Artifact hiv = findArtifactByArtifactId(contentArtifacts, "hiv");
        assertThat(hiv, notNullValue());
        assertThat(hiv, hasVersion("1.0.0"));
        assertThat(hiv.getGroupId(), equalTo("org.openmrs.new"));
        assertThat(hiv.getType(), equalTo("gzip"));
    }

    @Test(expected = MojoExecutionException.class)
    public void resolvePlaceholders_shouldFailIfNoPropertyForPlaceholderFound() throws MojoExecutionException {
        Properties properties = new Properties();
        properties.setProperty("omod.fails", "${failsVersion}");
        DistroProperties distro = new DistroProperties(properties);
        distro.resolvePlaceholders(getProjectProperties());
    }

    private static Artifact findArtifactByArtifactId(List<Artifact> artifacts, String artifactId){
        for(Artifact artifact : artifacts){
            if(artifact.getArtifactId().equals(artifactId)){
                return artifact;
            }
        }
        return null;
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
