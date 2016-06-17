package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;

import java.io.File;
import java.util.List;

public interface Wizard {
    void promptForNewServerIfMissing(Server server);

    void promptForMySQLDb(Server server);

    void promptForH2Db(Server server);

    void promptForDbCredentialsIfMissing(Server server);

    void promptForPlatformVersionIfMissing(Server server, List<String> versions);

    String promptForPlatformVersion(List<String> versions);

    void promptForDistroVersionIfMissing(Server server) throws MojoExecutionException;

    String promptForDistroVersion();

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, boolean allowCustom);

    void showMessage(String message);

    String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue);

    String promptForValueWithDefaultList(String value, String parameterName, List<String> values);

    String promptForValueWithDefaultList(String value, String parameterName, String defaultValue, List<String> values);

    String promptForValueIfMissing(String value, String parameterName);

    boolean promptForInstallDistro();

    boolean promptYesNo(String text);

    boolean checkYes(String value);

    File getCurrentServerPath() throws MojoExecutionException;

    String promptForExistingServerIdIfMissing(String serverId);

    List<String> getListOf5RecentServers();

    String addMySQLParamsIfMissing(String dbUri);

    void showJdkErrorMessage(String jdk, String platform, String recommendedJdk, String pathToProps);

    boolean promptForConfirmDistroUpgrade(UpgradeDifferential upgradeDifferential, Server server, DistroProperties distroProperties);

}
