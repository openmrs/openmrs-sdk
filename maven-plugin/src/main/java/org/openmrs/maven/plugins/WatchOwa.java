package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.utility.OwaHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @goal watch-owa
 * @requiresProject false
 *
 */
public class WatchOwa extends AbstractTask {

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    private final String CONFIG_FILENAME = "webpack.config.js";

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        OwaHelper owaHelper = new OwaHelper(mavenSession, mavenProject, pluginManager, wizard);
        File configFile = new File(CONFIG_FILENAME);
        if (configFile.exists()) {
            serverId = wizard.promptForExistingServerIdIfMissing(serverId);
            String port = getChosenServerPort();
            List<String> args = new ArrayList<>();
            args.add("run");
            args.add("watch");
            if (port != null) {
                args.add("-- --targetPort=" + port);
            }
            String projectNodeVersion = owaHelper.getProjectNodeVersion();
            if (projectNodeVersion != null) {
                owaHelper.runLocalNpmCommandWithArgs(args);
            } else {
                owaHelper.runSystemNpmCommandWithArgs(args);
            }
        } else {
            throw new IllegalStateException("Config file not found at" + new File(CONFIG_FILENAME).getAbsolutePath());
        }
    }

    private String getChosenServerPort() throws MojoExecutionException {
        final String defaultTomcatPort = "8080";
        Map<String,String> port = loadValidatedServer(serverId).getServerProperty("tomcat.port");
        if (port == null) {
            return defaultTomcatPort;
        }
        else {
            return port.get("port");
        }
    }

}
