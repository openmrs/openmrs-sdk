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

    private static final String DATE_FORMAT = "dd-M-yyyy";
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
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
    public void shouldSetStatsEnabledMode() {
        sdkStatistics.setStatsEnabled(true);

        assertThat(sdkStatistics.getStatsEnabled(), is(true));
    }

    @Test
    public void checkIfOneWeekApart_LessThanOneWeekApart() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field statisticsField = sdkStatistics.getClass().getDeclaredField("statistics");
        statisticsField.setAccessible(true);
        Properties statistics = (Properties) statisticsField.get(sdkStatistics);
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(6).format(dateTimeFormatter));
        Method checkIfOneWeekFromLastReportMethod = sdkStatistics.getClass().getDeclaredMethod("checkIfOneWeekFromLastReport");
        checkIfOneWeekFromLastReportMethod.setAccessible(true);
        assertFalse((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));

    }


    @Test
    public void checkIfOneWeekApart_MoreThanOneWeekApart() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field statisticsField = sdkStatistics.getClass().getDeclaredField("statistics");
        statisticsField.setAccessible(true);
        Properties statistics = (Properties) statisticsField.get(sdkStatistics);
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(8).format(dateTimeFormatter));
        Method checkIfOneWeekFromLastReportMethod = sdkStatistics.getClass().getDeclaredMethod("checkIfOneWeekFromLastReport");
        checkIfOneWeekFromLastReportMethod.setAccessible(true);
        assertTrue((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));
    }

    @Test
    public void checkIfOneWeekApart_OneWeekApart() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field statisticsField = sdkStatistics.getClass().getDeclaredField("statistics");
        statisticsField.setAccessible(true);
        Properties statistics = (Properties) statisticsField.get(sdkStatistics);
        statistics.setProperty("statsLastReported", LocalDate.now().minusDays(7).format(dateTimeFormatter));
        Method checkIfOneWeekFromLastReportMethod = sdkStatistics.getClass().getDeclaredMethod("checkIfOneWeekFromLastReport");
        checkIfOneWeekFromLastReportMethod.setAccessible(true);
        assertFalse((Boolean) checkIfOneWeekFromLastReportMethod.invoke(sdkStatistics));
    }


}
