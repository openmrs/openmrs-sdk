package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "exclusion", requiresProject = false)
public class AddExclusion extends AbstractTask {

    /**
     * Path to the openmrs-distro.properties file to modify
     */
    @Parameter(property = "distro")
    private String distro;

    /**
     * Name of the property you want to exclude
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

        DistroProperties originalDistroProperties = null;

        if (StringUtils.isNotBlank(distro)) {
            originalDistroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);

        }

        if (originalDistroProperties == null) {
            throw new MojoFailureException("Invalid distro file");
        }

        Artifact distroArtifact = originalDistroProperties.getDistroArtifact();
        if (distroArtifact != null) {
            DistroProperties distroProperties = distroHelper.resolveParentArtifact(distroArtifact, new File(distro).getParentFile(), originalDistroProperties, null);
            if (StringUtils.isBlank(property)) {
                List<String> currentExclusions = distroProperties.getExclusions();
                List<String> options = distroProperties.getPropertyNames().stream().filter(prop -> !currentExclusions.contains(prop)).collect(Collectors.toList());
                property = wizard.promptForMissingValueWithOptions("Enter the property you want to exclude",
                        null, null, options);
            } else {
                if (!distroProperties.getPropertyNames().contains(property)) {
                    wizard.showWarning("The property is not included in the parent distro");
                }
            }

        } else {
            wizard.showWarning("This distro properties file does not contain a valid parent distro");
            if (StringUtils.isBlank(property)) {
                property = wizard.promptForValueIfMissing(null, "property to exclude");
                if (StringUtils.isBlank(property)) {
                    throw new MojoFailureException("Invalid property name");
                }
            }
        }
        originalDistroProperties.addExclusion(property);
        wizard.showMessage(property + " is added to exclusions");
        originalDistroProperties.saveTo(new File(distro).getParentFile());
    }

}
