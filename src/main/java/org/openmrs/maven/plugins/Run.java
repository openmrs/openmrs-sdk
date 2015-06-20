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
            Process p = pb.start();
            BufferedReader errInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            // if there are not errors, run next task
            String error = null;
            if ((error = errInput.readLine()) == null) {
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                }
                File server = new File(serverPath, "server");
                ProcessBuilder pb2 = new ProcessBuilder("mvn", "jetty:run");
                pb2.directory(server);
                Process p2 = pb2.start();
                BufferedReader errInput2 = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
                if ((error = errInput2.readLine()) == null) {
                    BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
                    while ((s = stdInput2.readLine()) != null) {
                        System.out.println(s);
                    }
                }
                else {
                    System.out.println(error);
                    while ((error = errInput2.readLine()) != null) {
                        System.out.println(error);
                    }
                    throw new MojoExecutionException("There are error during starting server");
                }
            }
            else {
                System.out.println(error);
                while ((error = errInput.readLine()) != null) {
                    System.out.println(error);
                }
                throw new MojoExecutionException("There are error during installing server");
            }

        } catch (Exception e) {
            getLog().error(e.getMessage());
        }
    }
}
