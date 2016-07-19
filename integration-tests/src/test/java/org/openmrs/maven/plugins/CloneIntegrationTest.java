package org.openmrs.maven.plugins;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class CloneIntegrationTest extends AbstractSdkIntegrationTest {

    private static final String ORIGIN_URL = "https://github.com/tmarzeion/openmrs-module-appui.git";
    private static final String UPSTREAM_URL = "https://github.com/openmrs/openmrs-module-appui.git";

    @Test
    public void clone_shouldCloneRepository() throws Exception{

        String moduleArtifactId = "appui";
        String username = "tmarzeion";
        String password = "TEST CASE";

        addTaskParam("artifactId", moduleArtifactId);
        addTaskParam("githubUsername", username);
        addTaskParam("githubPassword", password);
        executeTask("clone");

        // Check build success
        assertSuccess();

        // Check if files are present
        assertFilePresent("/openmrs-module-appui/pom.xml");

        // Check if upstream was added
        String absolutePath = new File(testDirectory.getAbsolutePath(), "/openmrs-module-appui").getAbsolutePath();
        Repository repository = new RepositoryBuilder().findGitDir(new File(absolutePath)).build();
        Config storedConfig = repository.getConfig();
        Set<String> remotes = storedConfig.getSubsections("remote");

        assertThat(remotes, hasItem("upstream"));
    }

    @After
    public void deleteLocalRepo() throws Exception {
        File repoDir = new File("/openmrs-module-appui");
        if (repoDir.exists()) {
            repoDir.delete();
        }
    }
}