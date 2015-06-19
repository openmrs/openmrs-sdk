package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.OS;

import java.io.File;

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
        OS os = new OS();
        String task = String.format("cd %s && mvn clean install && cd server && mvn jetty:run && echo", serverPath.getPath());
        try {
            Process p = os.executeCommand(task);
            /*
            StringBuffer output = new StringBuffer();
            BufferedReader reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
            getLog().info(output.toString()); */
        } catch (Exception e) {
            getLog().error(e.getMessage());
        }
    }
}
