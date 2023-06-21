package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.SdkStatistics;

import java.util.Optional;

/**
 *
 */
public class StatsManager {

    private SdkStatistics sdkStatistics;

    private final Wizard wizard;

    private final MavenSession mavenSession;

    private final boolean stats;

    public StatsManager(Wizard wizard, MavenSession mavenSession, boolean stats){
        this.wizard = wizard;
        this.mavenSession = mavenSession;
        this.stats = stats;
    }

    private void loadStatistics() throws MojoExecutionException {
        try {
            sdkStatistics = SdkStatistics.loadStatistics();
        } catch (MojoExecutionException e) {
            boolean agree;
            if (!mavenSession.getRequest().isInteractiveMode()) {
                agree = stats;
            } else {
                agree = wizard.promptYesNo(SDKConstants.SDK_STATS_ENABLED_QUESTION);
            }
            sdkStatistics = new SdkStatistics().createSdkStatsFile(agree);
            sdkStatistics.sendReport(wizard);
        }
    }

    public void incrementGoalStats() {
        try {
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
        } catch (Exception e) {
            wizard.showMessage("Failed to send anonymous user statistics. " + e.getMessage());
        }
    }

    private String getGoal() {
        Optional<String> foundGoal = mavenSession.getGoals().stream().filter(mvnGoal -> mvnGoal.contains("openmrs-sdk"))
                .map(mvnGoal -> {
                    String goal = mvnGoal.substring(mvnGoal.lastIndexOf(":") + 1);
                    return goal.substring(0, 1).toUpperCase() + goal.substring(1);
                }).findFirst();

        return foundGoal.orElse(null);
    }
}
