package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.util.List;

/**
 * Created by user on 30.05.16.
 */
public interface Wizard {
    void promptForNewServerIfMissing(Server server);

    void promptForMySQLDb(Server server);

    void promptForH2Db(Server server);

    void promptForDbCredentialsIfMissing(Server server);

    void promptForPlatformVersionIfMissing(Server server, List<String> versions);

    String promptForPlatformVersion(List<String> versions);

    void promptForDistroVersionIfMissing(Server server);

    String promptForDistroVersion();

    String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options, boolean allowCustom);

    void showMessage(String message);

    String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue);

    String promptForValueWithDefaultList(String value, String parameterName, List<String> values);

    String promptForValueWithDefaultList(String value, String parameterName, String defaultValue, List<String> values);

    String promptForValueIfMissing(String value, String parameterName);

    Artifact parseDistro(String distro);

    boolean promptForInstallDistro();

    boolean promptYesNo(String text);

    boolean checkYes(String value);

    File getCurrentServerPath() throws MojoExecutionException;

    String promptForExistingServerIdIfMissing(String serverId);

    List<String> getListOf5RecentServers();

    String addMySQLParamsIfMissing(String dbUri);
}
