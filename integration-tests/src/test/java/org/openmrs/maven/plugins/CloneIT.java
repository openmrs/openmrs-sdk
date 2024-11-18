package org.openmrs.maven.plugins;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class CloneIT extends AbstractSdkIT {

    @Test
    public void clone_shouldCloneRepository() throws Exception {

        final String MODULE_ARTIFACT_ID = "appui";
        final String USERNAME = "tmarzeion";
        final String PASSWORD = "TEST CASE";

        addTaskParam("groupId", "org.openmrs.module");
        addTaskParam("artifactId", MODULE_ARTIFACT_ID);
        addTaskParam("githubUsername", USERNAME);
        addTaskParam("githubPassword", PASSWORD);
        executeTask("clone");

        // Check build success
        assertSuccess();

        // Check if files are present
        assertFilePresent("openmrs-module-appui", "pom.xml");

        // Check if upstream was added
        String absolutePath = new File(testDirectory.getAbsolutePath(), "openmrs-module-appui").getAbsolutePath();
        Repository repository = new RepositoryBuilder().findGitDir(new File(absolutePath)).build();
        Config storedConfig = repository.getConfig();
        Set<String> remotes = storedConfig.getSubsections("remote");

        assertThat(remotes, hasItem("upstream"));
    }
}
