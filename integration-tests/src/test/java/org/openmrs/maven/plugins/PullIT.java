package org.openmrs.maven.plugins;

import org.apache.maven.it.Verifier;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.nio.file.Path;

public class PullIT extends AbstractSdkIT {

    private static final String OPENMRS_MODULE_IDGEN = "openmrs-module-idgen";
    private static final String PULL_GOAL = "pull";
    private static final String MODULE_GITHUB_URL = "https://github.com/openmrs/openmrs-module-idgen.git";
    private static final String SUCCESS_MESSAGE = "updated successfully";
    private static final String NO_WATCHED_PROJECTS_MESSAGE = "Server with id %s has no watched modules";

    private static Path templateRefApp22;

    private String serverId;

    @BeforeClass
    public static void setupTemplate() throws Exception {
        Path testBaseDir = resolveTestBaseDir();
        File nodeCacheDir = testBaseDir.resolve("node-cache").toFile();
        Path testResourceDir = testBaseDir.resolve("test-resources");
        templateRefApp22 = getOrCreateTemplateServer("referenceapplication:2.2", testBaseDir, nodeCacheDir, testResourceDir);
    }

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
        serverId = copyServerFromTemplate(templateRefApp22);
        cloneGitProject();
        verifier = new Verifier(new File(testDirectory, OPENMRS_MODULE_IDGEN).getAbsolutePath());
        verifier.setSystemProperty("nodeCacheDir", nodeCacheDir.getAbsolutePath());
        addTaskParam("openMRSPath", testDirectory.getAbsolutePath());
    }

    @Test
    public void shouldUpdateWatchedProjectsToLatestUpstreamMaster() throws Exception {
        addWatchedProjects();
        addAnswer(serverId);
        executeTask(PULL_GOAL);
        verifier.verifyTextInLog(SUCCESS_MESSAGE);
        assertSuccess();
    }

    @Test
    public void  shouldNotifyThereIsNoWatchedProjects() throws Exception {
        addTaskParam("serverId", serverId);
        executeTask(PULL_GOAL);
        verifier.verifyTextInLog(String.format(NO_WATCHED_PROJECTS_MESSAGE, serverId));
        assertSuccess();
    }

    @Test
    public void shouldUpdateWatchedProjectsWithGivenServerIdFromGitProjectDir() throws Exception{
        addWatchedProjects();
        addTaskParam("serverId", serverId);
        executeTask(PULL_GOAL);
        verifier.verifyTextInLog(SUCCESS_MESSAGE);
        assertSuccess();
    }

    @Test
    public void shouldUpdateGitProjectFromDir() throws Exception {
        executeTask(PULL_GOAL);
        verifier.verifyTextInLog(SUCCESS_MESSAGE);
        assertSuccess();
    }

    private void cloneGitProject() throws GitAPIException {
        Git result = Git.cloneRepository()
                .setURI(MODULE_GITHUB_URL)
                .setDirectory(new File(testDirectory, OPENMRS_MODULE_IDGEN))
                .call();

        result.close();
    }

    private void addWatchedProjects() throws MojoExecutionException {
        Server server = Server.loadServer(testDirectoryPath.resolve(serverId));
        File firstDir = new File(testDirectory, OPENMRS_MODULE_IDGEN);
        server.addWatchedProject(Project.loadProject(firstDir));
        server.save();
    }

}
