package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.bintray.BintrayId;
import org.openmrs.maven.plugins.bintray.BintrayPackage;
import org.openmrs.maven.plugins.bintray.OpenmrsBintray;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @goal fetch
 * @requiresProject false
 */
public class Fetch extends AbstractTask {

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * @parameter expression="${owa}"
     */
    private String owa;

    /**
     * @parameter expression="${repository}"
     */
    private String repository;

    /**
     * @parameter expression="${dir}"
     */
    private String dir;

    // This enum is created to make future extensions development easier
    private enum ProjectType {
        OWA, MODULE
    }
    private ProjectType projectType;
    private String projectName;

    private File downloadDirectory;

    private OpenmrsBintray openmrsBintray;

    private static final String BINTRAY_URL = "https://bintray.com";
    private static final String REPOSITORY_OWNER = "openmrs";

    private static final String FETCH_MODULE_OPTION = "Module";
    private static final String FETCH_OWA_OPTION = "Open Web App";

    private static final String ERROR_INVALID_VERSION = "Invalid version number";
    private static final String ERROR_PACKAGE_NOT_EXIST = "There is no package with given name";


    public void executeTask() throws MojoExecutionException, MojoFailureException {
        openmrsBintray = new OpenmrsBintray();
        runInteractiveMode();
    }

    private void runInteractiveMode() throws MojoExecutionException, MojoFailureException {
        getData();
        fetch();
    }

    private void getData() {

        //Resolve project directory if undefined
        if (dir == null) {
            dir = System.getProperty("user.dir");
        }
        downloadDirectory = new File(dir);

        //Resolve project type and name if defined
        if (artifactId != null) {
            projectType = ProjectType.MODULE;
            projectName = adjustProjectNameIfPrefixMissing(artifactId, OpenmrsBintray.OPENMRS_MODULE_PREFIX);
        }
        else if (owa != null) {
            projectType = ProjectType.OWA;
            projectName = adjustProjectNameIfPrefixMissing(owa, OpenmrsBintray.OPENMRS_OWA_PREFIX);
        }

        //Resolve project type if undefined
        List<String> options = new ArrayList<>(Arrays.asList(
                FETCH_MODULE_OPTION,
                FETCH_OWA_OPTION
        ));
        if (projectType == null) {
            String choice = wizard.promptForMissingValueWithOptions("What would you like to fetch?%s", null, "", options);
            switch (choice) {
                case FETCH_MODULE_OPTION:
                    projectType = ProjectType.MODULE;
                    break;
                case FETCH_OWA_OPTION:
                    projectType = ProjectType.OWA;
                    break;
            }
        }

        //Resolve repository name if undefined
        if (repository == null) {
            switch (projectType) {
                case MODULE:
                    repository = OpenmrsBintray.OPENMRS_OMOD_REPO;
                    break;
                case OWA:
                    repository = OpenmrsBintray.BINTRAY_OWA_REPO;
                    break;
                }
        }

        //Resolve project name if undefined
        if (projectName == null) {
            List<String> projects = new ArrayList<>();
            for(BintrayId id : openmrsBintray.getAvailablePackages(REPOSITORY_OWNER, repository)){
                projects.add(id.getName());
            }
            projectName = wizard.promptForMissingValueWithOptions("Which project would you like to fetch?%s", projectName, "", projects, "Please specify project's id", null);
        }

        //Resolve version if undefined
        if (version == null) {
            //Check if there is version number in project name
            version = extractVersionFromProjectName(projectName);
        }
        if (version == null){
            BintrayPackage projectMetadata = openmrsBintray.getPackageMetadata(REPOSITORY_OWNER, repository, projectName);
            if(projectMetadata == null){
                throw new RuntimeException(ERROR_PACKAGE_NOT_EXIST);
            }
            List<String> versions = projectMetadata.getVersions();
            version = wizard.promptForMissingValueWithOptions("Which version would you like to fetch?%s", version, "", versions, "Please specify project's version", null);
        }
    }

    private void fetch() {
        wizard.showMessage("Downloading " + projectName + "from: "
                + BINTRAY_URL + "/"
                + REPOSITORY_OWNER + "/"
                + repository + "/"
                + projectName + "/"
                + version
        );
        openmrsBintray.downloadPackage(new File(dir), REPOSITORY_OWNER, repository, projectName, version);
        wizard.showMessage("Project " + projectName + ":" + version + " is downloaded to " + downloadDirectory.getAbsolutePath());
    }

    private String extractVersionFromProjectName(String name) {
        if (name.contains(":")) {
            String substringName = (name.split(":"))[0];
            String substringVersion = (name.split(":"))[1];
            if (!substringVersion.isEmpty()) {
                projectName = substringName;
                return substringVersion;
            }
            else {
                throw new IllegalArgumentException(ERROR_INVALID_VERSION);
            }
        }
        return null;
    }

    private String adjustProjectNameIfPrefixMissing(String projectName, String prefix) {
        if (!projectName.contains(prefix)) {
            return prefix + projectName;
        }
        else {
            return projectName;
        }
    }

}
