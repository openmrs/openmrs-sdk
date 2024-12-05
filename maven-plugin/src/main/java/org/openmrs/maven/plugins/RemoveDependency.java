package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.util.ArrayList;

@Mojo(name = "remove", requiresProject = false)
public class RemoveDependency extends AbstractTask {

    /**
     * Path to the openmrs-distro.properties file to modify
     */
    @Parameter(property = "distro")
    private String distro;

    /**
     * Name of the property you want to remove
     */
    @Parameter(property = "property")
    private String property;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        if (distro == null) {
            File userDir = new File(System.getProperty("user.dir"));
            File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);
            if (distroFile.exists()) {
                distro = distroFile.getAbsolutePath();
            } else {
                throw new MojoFailureException("Please specify a distro file");
            }
        }

        DistroProperties properties = null;

        if (StringUtils.isNotBlank(distro)) {
            properties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);

        }

        if (properties == null) {
            throw new MojoFailureException("Invalid distro file");
        }

        if (StringUtils.isBlank(property)) {
            property = wizard.promptForMissingValueWithOptions("Enter the property you want to remove",
                    null, null, new ArrayList<>(properties.getAllKeys()));
        }

        if (StringUtils.isNotBlank(property)) {
            properties.removeProperty(property);
            properties.saveTo(new File(distro).getParentFile());
        }
    }
}
