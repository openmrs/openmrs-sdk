package org.openmrs.maven.plugins;

import org.apache.maven.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CreateProjectIntegrationTest extends AbstractSdkIntegrationTest {


    Model model;

    List<String> options = Arrays.asList("moduleId", "moduleName", "version", "groupId", "artifactId", "moduleDescription");

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

        for( String option : options){
            addTaskParam(option, "test");
        }
        executeTask("create-project");
        assertSuccess();
        assertProjectCreated();
    }

    @Test
    public void createProject_shouldCreatePlatformModuleProject() throws Exception{
        addTaskParam("type", "platform-module");

        for( String option : options){
            addTaskParam(option, "test");
        }
        executeTask("create-project");
        assertSuccess();
        assertProjectCreated();
    }

    private void assertProjectCreated() {
        //check only basic structure of module project and pom existence, not coupled with archetype itself
        assertFilePresent("test");

        assertFilePresent("test"+ File.separator+"api");
        assertFilePresent("test"+ File.separator+"omod");
        assertFilePresent("test"+ File.separator+"pom.xml");
    }
}
