package org.openmrs.maven.plugins.utility;

import com.atlassian.util.concurrent.Nullable;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.util.ArrayDeque;
import java.util.List;

public interface Wizard {
    boolean isInteractiveMode();

    void setInteractiveMode(boolean interactiveMode);

    void promptForNewServerIfMissing(Server server);

    void promptForDb(Server server, DockerHelper dockerHelper, boolean h2supported, String dbDriver, String dockerHost) throws MojoExecutionException;

    public void promptForMySQLDb(Server server) throws MojoExecutionException;

    void promptForDbCredentialsIfMissing(Server server);

    String promptForPlatformVersionIfMissing(String version, List<String> versions);

    String promptForPlatformVersion(List<String> versions);

    public void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper) throws MojoExecutionException;

    void promptForRefAppVersionIfMissing(Server server, VersionsHelper versionsHelper, @Nullable String customMessage) throws MojoExecutionException;

    String promptForRefAppVersion(VersionsHelper versionsHelper);

    String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion, String distroName, VersionsHelper versionsHelper, @Nullable String customMessage);

    String promptForDistroVersion(String distroGroupId, String distroArtifactId, String distroVersion, String distroName, VersionsHelper versionsHelper);

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options);

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, String customMessage, String customDefault);

    void showMessage(String message);

    void showError(String message);

    String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue);

    String promptForValueWithDefaultList(String value, String parameterName, List<String> values);

    String promptForValueIfMissing(String value, String parameterName);

    String promptForPasswordIfMissing(String value, String parameter);

    void promptForJavaHomeIfMissing(Server server);

    boolean promptYesNo(String text);

    boolean checkYes(String value);

    String promptForExistingServerIdIfMissing(String serverId);

    List<String> getListOfServers();

    String addMySQLParamsIfMissing(String dbUri);

    void showJdkErrorMessage(String jdk, String platform, String recommendedJdk, String pathToProps);

    boolean promptForConfirmDistroUpgrade(UpgradeDifferential upgradeDifferential, Server server, DistroProperties distroProperties);

    void setAnswers(ArrayDeque<String> batchAnswers);
}
