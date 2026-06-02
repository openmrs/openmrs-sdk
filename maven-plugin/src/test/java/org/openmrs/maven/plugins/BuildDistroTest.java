package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuildDistroTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Tags that are treated as existing in Docker Hub for the purposes of unit tests,
     * reflecting actual availability as of the time these tests were written.
     * Tests that need specific behaviour for tags not in this set should set
     * {@code docker.image.openmrsVersion} or {@code docker.image.tag} explicitly in their
     * distro properties, which bypasses the Docker Hub lookup entirely.
     */
    private static final Set<String> SIMULATED_EXISTING_TAGS = new HashSet<>(Arrays.asList(
            "2.4.x",         // used by the custom-namespace-below-2.5 test
            "2.5.x-nightly", // 2.5.x has no plain rolling tag; only nightly is published
            "2.6.x-nightly", // 2.6.x has no plain rolling tag; only nightly is published
            "2.7.x",
            "2.8.x",
            "2.9.x",
            "3.0.x"
    ));

    private List<String> generateDockerfile(String platformVersion, Properties dockerProps, boolean bundled) throws Exception {
        Properties props = new Properties();
        props.setProperty("war.openmrs", platformVersion);
        props.putAll(dockerProps);
        DistroProperties distroProperties = new DistroProperties(props);

        // Override both check methods so tests run offline and deterministically.
        // dockerImageExistsLocally always returns false — no Docker daemon during unit tests.
        // dockerImageExistsOnHub uses the simulated set that mirrors actual Docker Hub availability.
        BuildDistro buildDistro = new BuildDistro() {
            @Override
            protected boolean dockerImageExistsLocally(String namespace, String repository, String tag) {
                return false;
            }
            @Override
            protected boolean dockerImageExistsOnHub(String namespace, String repository, String tag) {
                return SIMULATED_EXISTING_TAGS.contains(tag);
            }
        };
        buildDistro.bundled = bundled;
        File targetDir = temporaryFolder.newFolder();
        buildDistro.copyDockerfile(targetDir, distroProperties);
        return FileUtils.readLines(new File(targetDir, "Dockerfile"), "UTF-8");
    }

    // -----------------------------------------------------------------------
    // 2.5+ dynamic Dockerfile — tag resolved via Docker Hub probe
    // -----------------------------------------------------------------------

    @Test
    public void copyDockerfile_platform25_shouldResolveToNightlyTag() throws Exception {
        List<String> lines = generateDockerfile("2.5.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.5.x-nightly"));
    }

    @Test
    public void copyDockerfile_platform26_shouldResolveToNightlyTag() throws Exception {
        List<String> lines = generateDockerfile("2.6.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.6.x-nightly"));
    }

    @Test
    public void copyDockerfile_platform27_shouldResolveToRollingTag() throws Exception {
        List<String> lines = generateDockerfile("2.7.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.x"));
    }

    @Test
    public void copyDockerfile_platform3x_shouldResolveToRollingTag() throws Exception {
        List<String> lines = generateDockerfile("3.0.0", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:3.0.x"));
    }

    // -----------------------------------------------------------------------
    // 1.x and pre-2.5 2.x — static Dockerfiles
    // -----------------------------------------------------------------------

    @Test
    public void copyDockerfile_platform1x_shouldUseStaticJre7Dockerfile() throws Exception {
        List<String> lines = generateDockerfile("1.12.0", new Properties(), false);
        assertThat(lines, hasItem("FROM tomcat:7-jre7"));
    }

    @Test
    public void copyDockerfile_platform1xBundled_shouldUseStaticJre7BundledDockerfile() throws Exception {
        List<String> lines = generateDockerfile("1.12.0", new Properties(), true);
        assertThat(lines, hasItem("FROM tomcat:7-jre7"));
        assertThat(lines, not(hasItem("COPY modules /usr/local/tomcat/.OpenMRS/modules")));
    }

    @Test
    public void copyDockerfile_platformBelow25_shouldUseStaticJre8Dockerfile() throws Exception {
        List<String> lines = generateDockerfile("2.4.0", new Properties(), false);
        assertThat(lines, hasItem("FROM tomcat:7-jdk8"));
    }

    @Test
    public void copyDockerfile_platformBelow25Bundled_shouldUseStaticJre8BundledDockerfile() throws Exception {
        List<String> lines = generateDockerfile("2.4.0", new Properties(), true);
        assertThat(lines, hasItem("FROM tomcat:7-jdk8"));
        assertThat(lines, not(hasItem("COPY modules /usr/local/tomcat/.OpenMRS/modules")));
    }

    @Test
    public void copyDockerfile_platformBelow25WithCustomNamespace_shouldUseDynamicDockerfile() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_NAMESPACE, "myorg");
        List<String> lines = generateDockerfile("2.4.0", props, false);
        assertThat(lines, hasItem("FROM myorg/openmrs-core:2.4.x"));
    }

    // -----------------------------------------------------------------------
    // Explicit overrides — docker.image.tag and docker.image.openmrsVersion bypass the probe
    // -----------------------------------------------------------------------

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
    public void copyDockerfile_withSnapshotVersion_shouldUseSnapshotVersionAsTag() throws Exception {
        Properties props = new Properties();
        props.setProperty(BuildDistro.DOCKER_IMAGE_OPENMRS_VERSION, "2.7.0-SNAPSHOT");
        List<String> lines = generateDockerfile("2.7.0", props, false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.7.0-SNAPSHOT"));
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
    public void copyDockerfile_withSnapshotWarVersion_shouldFallBackToRollingTag() throws Exception {
        // SNAPSHOT versions never have a dedicated Docker image, so the lookup must fall through
        // to the .x rolling tag (2.8.x in this case) rather than producing an unusable FROM line.
        List<String> lines = generateDockerfile("2.8.7-SNAPSHOT", new Properties(), false);
        assertThat(lines, hasItem("FROM openmrs/openmrs-core:2.8.x"));
    }

    // -----------------------------------------------------------------------
    // bundled / non-bundled COPY line assertions
    // -----------------------------------------------------------------------

    @Test
    public void copyDockerfile_notBundled_shouldIncludeAllCopyLines() throws Exception {
        List<String> lines = generateDockerfile("2.5.0", new Properties(), false);
        assertThat(lines, hasItem("COPY openmrs_modules /openmrs/distribution/openmrs_modules"));
        assertThat(lines, hasItem("COPY openmrs_owas /openmrs/distribution/openmrs_owas"));
        assertThat(lines, hasItem("COPY openmrs_config /openmrs/distribution/openmrs_config"));
        assertThat(lines, hasItem("COPY openmrs_spa /openmrs/distribution/openmrs_spa"));
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
