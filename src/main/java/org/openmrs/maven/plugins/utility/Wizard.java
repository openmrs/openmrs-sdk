package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.List;

/**
 * Created by user on 30.05.16.
 */
public interface Wizard {
    String promptForNewServerIfMissing(String omrsPath, String serverId);

    String promptForValueIfMissingWithDefault(String value, String parameterName, String defValue);

    String promptForValueWithDefaultList(String value, String parameterName, List<String> values);

    String promptForValueWithDefaultList(String value, String parameterName, String defaultValue, List<String> values);

    String promptForValueIfMissing(String value, String parameterName);

    boolean promptYesNo(String text);

    boolean checkYes(String value);

    File getServerPath(String serverId, String failureMessage) throws MojoFailureException;

    File getCurrentServerPath() throws MojoExecutionException;

    File getServerPath(String serverId) throws MojoFailureException;

    List<String> getListOf5RecentServers();

    String addMySQLParamsIfMissing(String dbUri);
}
