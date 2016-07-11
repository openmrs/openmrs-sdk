package org.openmrs.maven.plugins.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 *
 */
public class SdkStatisticsTest {

    private SdkStatistics sdkStatistics;

    @Before
    public void loadStatsFile() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        sdkStatistics = new SdkStatistics(new File(classLoader.getResource("sdk-stats.properties").getFile()));
    }

    @Test
    public void shouldIncrementGoal() throws Exception {
        int beforeCalls = sdkStatistics.getGoalCalls("Setup");
        beforeCalls++;
        sdkStatistics.incrementGoal("Setup");

        assertThat(beforeCalls, is(sdkStatistics.getGoalCalls("Setup")));
    }

    @Test
    public void shouldSetStatsEnabledMode(){
        sdkStatistics.setStatsEnabled(true);

        assertThat(sdkStatistics.getStatsEnabled(), is(true));
    }

}
