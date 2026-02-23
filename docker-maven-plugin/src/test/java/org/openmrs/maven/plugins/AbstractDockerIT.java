package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractDockerIT {

    protected static final String MOJO_OPTION_TMPL = "-D%s=\"%s\"";

    protected static int counter = 0;

    protected Verifier verifier;

    protected File testDirectory;

    protected Path testDirectoryPath;

    protected Path testBaseDir;

    protected DockerClient dockerClient;

    public String resolvePluginArtifact() throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("plugin.properties")) {
            assertNotNull("plugin.properties not found on classpath", is);
            props.load(is);
        }
        return props.getProperty("groupId") + ":" + props.getProperty("artifactId") + ":" + props.getProperty("version");
    }

    @Before
    public void setup() throws Exception {
        Path classesPath = Paths.get(Objects.requireNonNull(getClass().getResource("/")).toURI());
        testBaseDir = classesPath.resolveSibling("docker-it-base-dir");
        testDirectoryPath = testBaseDir.resolve(getClass().getSimpleName() + "_" + nextCounter());
        testDirectory = testDirectoryPath.toFile();
        FileUtils.deleteQuietly(testDirectory);
        if (!testDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create test directory: " + testDirectory);
        }

        createTestPom();

        verifier = new Verifier(testDirectory.getAbsolutePath());
        verifier.setAutoclean(false);

        dockerClient = createDockerClient();
    }

    @After
    public void teardown() throws IOException {
        if (verifier != null) {
            verifier.resetStreams();
        }
        if (dockerClient != null) {
            dockerClient.close();
        }
        if (Boolean.parseBoolean(System.getProperty("deleteTestArtifacts"))) {
            FileUtils.deleteQuietly(testDirectory);
        }
    }

    static synchronized int nextCounter() {
        return counter++;
    }

    protected DockerClient createDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(
                        new ApacheDockerHttpClient.Builder()
                                .dockerHost(config.getDockerHost())
                                .sslConfig(config.getSSLConfig())
                                .build()
                ).build();
    }

    protected void createTestPom() throws IOException {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>org.openmrs.maven.test</groupId>\n" +
                "    <artifactId>docker-test</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "</project>";
        Files.write(testDirectoryPath.resolve("pom.xml"), pomContent.getBytes());
    }

    protected void addTaskParam(String param, String value) {
        verifier.addCliOption(String.format(MOJO_OPTION_TMPL, param, value));
    }

    protected void executeGoal(String goal) throws VerificationException, Exception {
        String plugin = resolvePluginArtifact();
        verifier.executeGoal(plugin + ":" + goal);
    }

    protected void assertSuccess() throws Exception {
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
    }

    protected Container findContainer(String name) {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            List<String> names = Arrays.asList(container.getNames());
            if (names.contains(name) || names.contains("/" + name)) {
                return container;
            }
        }
        return null;
    }

    protected void cleanupContainer(String containerName) {
        try {
            Container container = findContainer(containerName);
            if (container != null) {
                String containerId = container.getId();
                try {
                    dockerClient.stopContainerCmd(containerId).exec();
                } catch (com.github.dockerjava.api.exception.NotModifiedException ignored) {
                } catch (Exception ignored) {
                }
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    protected void cleanupVolume(String volumeName) {
        try {
            dockerClient.removeVolumeCmd(volumeName).exec();
        } catch (Exception ignored) {
        }
    }

    protected boolean isDockerAvailable() {
        try {
            dockerClient.infoCmd().exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void assumeDockerAvailable() {
        org.junit.Assume.assumeTrue("Docker is not available", isDockerAvailable());
    }
}
