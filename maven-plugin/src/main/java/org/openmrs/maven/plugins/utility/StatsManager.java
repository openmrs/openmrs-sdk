package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.SdkStatistics;

/**
 *
 */
public class StatsManager {

    private SdkStatistics sdkStatistics;

    private Wizard wizard;

    private MavenSession mavenSession;

    public StatsManager(Wizard wizard, MavenSession mavenSession){
        this.wizard = wizard;
        this.mavenSession = mavenSession;
    }

    private void loadStatistics() throws MojoExecutionException {
        try {
            sdkStatistics = SdkStatistics.loadStatistics();
        } catch (IllegalStateException e) {
            boolean agree = wizard.promptYesNo(SDKConstants.SDK_STATS_ENABLED_QUESTION);
            sdkStatistics = new SdkStatistics().createSdkStatsFile(agree);
            sdkStatistics.sendReport(wizard);
        }
    }

    public void incrementGoalStats() throws MojoExecutionException {
        loadStatistics();

        if(sdkStatistics.getStatsEnabled() && wizard.isInteractiveMode()){
            String goal = getGoal();
            if (StringUtils.isNotBlank(goal)) {
                sdkStatistics.incrementGoal(goal);
                sdkStatistics.setLastUsed();
                sdkStatistics.save();
            }
            sdkStatistics.sendReport(wizard);
        }
    }

    private String getGoal() {
        for(String mvnGoal: mavenSession.getGoals()){
            if(mvnGoal.contains("openmrs-sdk-maven-plugin")){
                String goal = mvnGoal.substring(mvnGoal.lastIndexOf(":")+1);
                return goal.substring(0,1).toUpperCase() + goal.substring(1);
            }
        }
        return null;
    }
}
