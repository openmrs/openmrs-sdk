package org.openmrs.maven.plugins.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

public class ServerTest {
    @Test
    public void testMergeArtifactList() throws Exception{
        Artifact owa14 = new Artifact("owa", "1.4");
        Artifact appui15 = new Artifact("appui", "1.5");
        Artifact refapp2232 = new Artifact("refapp", "2232");
        Artifact groupDistroGroup = new Artifact("group", "222", "org.distro");

        List<Artifact> mainList = Arrays.asList(
                owa14,
                appui15,
                refapp2232,
                groupDistroGroup
        );

        Artifact owa15 = new Artifact("owa", "1.5");
        Artifact appui15snap = new Artifact("appui", "1.5-SNAPSHOT");
        Artifact platform2020 = new Artifact("platform module", "2020");
        Artifact refapp33 = new Artifact("refapp", "33");
        Artifact group3456 = new Artifact("group", "3456");

        List<Artifact> updated = Arrays.asList(
                owa15,
                appui15snap,
                platform2020,
                refapp33,
                group3456
        );

        List<Artifact> result = Server.mergeArtifactLists(mainList, updated);
        assertThat(result, hasItems(owa15, platform2020, refapp33, appui15snap, group3456, groupDistroGroup));
        assertThat(result, not(hasItems(owa14, appui15, refapp2232)));
    }
}
