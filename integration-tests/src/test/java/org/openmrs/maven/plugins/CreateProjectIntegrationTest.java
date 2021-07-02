package org.openmrs.maven.plugins;

import org.apache.maven.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CreateProjectIntegrationTest extends AbstractSdkIntegrationTest {

    Model model;

    @Before
    public void setup() throws Exception {
        //test pom needs reset after this test, because archetype plugin modifies it
        //overwrite superclass setup to ensure testDirectory is initialized
        super.setup();
        model = getTestPom(testDirectory);
    }

    @After
    public void resetPom() throws Exception {
        saveTestPom(testDirectory, model);
    }

    @Test
    public void createProject_shouldCreateRefappModuleProject() throws Exception{
        addTaskParam("type", "referenceapplication-module");

        addTaskParam("moduleId", "test");
        addTaskParam("moduleName", "Test");
        addTaskParam("moduleDescription", "none");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("version", "1.0.0-SNAPSHOT");

        addAnswer("2.4");

        addTaskParam(BATCH_ANSWERS, getAnswers()); //only to set interactive mode to false

        executeTask("create-project");
        assertSuccess();
        assertProjectCreated();
    }

    @Test
    public void createProject_shouldCreateRefappModuleProjectUsingBatchAnswers() throws Exception{
        addAnswer("Reference Application module");
        addAnswer("test");
        addAnswer("Test");
        addAnswer("none");
        addAnswer("org.openmrs.module");
        addAnswer("1.0.0-SNAPSHOT");
        addAnswer("2.4");

        executeTask("create-project");
        assertSuccess();
        assertProjectCreated();
    }

    @Test
    public void createProject_shouldCreatePlatformModuleProject() throws Exception{
        addTaskParam("type", "platform-module");

        addTaskParam("moduleId", "test");
        addTaskParam("moduleName", "Test");
        addTaskParam("moduleDescription", "none");
        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("version", "1.0.0-SNAPSHOT");

        addAnswer("1.11.6");
        addTaskParam(BATCH_ANSWERS, getAnswers()); //only to set interactive mode to false

        executeTask("create-project");
        assertSuccess();
        assertProjectCreated();
    }

    private void assertProjectCreated() {
        //check only basic structure of module project and pom existence, not coupled with archetype itself
        assertFilePresent("test");

        assertFilePresent("test", "api");
        assertFilePresent("test", "omod");
        assertFilePresent("test", "pom.xml");
    }
}
