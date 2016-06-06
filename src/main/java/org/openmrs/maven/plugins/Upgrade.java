package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @goal upgrade
 * @requiresProject false
 *
 */
public class Upgrade extends AbstractTask {
	
	private static final String TEMPLATE_SUCCESS = "Server '%s' has been successfully upgraded to '%s'";

    /**
     * Server id (folder name)
     *
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * Platform version
     *
     * @parameter expression="${version}"
     */
    private String version;

    public void execute() throws MojoExecutionException, MojoFailureException {
        UpgradePlatform upgrader = new UpgradePlatform(this);
        upgrader.upgradeServer(serverId, version, false);
        getLog().info(String.format(TEMPLATE_SUCCESS, serverId, version));
    }


}
