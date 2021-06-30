package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mojo(name = "watch-owa", requiresProject = false)
public class WatchOwa extends AbstractServerTask {

    @Override
    public void executeTask() throws MojoExecutionException {
        String configFilename = "webpack.config.js";
        File configFile = new File(configFilename);
        if (configFile.exists()) {
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
            throw new IllegalStateException("Config file not found at" + new File(configFilename).getAbsolutePath());
        }
    }

    private String getChosenServerPort() throws MojoExecutionException {
        final String defaultTomcatPort = "8080";
        Map<String,String> port = getServer().getServerProperty("tomcat.port");
        if (port == null) {
            return defaultTomcatPort;
        }
        else {
            return port.get("port");
        }
    }

    @Override
    public Server loadServer() throws MojoExecutionException {
        return loadValidatedServer(serverId);
    }
}
