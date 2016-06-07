package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

/**
 *
 */
public class Upgrader{
    private static final String OLD_PROPERTIES_FILENAME = "old.properties";

    private AbstractTask parentTask;

    public Upgrader(AbstractTask parentTask) {
        this.parentTask = parentTask;
    }

    /**
     * Upgrades platform of given server
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void upgradePlatform(Server server, String version) throws MojoExecutionException, MojoFailureException {
        saveOldProperties(server);
        //upgrade distro's war
        if (server.getVersion() != null) {
            confirmUpgrade(server.getVersion(), version);
            replaceWebapp(server, Server.PROPERTY_VERSION, version);
        } else {
            confirmUpgrade(server.getPlatformVersion(), version);
            replaceWebapp(server, Server.PROPERTY_PLATFORM, version);
        }
        parentTask.getLog().info(String.format("Server %s has been successfully upgraded to %s", server.getServerId(), version));
    }

    public void upgradeDistro(Server server, String version) throws MojoExecutionException, MojoFailureException {

    }

    private void saveOldProperties(Server server) throws MojoExecutionException {
        File oldProperties = new File(server.getServerDirectory(), OLD_PROPERTIES_FILENAME);
        server.saveTo(oldProperties);
    }
    private void deleteOldProperties(Server server) throws MojoExecutionException {
        File oldProperties = new File(server.getServerDirectory(), OLD_PROPERTIES_FILENAME);
        oldProperties.delete();
    }

    private void confirmUpgrade(String prevVersion, String nextVersion) throws MojoExecutionException {
        Version prev = new Version(prevVersion);
        Version next = new Version(nextVersion);
        if (prev.higher(next)){
            boolean confirmed =parentTask.wizard.promptYesNo("Note that downgrades are generally not supported by OpenMRS. " +
                    "Please consider setting up a new server with the given version instead.\n" +
                    "Are you sure you would like to downgrade?");
            if(!confirmed) throw new MojoExecutionException("Installation has been aborted");
        }
    }

    /**
     *
     * deletes old webapp war and installs new with version stated in 'version' field
     *
     * @param server
     * @param versionParam key used to obtain and replace old version from server
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void replaceWebapp(Server server, String versionParam, String version) throws MojoFailureException, MojoExecutionException {
        File webapp = new File(server.getServerDirectory(), "openmrs-"+server.getParam(versionParam)+".war");
        webapp.delete();
        Artifact webappArtifact = new Artifact(SDKConstants.WEBAPP_ARTIFACT_ID, version, Artifact.GROUP_WEB, Artifact.TYPE_WAR);
        parentTask.moduleInstaller.installModule(webappArtifact, server.getServerDirectory().getPath());
        server.setParam(versionParam, version);
        server.save();
        deleteOldProperties(server);
    }
}
