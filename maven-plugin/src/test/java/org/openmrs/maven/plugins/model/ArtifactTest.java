package org.openmrs.maven.plugins.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArtifactTest {

    @Test
    public void testGetDestFileName() {
        Artifact nodashes = new Artifact("modulename-omod","1.4-SNAPSHOT");
        assertThat(nodashes.getDestFileName(), is("modulename-1.4-SNAPSHOT.jar"));

        Artifact withdashes = new Artifact("modulename-submodule-omod","1.2");
        assertThat(withdashes.getDestFileName(), is("modulename-submodule-1.2.jar"));

        Artifact withdoubledashes = new Artifact("modulename-submodule-subsubmodule-omod","5.7.1");
        assertThat(withdoubledashes.getDestFileName(), is("modulename-submodule-subsubmodule-5.7.1.jar"));

        Artifact withoutomod = new Artifact("modulename-anothername", "1.0.0");
        assertThat(withoutomod.getDestFileName(), is("modulename-anothername-1.0.0.jar"));
    }
}
