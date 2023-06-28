package org.openmrs.maven.plugins.model;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SdkStatisticsTest {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-M-yyyy");
    private SdkStatistics sdkStatistics;
    private Properties statistics;
    private Method checkIfOneWeekFromLastReportMethod;

    @Before
    public void before() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        sdkStatistics = new SdkStatistics(new File(classLoader.getResource("sdk-stats.properties").getFile()));
        Field statisticsField = sdkStatistics.getClass().getDeclaredField("statistics");
        statisticsField.setAccessible(true);
        statistics = (Properties) statisticsField.get(sdkStatistics);
        checkIfOneWeekFromLastReportMethod = sdkStatistics.getClass().getDeclaredMethod("checkIfOneWeekFromLastReport");
        checkIfOneWeekFromLastReportMethod.setAccessible(true);
    }

    @Test
    public void shouldIncrementGoal() {
        int beforeCalls = sdkStatistics.getGoalCalls("Setup");
        beforeCalls++;
        sdkStatistics.incrementGoal("Setup");

        assertThat(beforeCalls, is(sdkStatistics.getGoalCalls("Setup")));
    }

    @Test
    public void shouldSetStatsEnabledMode() {
        sdkStatistics.setStatsEnabled(true);

        assertThat(sdkStatistics.getStatsEnabled(), is(true));
    }

    @Test
    public void checkIfOneWeekApart_LessThanOneWeekApart() throws IllegalAccessException, InvocationTargetException {
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(6).format(dateTimeFormatter));
        assertFalse((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));
    }


    @Test
    public void checkIfOneWeekApart_MoreThanOneWeekApart() throws IllegalAccessException, InvocationTargetException {
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(8).format(dateTimeFormatter));
        assertTrue((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));
    }

    @Test
    public void checkIfOneWeekApart_OneWeekApart() throws IllegalAccessException, InvocationTargetException {
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(7).format(dateTimeFormatter));
        assertFalse((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));
    }


}
