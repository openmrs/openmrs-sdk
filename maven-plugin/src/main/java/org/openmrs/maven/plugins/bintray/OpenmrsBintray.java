package org.openmrs.maven.plugins.bintray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.openmrs.maven.plugins.model.PackageJson;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DefaultJira;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OpenmrsBintray extends Bintray{
    public static final String OPENMRS_USERNAME = "openmrs";
    public static final String BINTRAY_OWA_REPO = "owa";
    public static final String OPENMRS_MAVEN_REPO = "maven";
    public static final String OPENMRS_OMOD_REPO = "omod";
    public static final String OPENMRS_OWA_PREFIX = "openmrs-owa-";
    public static final String OPENMRS_MODULE_PREFIX = "openmrs-module-";
    public static final String OWA_PACKAGE_EXTENSION = ".owa";

    public OpenmrsBintray(Proxy proxy) {
        super(proxy);
    }

    public OpenmrsBintray(Proxy proxy,String username, String password) {
        super(proxy,username, password);
    }

    public List<BintrayId> getAvailableOWA() throws MojoExecutionException {
        return getAvailablePackages(OPENMRS_USERNAME, BINTRAY_OWA_REPO);
    }

    public BintrayPackage getOwaMetadata(String name) throws MojoExecutionException {
        return getPackageMetadata(OPENMRS_USERNAME, BINTRAY_OWA_REPO, name);
    }

    public File downloadOWA(File destination, String name, String version) {
        if(!destination.exists()){
            destination.mkdir();
        }
        List<BintrayFile> bintrayFiles = getPackageFiles(OPENMRS_USERNAME, BINTRAY_OWA_REPO, name, version);

        String filename = bintrayFiles.get(0).getName();
        int versionPart = filename.lastIndexOf("-" + version);
        if (versionPart > 0) {
            filename = filename.substring(0, versionPart);
            filename = filename + OWA_PACKAGE_EXTENSION;
        } else if (filename.endsWith(".zip")) {
            filename = filename.substring(0, filename.length() - ".zip".length());
            filename = filename + OWA_PACKAGE_EXTENSION;
        }

        File file = downloadFile(bintrayFiles.get(0), destination, filename);

        return file;
    }

    public void downloadAndExtractOWA(File destination, String name, String version) {
        File downloadedFile = downloadOWA(destination, name, version);
        extractOwa(downloadedFile);
        downloadedFile.delete();
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
            String owaName = owaZip.getName();
            owaName = owaName.substring(0, owaName.length() - OWA_PACKAGE_EXTENSION.length());
            File outputDir = new File(owaZip.getParentFile(), owaName);
            outputDir.mkdirs();
            zipFile.extractAll(outputDir.getPath());
        } catch (ZipException e) {
            throw new RuntimeException(e);
        }
    }

    public BintrayPackage getPackageMetadata(String repository, String name) {
        return getPackageMetadata(OPENMRS_USERNAME, repository, name);
    }

    public List<BintrayId> getMavenAvailablePackages(String repo) {
        return getAvailablePackages(OPENMRS_USERNAME, OPENMRS_MAVEN_REPO);
    }

    public BintrayPackage createPackage(MavenProject mavenProject, String repository){
        CreatePackageRequest request = new CreatePackageRequest();
        request.setName(mavenProject.getArtifactId());
        request.setDescription(mavenProject.getDescription());

        String githubUrl = mavenProject.getScm().getUrl();
        if(githubUrl.endsWith("/")) githubUrl = StringUtils.stripEnd(githubUrl, "/");
        String githubRepo = githubUrl.substring(githubUrl.lastIndexOf("/")).replace(".git", "");
        request.setVcsUrl(githubUrl + (githubUrl.endsWith(".git") ? "" : ".git"));
        request.setGithubRepo(OPENMRS_USERNAME+githubRepo);
        request.setWebsiteUrl("http://openmrs.org/");

        request.setIssueTrackerUrl(new DefaultJira().getJiraUrl());
        //so far all OpenMRS projects have MPL-2.0 license
        request.setLicenses(Arrays.asList("MPL-2.0"));
        return createPackage(OPENMRS_USERNAME, repository, request);
    }

    /**
     * creates new version of omod package and uploads the file
     */
    public void uploadOmod(BintrayPackage bintrayPackage, String targetPath, File omodFile, String version){
        uploadFile(bintrayPackage, targetPath, version, omodFile);
    }

    public void publishOpenmrsPackageVersion(String repository, String packageName, String versionName){
        publishVersion(OPENMRS_USERNAME, repository, packageName, versionName);
    }
}
