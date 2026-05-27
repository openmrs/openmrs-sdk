package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuildDistroTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private List<String> generateDockerfile(String platformVersion, Properties dockerProps, boolean bundled) throws Exception {
        Properties props = new Properties();
        props.setProperty("war.openmrs", platformVersion);
        props.putAll(dockerProps);
        DistroProperties distroProperties = new DistroProperties(props);
        BuildDistro buildDistro = new BuildDistro();
        buildDistro.bundled = bundled;
        File targetDir = temporaryFolder.newFolder();
        buildDistro.copyDockerfile(targetDir, distroProperties);
        return FileUtils.readLines(new File(targetDir, "Dockerfile"), "UTF-8");
    }

    @Test
    public void copyDockerfile_platform25_shouldUseJre11DefaultTag() throws Exception {
        List<String> lines = generateDockerfile("2.5.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:nightly-amazoncorretto-11"));
    }

    @Test
    public void copyDockerfile_platformBelow25_shouldUseJre8DefaultTag() throws Exception {
        List<String> lines = generateDockerfile("2.4.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:nightly-amazoncorretto-8"));
    }

    @Test
    public void copyDockerfile_platform3x_shouldUseJre11DefaultTag() throws Exception {
        List<String> lines = generateDockerfile("3.0.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:nightly-amazoncorretto-11"));
    }

    @Test
    public void copyDockerfile_withDockerImageTag_shouldUseTagDirectly() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_TAG, "2.7.0-amazoncorretto-11");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.0-amazoncorretto-11"));
    }

    @Test
    public void copyDockerfile_withOpenmrsVersion_shouldUseVersionAsTag() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, "2.7.0");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.0"));
    }

    @Test
    public void copyDockerfile_withOpenmrsVersionAndJavaVersion_shouldAppendJavaSuffix() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, "2.7.0");
        props.setProperty(BuildDistro.DOCKER_IMAGE_JAVA_VERSION, "amazoncorretto-11");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.0-amazoncorretto-11"));
    }

    @Test
    public void copyDockerfile_withSnapshotVersion_shouldConvertToXSuffix() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, "2.7.0-SNAPSHOT");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.x"));
    }

    @Test
    public void copyDockerfile_withSnapshotVersionAndCustomNamespace_shouldNotConvertSnapshot() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_NAMESPACE, "myorg");
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, "2.7.0-SNAPSHOT");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM myorg/openmrs-core:2.7.0-SNAPSHOT"));
    }

    @Test
    public void copyDockerfile_withCustomNamespaceAndRepository_shouldUseCustomImage() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_NAMESPACE, "myorg");
        props.setProperty(BuildDistro.DOCKER_IMAGE_REPOSITORY, "myimage");
        props.setProperty(BuildDistro.DOCKER_IMAGE_TAG, "latest");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM myorg/myimage:latest"));
    }

    @Test
    public void copyDockerfile_notBundled_shouldIncludeAllCopyLines() throws Exception {
        List<String> lines = generateDockerfile("2.5.0", new Properties(), false);
        assertThat(lines, hasItem("COPY openmrs_modules /openmrs/distribution/openmrs_modules"));
        assertThat(lines, hasItem("COPY openmrs_owas /openmrs/distribution/openmrs_owas"));
        assertThat(lines, hasItem("COPY openmrs_config /openmrs/distribution/openmrs_config"));
        assertThat(lines, hasItem("COPY openmrs_spa /openmrs/distribution/openmrs_spa"));
    }

    @Test
    public void copyDockerfile_withPlatformMagicWord_shouldUseWarOpenmrsVersion() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION_PLATFORM);
        props.setProperty(BuildDistro.DOCKER_IMAGE_JAVA_VERSION, "amazoncorretto-11");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.0-amazoncorretto-11"));
    }

    @Test
    public void copyDockerfile_bundled_shouldOmitModuleCopyLines() throws Exception {
        List<String> lines = generateDockerfile("2.5.0", new Properties(), true);
        assertThat(lines, not(hasItem("COPY openmrs_modules /openmrs/distribution/openmrs_modules")));
        assertThat(lines, not(hasItem("COPY openmrs_owas /openmrs/distribution/openmrs_owas")));
        assertThat(lines, not(hasItem("COPY openmrs_config /openmrs/distribution/openmrs_config")));
        assertThat(lines, not(hasItem("COPY openmrs_spa /openmrs/distribution/openmrs_spa")));
    }
}
