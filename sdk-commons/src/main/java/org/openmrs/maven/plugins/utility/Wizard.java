package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
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

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options) throws MojoExecutionException;

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, String customMessage, String customDefault) throws MojoExecutionException;

    String promptForArtifactVersion(String message, Artifact artifact, String otherMessage, VersionsHelper versionsHelper) throws MojoExecutionException;

    String promptForPlatformVersion(VersionsHelper versionsHelper) throws MojoExecutionException;

    Artifact promptForPlatformArtifact(VersionsHelper versionsHelper) throws MojoExecutionException;

    Artifact promptForRefApp2xArtifact(VersionsHelper versionsHelper) throws MojoExecutionException;

    Artifact promptForRefApp3xArtifact(VersionsHelper versionsHelper) throws MojoExecutionException;

    void showMessage(String message);

    void showMessageNoEOL(String message);

    void showError(String message);

    void showWarning(String message);

    String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue) throws MojoExecutionException;

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

    String promptForPasswordIfMissingWithDefault(String s, String dbPassword, String dbPassword1, String s1) throws MojoExecutionException;
}
