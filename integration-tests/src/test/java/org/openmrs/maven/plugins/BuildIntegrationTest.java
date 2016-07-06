package org.openmrs.maven.plugins;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.util.UUID;

/**
 *
 */
public class BuildIntegrationTest extends AbstractSdkIntegrationTest{

    private String serverId;


    @Before
    public void setupServer() throws Exception{
        serverId = setupTestServer();

        Server server = Server.loadServer(new File(testDirectory, serverId));
        File firstDir = new File(testDirectory, "module1");
        File secondDir = new File(testDirectory, "module2");
        server.addWatchedProject(Project.loadProject(firstDir));
        server.addWatchedProject(Project.loadProject(secondDir));
        server.save();

        clearParams();
    }

    @After
    public void deleteServer() throws Exception {
        deleteTestServer(serverId);
    }

    @Test
    public void build_shouldBuildAllWatchedProjects() throws Exception{

        assertFileNotPresent(serverId+ File.separator+"module2-1.0-SNAPSHOT.omod");
        assertFileNotPresent(serverId+ File.separator+"module1-1.0-SNAPSHOT.omod");

        addTaskParam("openMRSPath",testDirectory.getAbsolutePath());

        addAnswer(serverId);
        addTaskParam(BATCH_ANSWERS, getAnswers());


        executeTask("build");

        assertSuccess();
        assertModulesInstalled(serverId, "module2-1.0-SNAPSHOT.omod");
        assertModulesInstalled(serverId, "module1-1.0-SNAPSHOT.omod");

    }
}
