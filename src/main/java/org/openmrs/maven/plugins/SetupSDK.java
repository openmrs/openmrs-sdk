package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.SettingsManager;

import java.io.*;

/**
 *
 * @goal setup-sdk
 * @requiresProject false
 *
 */
public class SetupSDK extends AbstractMojo{

    private static final String SUCCESS_TEMPLATE = "SDK installed successfully, settings file: %s";
    private static final String SDK_INFO = "Now you can use sdk: mvn openmrs-sdk:<task_name>";

    public void execute() throws MojoExecutionException, MojoFailureException {
        File mavenHome = new File(System.getProperty("user.home"), SDKConstants.MAVEN_SETTINGS_FOLDER);
        mavenHome.mkdirs();
        File mavenSettings = new File(mavenHome, SDKConstants.MAVEN_SETTINGS);
        try {
            SettingsManager settings = null;
            if (!mavenSettings.exists()) {
                mavenSettings.createNewFile();
                settings = new SettingsManager();
            }
            else {
                InputStream stream = new FileInputStream(mavenSettings);
                settings = new SettingsManager(stream);
            }
            InputStream settingsStream = getClass().getClassLoader().getResourceAsStream(SDKConstants.MAVEN_SETTINGS);
            SettingsManager defaultSettings = new SettingsManager(settingsStream);
            settings.updateSettings(defaultSettings.getSettings());
            OutputStream out = new FileOutputStream(mavenSettings);
            settings.apply(out);
            getLog().info(String.format(SUCCESS_TEMPLATE, mavenSettings.getPath()));
            getLog().info(SDK_INFO);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
