package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.NpmVersionHelper;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "add", requiresProject = false)
public class AddDependency extends AbstractTask {

    /**
     * Path to the openmrs-distro.properties file to modify
     */
    @Parameter(property = "distro")
    private String distro;

    /**
     * Type of the dependency to add (OMOD, SPA, OWA, WAR or Custom)
     */
    @Parameter(property = "type")
    private String type;

    /**
     * Maven group id of the dependency
     */
    @Parameter(property = "groupId")
    private String groupId;

    /**
     * Maven artifact id of the dependency
     */
    @Parameter(property = "artifactId")
    private String artifactId;

    /**
     * Version of the dependency
     */
    @Parameter(property = "version")
    private String version;

    /**
     * Name of the frontend module
     */
    @Parameter(property = "moduleName")
    private String moduleName;

    /**
     * Name of the custom property
     */
    @Parameter(property = "property")
    private String property;

    private static final String DEPENDENCY_TYPE_PROMPT = "Enter the type of dependency you need to add";

    private static final String OMOD_OPTION = "OMOD";
    private static final String SPA_OPTION = "SPA";
    private static final String OWA_OPTION = "OWA";
    private static final String WAR_OPTION = "WAR";
    private static final String CUSTOM_OPTION = "Custom";


    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        if (distro == null) {
            File userDir = new File(System.getProperty("user.dir"));
            File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);
            if (distroFile.exists()) {
                distro = distroFile.getAbsolutePath();
            }
        }

        if (StringUtils.isBlank(type) && StringUtils.isBlank(property)) {
            List<String> dependencyTypes = new ArrayList<>(Arrays.asList(OMOD_OPTION, SPA_OPTION, OWA_OPTION, WAR_OPTION, CUSTOM_OPTION));
            type = wizard.promptForMissingValueWithOptions(DEPENDENCY_TYPE_PROMPT, null, null, dependencyTypes);
        }

        if (StringUtils.isBlank(type) && StringUtils.isNotBlank(property)) {
            type = CUSTOM_OPTION;
        }

        DistroProperties distroProperties = null;

        if (StringUtils.isNotBlank(distro)) {
            distroProperties = distroHelper.resolveDistroPropertiesForStringSpecifier(distro, versionsHelper);
        }

        if (distroProperties == null) {
            throw new MojoFailureException("Invalid distro properties");
        }

        switch (type.toUpperCase()) {
            case OMOD_OPTION:
                groupId = wizard.promptForValueIfMissingWithDefault(null, groupId, "groupId", Artifact.GROUP_MODULE);
                artifactId = wizard.promptForValueIfMissing(artifactId, "artifactId");
                if (StringUtils.isBlank(version)) {
                    Artifact artifact = new Artifact(artifactId, "1.0", groupId);
                    List<String> versions = versionsHelper.getSuggestedVersions(artifact, 5);
                    version = wizard.promptForMissingValueWithOptions("Enter the version", null, null, versions,
                            "Please specify the module version", null);
                }
                distroProperties.addProperty("omod." + artifactId, version);
                break;
            case SPA_OPTION:
                moduleName = wizard.promptForValueIfMissing(moduleName, "frontend module name");
                if (StringUtils.isBlank(version)) {
                    List<String> versions =  new NpmVersionHelper().getPackageVersions(moduleName, 6);;
                    version = wizard.promptForMissingValueWithOptions("Enter the module version", null, null, versions,
                            "Please specify the SPA version", null);
                }
                distroProperties.addProperty("spa.frontendModules." + moduleName, version);
                break;
            case OWA_OPTION:
                artifactId = wizard.promptForValueIfMissing(artifactId, "OWA name");
                Artifact artifact = new Artifact(artifactId, "1.0", Artifact.GROUP_OWA);
                if (StringUtils.isBlank(version)) {
                    List<String> suggestedVersions = versionsHelper.getSuggestedVersions(artifact, 6);
                    version = wizard
                            .promptForMissingValueWithOptions("Which version would you like to deploy?%s", null, "", suggestedVersions,
                                    "Please specify OWA version", null);
                }
                distroProperties.addProperty("owa." + artifactId, version);
                break;
            case WAR_OPTION:
                if (StringUtils.isBlank(version)) {
                    version = wizard.promptForPlatformVersion(versionsHelper);
                }
                distroProperties.addProperty("war.openmrs", version);
                break;
            default:
                property = wizard.promptForValueIfMissing(property, "the property name (ex: omod.legacyui)");
                version = wizard.promptForValueIfMissing(version, "the property version");
                distroProperties.addProperty(property, version);
        }

        if (StringUtils.isBlank(distro)) {
            wizard.showMessage("No distro.properpties file is provided. Generating a new openmrs-distro.properties file.");
            distro = Paths.get(System.getProperty("user.dir"), DistroProperties.DISTRO_FILE_NAME).toString();
        }

        distroProperties.saveTo(new File(distro).getParentFile());
    }

}
