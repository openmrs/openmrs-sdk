package org.openmrs.maven.plugins;

import com.github.dockerjava.api.model.Container;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CreateMySqlIT extends AbstractDockerIT {

    private static final String TEST_CONTAINER_NAME = "openmrs-sdk-test-mysql";
    private static final String TEST_PORT = "3310";
    private static final String TEST_PASSWORD = "testPass123";

    @After
    public void cleanup() {
        cleanupContainer(TEST_CONTAINER_NAME);
        cleanupVolume(TEST_CONTAINER_NAME + "-data");
    }

    @Test
    public void createMysql_shouldCreateContainerWithDefaultSettings() throws Exception {
        assumeDockerAvailable();

        cleanupContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
        cleanupVolume(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER + "-data");

        try {
            executeGoal("create-mysql");
            assertSuccess();

            Container container = findContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
            assertNotNull("Container should be created", container);
        } finally {
            cleanupContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
            cleanupVolume(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER + "-data");
        }
    }

    @Test
    public void createMysql_shouldCreateContainerWithCustomSettings() throws Exception {
        assumeDockerAvailable();

        addTaskParam("container", TEST_CONTAINER_NAME);
        addTaskParam("port", TEST_PORT);
        addTaskParam("rootPassword", TEST_PASSWORD);

        executeGoal("create-mysql");
        assertSuccess();

        Container container = findContainer(TEST_CONTAINER_NAME);
        assertNotNull("Container should be created with custom name", container);
    }

    @Test
    public void createMysql_shouldNotFailWhenDefaultContainerAlreadyExists() throws Exception {
        assumeDockerAvailable();

        cleanupContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
        cleanupVolume(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER + "-data");

        try {
            executeGoal("create-mysql");
            assertSuccess();

            verifier.getCliOptions().clear();

            executeGoal("create-mysql");
            assertSuccess();

            Container container = findContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
            assertNotNull("Container should still exist", container);
        } finally {
            cleanupContainer(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER);
            cleanupVolume(AbstractDockerMojo.DEFAULT_MYSQL_CONTAINER + "-data");
        }
    }
}
