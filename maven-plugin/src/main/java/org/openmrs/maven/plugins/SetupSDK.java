package org.openmrs.maven.plugins;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @goal setup-sdk
 * @requiresProject false
 *
 */
public class SetupSDK extends AbstractMojo{

    private static final String SUCCESS_TEMPLATE = "SDK installed successfully, settings file: %s";
    private static final String SDK_INFO = "Now you can use sdk: mvn openmrs-sdk:<task_name>";

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     */
    private MavenSession mavenSession;

    public void execute() throws MojoExecutionException, MojoFailureException {
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
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
