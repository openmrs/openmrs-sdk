package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;
import org.openmrs.maven.plugins.model.Version;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public class DistroHelper {
    /**
     * The project currently being build.
     */
    MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    BuildPluginManager pluginManager;

    /**
     *
     */
    Wizard wizard;

    public DistroHelper(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager, Wizard wizard) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.wizard = wizard;
    }

    /**
     * @return distro properties from openmrs-distro.properties file in current directory or null if not exist
     */
    public static DistroProperties getDistroPropertiesFromDir() {
        File distroFile = new File(new File(System.getProperty("user.dir")), "openmrs-distro.properties");
        return getDistroPropertiesFromFile(distroFile);
    }

    /**
     * @param distroFile file which contains distro properties
     * @return distro properties loaded from specified file or null if file is not distro properties
     */
    public static DistroProperties getDistroPropertiesFromFile(File distroFile) {
        if(distroFile.exists()){
            try {
                return new DistroProperties(distroFile);
            } catch (MojoExecutionException e) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Saves all custom properties from distroProperties starting with "property." to server
     * @param properties
     * @param server
     */
    public void savePropertiesToServer(DistroProperties properties, Server server) {
        if (properties != null) {
            Properties userBashProperties = mavenSession.getRequest().getUserProperties();
            Set<String> propertiesNames = properties.getPropertiesNames();
            for(String propertyName: propertiesNames){
                String propertyValue = properties.getPropertyValue(propertyName);
                String propertyValueBash = userBashProperties.getProperty(propertyName);
                String propertyPrompt = properties.getPropertyPromt(propertyName);
                String propertyDefault = properties.getPropertyDefault(propertyName);
                if(propertyValueBash != null){
                    server.setPropertyValue(propertyName, propertyValueBash);
                } else if(propertyValue != null){
                    server.setPropertyValue(propertyName, propertyValue);
                } else {
                    if(propertyPrompt != null){
                        if(propertyDefault != null){
                            propertyValue = wizard.promptForValueIfMissingWithDefault(propertyPrompt, null, propertyName, propertyDefault);
                            server.setPropertyValue(propertyName, propertyValue);
                        } else {
                            propertyValue = wizard.promptForValueIfMissingWithDefault(propertyPrompt, null, propertyName, null);
                            server.setPropertyValue(propertyName, propertyValue);
                        }
                    }

                }
            }
        }
    }


    /**
     * valid formats are 'groupId:artifactId:version' and 'artifactId:version'
     * parser makes user-friendly assumptions, like inferring default groupId or full artifactId for referenceapplication
     * returns null if string is invalid
     */
    public static Artifact parseDistroArtifact(String distro) throws MojoExecutionException {
        String[] split = distro.split(":");
        if(split.length == 2){
            return new Artifact(inferDistroArtifactId(split[0], Artifact.GROUP_DISTRO), split[1], Artifact.GROUP_DISTRO);
        } else if (split.length == 3){
            Artifact artifact = new Artifact(inferDistroArtifactId(split[1], split[0]), split[2], split[0]);
            if(artifact.getGroupId().equals(Artifact.GROUP_MODULE)){
                artifact.setArtifactId(artifact.getArtifactId()+"-omod");
            }
            return artifact;
        } else {
            throw new MojoExecutionException("Invalid distro: "+distro);
        }
    }

    private static String inferDistroArtifactId(String artifactId, String groupId){
        if(Artifact.GROUP_DISTRO.equals(groupId)&&"referenceapplication".equals(artifactId)){
            return SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID;
        }
        else return artifactId;
    }
    /**
     * openmrs-sdk has hardcoded distro properties for certain versions of refapp which don't include them
     * @return
     */
    public static boolean isRefapp2_3_1orLower(String artifactId, String version){
        if(artifactId!=null&&artifactId.equals(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID)){
            return SDKConstants.SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER.contains(version);
        } else return false;
    }

    public static boolean isRefapp2_3_1orLower(Artifact artifact){
        return isRefapp2_3_1orLower(artifact.getArtifactId(), artifact.getVersion());
    }

    public static boolean isRefappBelow2_1(String artifactId, String version){
        if(artifactId!=null&&artifactId.equals(SDKConstants.REFERENCEAPPLICATION_ARTIFACT_ID)){
            return new Version(version).lower(new Version("2.1"));
        } else return false;
    }

    public static boolean isRefappBelow2_1(Artifact artifact){
        return isRefappBelow2_1(artifact.getArtifactId(), artifact.getVersion());
    }

    public File downloadDistro(File path, Artifact artifact) throws MojoExecutionException {
        artifact.setDestFileName("openmrs-distro.jar");
        List<MojoExecutor.Element> artifactItems = new ArrayList<MojoExecutor.Element>();
        MojoExecutor.Element element = artifact.toElement(path.toString());
        artifactItems.add(element);

        executeMojo(
                plugin(
                        groupId(SDKConstants.PLUGIN_DEPENDENCIES_GROUP_ID),
                        artifactId(SDKConstants.PLUGIN_DEPENDENCIES_ARTIFACT_ID),
                        version(SDKConstants.PLUGIN_DEPENDENCIES_VERSION)
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"), artifactItems.toArray(new Element[0]))
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
        return new File(path, artifact.getDestFileName());
    }

    public File extractFileFromDistro(File path, Artifact artifact, String filename) throws MojoExecutionException {
        File distroFile = downloadDistro(path, artifact);
        File resultFile = new File(path, filename);
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(distroFile);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()){
                ZipEntry zipEntry = entries.nextElement();
                if(zipEntry.getName().equals(filename)){
                    FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), resultFile);
                }
            }
            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + distroFile.toString(), e);
        } finally {
            IOUtils.closeQuietly(zipFile);
            distroFile.delete();
        }
        return resultFile;
    }

    public DistroProperties downloadDistroProperties(File path, Artifact artifact) throws MojoExecutionException {
        File file = downloadDistro(path, artifact);

        DistroProperties distroProperties = null;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()){
                ZipEntry zipEntry = entries.nextElement();
                if("openmrs-distro.properties".equals(zipEntry.getName())){
                    Properties properties = new Properties();
                    properties.load(zipFile.getInputStream(zipEntry));
                    distroProperties = new DistroProperties(properties);
                }
            }

            zipFile.close();


        } catch (IOException e) {
            throw new RuntimeException("Could not read " + file.toString(), e);
        } finally {
            IOUtils.closeQuietly(zipFile);
            file.delete();
        }

        return distroProperties;
    }

    public DistroProperties downloadDistroProperties(File serverPath, Server server) throws MojoExecutionException {
        Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
        if (StringUtils.isNotBlank(artifact.getArtifactId())) {
            return downloadDistroProperties(serverPath, artifact);
        } else {
            return null;
        }
    }

    /**
     * Distro can be passed in two ways: either as maven artifact identifier or path to distro file
     * Returns null if string is invalid as path or identifier
     * @param distro
     * @return
     */
    public DistroProperties retrieveDistroProperties(String distro) throws MojoExecutionException {
        DistroProperties result;
        result = getDistroPropertiesFromFile(new File(distro));
        if(result != null && mavenProject != null){
            result.resolvePlaceholders(getProjectProperties());
        } else {
            Artifact artifact = parseDistroArtifact(distro);
            if(isRefapp2_3_1orLower(artifact)){
                result = new DistroProperties(artifact.getVersion());
            } else if (isRefappBelow2_1(artifact)) {
                throw new MojoExecutionException("Reference Application versions below 2.1 are not supported!");
            } else {
                result = downloadDistroProperties(new File(System.getProperty("user.dir")), artifact);
            }
        }
        return result;
    }

    private Properties getProjectProperties() {
        Properties properties = mavenProject.getProperties();
        properties.setProperty("project.parent.version", mavenProject.getVersion());
        properties.setProperty("project.version", mavenProject.getVersion());
        return properties;
    }

    /**
     * resolves distro based on passed artifact and saves distro.properties file in destination
     */
    public void saveDistroPropertiesTo(File destination, String distro) throws MojoExecutionException {
        DistroProperties distroProperties = retrieveDistroProperties(distro);
        if(distroProperties != null){
            distroProperties.saveTo(destination);
        }
    }

    /**
     * should:
     * - ignore modules which are already on server, but not included in distro properties of upgrade
     * - keep new platform artifact if distro properties declares newer version
     * - updateMap include modules which are already on server with newer/equal SNAPSHOT version
     * - add modules which are not installed on server yet
     */
    public static UpgradeDifferential calculateUpdateDifferential(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        List<Artifact> newList = new ArrayList<>(distroProperties.getWarArtifacts());
        newList.addAll(distroProperties.getModuleArtifacts());
        return calculateUpdateDifferential(server.getServerModules(), newList);
    }

    static UpgradeDifferential calculateUpdateDifferential(List<Artifact> oldList, List<Artifact> newList){
        UpgradeDifferential upgradeDifferential = new UpgradeDifferential();
        for(Artifact newListModule: newList){
            boolean toAdd = true;
            for(Artifact oldListModule : oldList){
                if(isSameArtifact(oldListModule, newListModule)){
                    if(isHigherVersion(oldListModule, newListModule)){
                        if(isOpenmrsWebapp(newListModule)){
                            upgradeDifferential.setPlatformArtifact(newListModule);
                            upgradeDifferential.setPlatformUpgraded(true);
                        } else {
                            upgradeDifferential.putUpdateEntry(oldListModule, newListModule);
                        }
                    } else if(isLowerVersion(oldListModule, newListModule)){
                        if(isOpenmrsWebapp(newListModule)){
                            upgradeDifferential.setPlatformArtifact(newListModule);
                            upgradeDifferential.setPlatformUpgraded(false);
                        } else {
                            upgradeDifferential.putDowngradeEntry(oldListModule, newListModule);
                        }
                    }
                    toAdd = false;
                    break;
                }
            }
            if(toAdd){
                upgradeDifferential.addModuleToAdd(newListModule);
            }
        }
        for(Artifact oldListModule: oldList){
            boolean moduleNotFound = true;
            for(Artifact newListModule: newList){
                if(isSameArtifact(newListModule, oldListModule)){
                    moduleNotFound = false;
                    break;
                }
            }
            if(moduleNotFound){
                if (isOpenmrsWebapp(oldListModule)) {
                    throw new IllegalStateException("You can delete only modules. Deleting openmrs core is not available");
                } else {
                    upgradeDifferential.addModuleToDelete(oldListModule);
                }
            }
        }
        return upgradeDifferential;
    }

    private static boolean isOpenmrsWebapp(Artifact artifact) {
        return Artifact.TYPE_WAR.equals(artifact.getType())&&SDKConstants.WEBAPP_ARTIFACT_ID.equals(artifact.getArtifactId());
    }

    private static boolean isSameArtifact(Artifact left, Artifact right){
        return getId(left.getDestFileName()).equals(getId(right.getDestFileName()));
    }

    private static String getId(String name) {
        int index = name.indexOf('-');
        if (index == -1) return name;
        return name.substring(0, index);
    }

    /**
     * checks if next artifact is higher version of the same artifact
     * returns true for equal version snapshots
     */
    private static boolean isHigherVersion(Artifact previous, Artifact next){
        if (!validateArtifactsToCompare(previous, next)) return false;

        Version previousVersion = new Version(previous.getVersion());
        Version nextVersion = new Version(next.getVersion());

        if(nextVersion.higher(previousVersion)){
            return true;
        } else if(nextVersion.equal(previousVersion)){
            return(previousVersion.isSnapshot()&&nextVersion.isSnapshot());
        } else {
            return false;
        }
    }
    private static boolean isLowerVersion(Artifact previous, Artifact next) {
        if (!validateArtifactsToCompare(previous, next)) return false;

        Version previousVersion = new Version(previous.getVersion());
        Version nextVersion = new Version(next.getVersion());

        if(nextVersion.lower(previousVersion)){
            return true;
        } else if(nextVersion.equal(previousVersion)){
            return(previousVersion.isSnapshot()&&nextVersion.isSnapshot());
        } else {
            return false;
        }
    }

    private static boolean validateArtifactsToCompare(Artifact previous, Artifact next) {
        if(previous==null||next==null
                ||previous.getArtifactId()==null||next.getArtifactId()==null
                ||previous.getVersion()==null||next.getVersion()==null
                ||!isSameArtifact(previous, next)){
            return false;
        }
        return true;
    }

}
