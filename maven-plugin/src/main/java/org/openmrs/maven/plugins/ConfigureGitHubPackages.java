package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;

/**
 * Configures GitHub Packages authentication and repositories based on distro.properties
 * This plugin should run early in the lifecycle to ensure GitHub Packages are configured
 * before any dependencies are resolved.
 */
@Mojo(name = "configure-github-packages", defaultPhase = LifecyclePhase.INITIALIZE)
public class ConfigureGitHubPackages extends AbstractTask {

    /**
     * Path to the openmrs-distro.properties file
     */
    @Parameter(property = "distro")
    private String distro;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        if (distro == null) {
            File userDir = new File(System.getProperty("user.dir"));
            File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);
            if (distroFile.exists()) {
                distro = distroFile.getAbsolutePath();
            } else {
                throw new MojoFailureException("Please specify a distro.properties file");
            }
        }

        DistroProperties distroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
        if (distroProperties == null) {
            throw new MojoFailureException("Invalid distro properties");
        }

        getMavenEnvironment().gitHubPackagesHelper().configureGitHubPackages(distroProperties);
    }
}
