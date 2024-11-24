package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "exclusion", requiresProject = false)
public class AddExclusion extends AbstractTask {

    public static final String WARNING_PROPERTY_NOT_IN_PARENT = "The property is not included in the parent distro";
    public static final String WARNING_NO_PARENT_DISTRO = "This distro properties file does not contain a valid parent distro";

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

        Distribution distribution = distroHelper.resolveDistributionForStringSpecifier(distro, versionsHelper);
        if (distribution == null) {
            throw new MojoFailureException("Invalid distro file");
        }

        if (distribution.getParent() != null) {
            DistroProperties parentProperties = distribution.getParent().getEffectiveProperties();
            if (StringUtils.isBlank(property)) {
                List<String> currentExclusions = distribution.getProperties().getExclusions();
                List<String> options = parentProperties.getPropertyNames().stream().filter(prop -> !currentExclusions.contains(prop)).collect(Collectors.toList());
                property = wizard.promptForMissingValueWithOptions("Enter the property you want to exclude", null, null, options);
            }
            else {
                if (!parentProperties.getPropertyNames().contains(property)) {
                    wizard.showWarning(WARNING_PROPERTY_NOT_IN_PARENT);
                }
            }
        }
        else {
            wizard.showWarning(WARNING_NO_PARENT_DISTRO);
            if (StringUtils.isBlank(property)) {
                property = wizard.promptForValueIfMissing(null, "property to exclude");
                if (StringUtils.isBlank(property)) {
                    throw new MojoFailureException("Invalid property name");
                }
            }
        }
        distribution.getProperties().addExclusion(property);
        wizard.showMessage(property + " is added to exclusions");
        distribution.getProperties().saveTo(new File(distro).getParentFile());
    }

}
