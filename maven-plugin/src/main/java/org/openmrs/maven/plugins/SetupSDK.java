package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.SdkStatistics;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Mojo(name = "setup-sdk", requiresProject = false)
public class SetupSDK extends AbstractTask {

	private static final String SUCCESS_TEMPLATE = "SDK installed successfully, settings file: %s";

	private static final String SDK_INFO = "Now you can use sdk: mvn openmrs-sdk:<task_name>";

	public void execute()
			throws MojoFailureException, MojoExecutionException { //execute method is overwritten to not change the workflow
		initTask();                                                             //in SetupSDK, but we need to extends AbstractTask to support batchAnswers
		executeTask();
	}

	public void executeTask() throws MojoExecutionException, MojoFailureException {
		initSdkStatsFile();
		String localRepository = mavenSession.getSettings().getLocalRepository();
		File mavenHome = new File(localRepository).getParentFile();
		mavenHome.mkdirs();
		File mavenSettings = new File(mavenHome, SDKConstants.MAVEN_SETTINGS);
		try {
			SettingsManager settings = new SettingsManager(mavenSession);
			InputStream settingsStream = getClass().getClassLoader().getResourceAsStream(SDKConstants.MAVEN_SETTINGS);
			SettingsManager defaultSettings = new SettingsManager(settingsStream);
			settings.updateSettings(defaultSettings.getSettings());
			OutputStream out = new FileOutputStream(mavenSettings);
			settings.apply(out);
			getLog().info(String.format(SUCCESS_TEMPLATE, mavenSettings.getPath()));
			getLog().info(SDK_INFO);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	private void initSdkStatsFile() throws MojoExecutionException {
		File openMrsServers = new File(Server.getServersPath());
		if (!openMrsServers.exists()) {
			openMrsServers.mkdirs();
		}
		File sdkStats = new File(Server.getServersPath(), "sdk-stats.properties");
		if (!sdkStats.exists()) {
			boolean agree;
			if (!mavenSession.getRequest().isInteractiveMode()) {
				agree = stats;
			} else {
				agree = wizard.promptYesNo(SDKConstants.SDK_STATS_ENABLED_QUESTION);
			}
			SdkStatistics sdkStatistics = new SdkStatistics().createSdkStatsFile(agree);
			sdkStatistics.sendReport(wizard);
		}
	}
}
