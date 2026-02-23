package org.openmrs.maven.plugins;

import com.github.dockerjava.api.model.Container;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

public class RunDbIT extends AbstractDockerIT {

    private static final String TEST_CONTAINER_NAME = "openmrs-sdk-test-rundb";

    @After
    public void cleanup() {
        cleanupContainer(TEST_CONTAINER_NAME);
        cleanupVolume(TEST_CONTAINER_NAME + "-data");
    }

    @Test
    public void runDb_shouldFailWhenContainerNotFound() throws Exception {
        assumeDockerAvailable();

        String nonExistentContainer = "non-existent-container-" + System.currentTimeMillis();
        addTaskParam("container", nonExistentContainer);

        try {
            executeGoal("run-db");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("BUILD FAILURE"));
        }
    }
}
