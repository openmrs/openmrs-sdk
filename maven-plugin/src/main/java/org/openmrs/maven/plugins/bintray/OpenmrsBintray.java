package org.openmrs.maven.plugins.bintray;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.utility.DefaultJira;
import org.openmrs.maven.plugins.utility.Jira;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OpenmrsBintray extends Bintray{
    public static final String OPENMRS_USERNAME = "openmrs";
    public static final String BINTRAY_OWA_REPO = "owa";
    public static final String OPENMRS_MAVEN_REPO = "maven";
    public static final String OPENMRS_OWA_PREFIX = "openmrs-owa-";

    private static final String OWA_PACKAGE_EXTENSION = ".zip";

    public OpenmrsBintray() {}

    public OpenmrsBintray(String username, String password) {
        super(username, password);
    }

    public List<BintrayId> getAvailableOWA() throws MojoExecutionException {
        return getAvailablePackages(OPENMRS_USERNAME, BINTRAY_OWA_REPO);
    }

    public BintrayPackage getOwaMetadata(String name) throws MojoExecutionException {
        return getPackageMetadata(OPENMRS_USERNAME, BINTRAY_OWA_REPO, name);
    }
    public void downloadOWA(File destination, String name, String version) {
        if(!destination.exists()){
            destination.mkdir();
        }
        List<BintrayFile> bintrayFiles = getPackageFiles(OPENMRS_USERNAME, BINTRAY_OWA_REPO, name, version);
        //Assumption: owa release is single zip file
        String packageName = parseOwaNameFromFile(bintrayFiles.get(0).getPath())+ OWA_PACKAGE_EXTENSION;
        File downloadedFile = downloadFile(bintrayFiles.get(0), destination, packageName);
        extractOwa(downloadedFile);
        downloadedFile.delete();
    }

    private String parseOwaNameFromFile(String name){
        return name.split("-")[0];
    }

    public static BintrayId parseOwa(String input){
        String[] tokens = input.split(":");
        if(tokens.length!=2){
            throw new RuntimeException("Illegal OWA identifier: "+input);
        }
        if(!tokens[0].startsWith(OPENMRS_OWA_PREFIX)){
            tokens[0] = OPENMRS_OWA_PREFIX+tokens[0];
        }
        return new BintrayId(tokens[0], tokens[1]);
    }

    public void extractOwa(File owaZip){
        try {
            ZipFile zipFile = new ZipFile(owaZip);
            File outputDir = new File(owaZip.getParentFile(), StringUtils.removeEnd(owaZip.getName(), OWA_PACKAGE_EXTENSION));
            zipFile.extractAll(outputDir.getPath());
        } catch (ZipException e) {
            throw new RuntimeException(e);
        }
    }

    public BintrayPackage getMavenPackageMetadata(String name) {
        return getPackageMetadata(OPENMRS_USERNAME, OPENMRS_MAVEN_REPO, name);
    }

    public List<BintrayId> getMavenAvailablePackages(String repo) {
        return getAvailablePackages(OPENMRS_USERNAME, OPENMRS_MAVEN_REPO);
    }

    public BintrayPackage createMavenPackage(MavenProject mavenProject){
        CreatePackageRequest request = new CreatePackageRequest();
        request.setName(mavenProject.getArtifactId());
        request.setDescription(mavenProject.getDescription());

        String githubUrl = mavenProject.getScm().getUrl();
        String githubRepo = githubUrl.substring(githubUrl.lastIndexOf("/"));
        request.setVcsUrl(githubUrl+".git");
        request.setGithubRepo(OPENMRS_USERNAME+githubRepo);
        request.setWebsiteUrl("http://openmrs.org/");

        request.setIssueTrackerUrl(new DefaultJira().getJiraUrl());
        //so far all OpenMRS projects have MPL-2.0 license
        //TODO: resolve it dynamically?
        request.setLicenses(Arrays.asList("MPL-2.0"));
        return createPackage(OPENMRS_USERNAME, OPENMRS_MAVEN_REPO, request);
    }
}
