package org.openmrs.maven.plugins.model;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openmrs.maven.plugins.model.SdkStatistics.SDK_STATS_FILE_NAME;

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
    public void shouldIncrementGoal() {
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

    @Test
    public void testCheckIfOneWeekApart_LessThanOneWeekApart() {
        assertFalse(sdkStatistics.checkIfOneWeekApart(LocalDate.now().minusDays(6)));
    }

    @Test
    public void testCheckIfOneWeekApart_MoreThanOneWeekApart() {
        assertTrue(sdkStatistics.checkIfOneWeekApart(LocalDate.now().minusDays(8)));
    }

    @Test
    public void testCheckIfOneWeekApart_OneWeekApart() {
        assertFalse(sdkStatistics.checkIfOneWeekApart(LocalDate.now().minusDays(7)) );
    }


}
