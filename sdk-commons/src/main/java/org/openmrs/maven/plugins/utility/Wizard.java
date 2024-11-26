package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.util.ArrayDeque;
import java.util.List;

public interface Wizard {
    boolean isInteractiveMode();

    void setInteractiveMode(boolean interactiveMode);

    void promptForNewServerIfMissing(Server server) throws MojoExecutionException;

    void promptForDb(Server server, DockerHelper dockerHelper, boolean h2supported, String dbDriver, String dockerHost) throws MojoExecutionException;

    void promptForDbCredentialsIfMissing(Server server) throws MojoExecutionException;

    String promptForPlatformVersionIfMissing(String version, List<String> versions) throws MojoExecutionException;

    String promptForPlatformVersion(List<String> versions) throws MojoExecutionException;

    void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper) throws MojoExecutionException;

    void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper, String customMessage) throws MojoExecutionException;

    void promptForO3RefAppVersionIfMissing(Server server, VersionsHelper versionsHelper, String customMessage) throws MojoExecutionException;

    void promptForO3RefAppVersionIfMissing(Server server, VersionsHelper versionsHelper) throws MojoExecutionException;

    String promptForRefAppVersion(VersionsHelper versionsHelper) throws MojoExecutionException;

    String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion, String distroName, VersionsHelper versionsHelper, String customMessage)
            throws MojoExecutionException;

    String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion, String distroName, VersionsHelper versionsHelper)
            throws MojoExecutionException;

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options)
            throws MojoExecutionException;

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, String customMessage, String customDefault)
            throws MojoExecutionException;

    void showMessage(String message);

    void showMessageNoEOL(String message);

    void showError(String message);

    void showWarning(String message);

    String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue)
            throws MojoExecutionException;

    String promptForValueIfMissing(String value, String parameterName) throws MojoExecutionException;

    String promptForPasswordIfMissing(String value, String parameter) throws MojoExecutionException;

    void promptForJavaHomeIfMissing(Server server) throws MojoExecutionException;

    boolean promptYesNo(String text) throws MojoExecutionException;

    boolean checkYes(String value);

    String promptForExistingServerIdIfMissing(String serverId) throws MojoExecutionException;

    List<String> getListOfServers() throws MojoExecutionException;

    void showJdkErrorMessage(String jdk, String platform, String recommendedJdk, String pathToProps);

    boolean promptForConfirmDistroUpgrade(UpgradeDifferential upgradeDifferential) throws MojoExecutionException;

    void setAnswers(ArrayDeque<String> batchAnswers);
}
