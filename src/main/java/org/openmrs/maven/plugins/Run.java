package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @goal run
 * @requiresProject false
 */
public class Run extends AbstractMojo {

    private static final String NO_SERVER_TEXT = "There no server with given serverId. Please create it using omrs:setup first";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        ModuleInstall installer = new ModuleInstall(prompter);
        File serverPath = installer.getServerPath(helper, serverId, NO_SERVER_TEXT);
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "install");
            pb.directory(serverPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            int exitCode = p.exitValue();
            if (exitCode == 0) {
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    getLog().info(s);
                }
                File server = new File(serverPath, "server");
                ProcessBuilder pb2 = new ProcessBuilder("mvn", "jetty:run");
                pb2.directory(server);
                pb2.redirectErrorStream(true);
                Process p2 = pb2.start();
                p2.waitFor();
                int exitCode2 = p2.exitValue();
                if (exitCode2 == 0) {
                    BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
                    while ((s = stdInput2.readLine()) != null) {
                        getLog().info(s);
                    }
                }
                else {
                    throw new MojoExecutionException("There are error during starting server");
                }
            }
            else {
                throw new MojoExecutionException("There are error during installing server");
            }

        } catch (Exception e) {
            getLog().error(e.getMessage());
        }
    }
}
