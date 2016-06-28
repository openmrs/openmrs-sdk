package org.openmrs.maven.plugins.bintray;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.List;

public class OpenmrsBintray {
    private static final String BINTRAY_OPENMRS_USER = "openmrs";
    private static final String BINTRAY_OWA_REPO = "owa";
    private static final String OPENMRS_OWA_PREFIX = "openmrs-owa-";

    private static final String OWA_PACKAGE_EXTENSION = ".zip";


    public List<BintrayId> getAvailableOWA() throws MojoExecutionException {
        return Bintray.getAvailablePackages(BINTRAY_OPENMRS_USER, BINTRAY_OWA_REPO);
    }

    public BintrayPackage getOwaMetadata(String name) throws MojoExecutionException {
        return Bintray.getPackageMetadata(BINTRAY_OPENMRS_USER, BINTRAY_OWA_REPO, name);
    }
    public void downloadOWA(File destination, String name, String version) {
        if(!destination.exists()){
            destination.mkdir();
        }
        List<BintrayFile> bintrayFiles = Bintray.getPackageFiles(BINTRAY_OPENMRS_USER, BINTRAY_OWA_REPO, name, version);
        //Assumption: owa release is single zip file
        String packageName = parseOwaNameFromFile(bintrayFiles.get(0).getPath())+ OWA_PACKAGE_EXTENSION;
        File downloadedFile = Bintray.downloadFile(bintrayFiles.get(0), destination, packageName);
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
}
