package org.openmrs.maven.plugins.model;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.PropertiesUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromFile;

public class DistroProperties extends BaseSdkProperties {

    public static final String PROPERTY_PROMPT_KEY = "property.%s.prompt";
    public static final String DISTRO_FILE_NAME = "openmrs-distro.properties";
    private static final String DB_SQL = "db.sql";
    public static final String PROPERTY_DEFAULT_VALUE_KEY = "property.%s.default";
    public static final String PROPERTY_KEY = "property.%s";

    public DistroProperties(String name, String platformVersion){
        properties = new Properties();
        setName(name);
        setVersion("1.0");  // it's unclear what this means or why it is necessary, but it is tested for
        setPlatformVersion(platformVersion);
    }

    public DistroProperties(Properties properties){
        this.properties = properties;
    }

    public DistroProperties(File file) throws MojoExecutionException{
        this.properties = new Properties();
        loadPropertiesFromFile(file, this.properties);
    }

    public boolean isH2Supported(){
        return Boolean.parseBoolean(getParam("db.h2.supported"));
    }

    public void setH2Support(boolean supported) {
        properties.setProperty("db.h2.supported", String.valueOf(supported));
    }

    public String getSqlScriptPath() {
        return getParam(DB_SQL);
    }

    public String getPropertyPrompt(String propertyName){
        return getParam(String.format(PROPERTY_PROMPT_KEY, propertyName));
    }

    public String getPropertyDefault(String propertyName){
        return getParam(String.format(PROPERTY_DEFAULT_VALUE_KEY, propertyName));
    }

    public String getPropertyValue(String propertyName){
        return getParam(String.format(PROPERTY_KEY, propertyName));
    }

    public Set<String> getPropertiesNames(){
        Set<String> propertiesNames = new HashSet<>();
        for(String key : getAllKeys()){
            if(key.startsWith("property.")){
                propertiesNames.add(extractPropertyName(key));
            }
        }
        return propertiesNames;
    }

    private String extractPropertyName(String key) {
        int beginIndex = key.indexOf(".");
        if(key.endsWith(".default")){
            return key.substring(beginIndex+1, key.length()-8);
        } else if(key.endsWith(".prompt")){
            return key.substring(beginIndex+1, key.length()-7);
        } else {
            return key.substring(beginIndex+1);
        }
    }

    /**
     * Allowed formats for artifactId and version
     * distro.artifactId=version
     * distro.artifactId.groupId=
     * distro.artifactId.type=
     * -OR-
     * distro.anything=version
     * distro.anything.artifactId=
     * distro.anything.groupId=
     * distro.anything.type=
     * -OR-
     * parent.artifactId=
     * parent.groupId=
     * parent.version=
     * parent.type=
     */
    public Artifact getParentDistroArtifact() throws MojoExecutionException {
        Artifact artifact = null;
        for (String key : getAllKeys()) {
            String artifactType = getArtifactType(key);
            if (artifactType.equals(TYPE_DISTRO)) {
                String artifactId = checkIfOverwritten(key, ARTIFACT_ID);
                String version = getParam(key);
                String groupId = checkIfOverwritten(key, GROUP_ID);
                String type = checkIfOverwritten(key, TYPE);
                if (artifact != null) {
                    throw new MojoExecutionException("Only a single " + TYPE_DISTRO + " property can be added to indicate the parent distribution");
                }
                artifact = new Artifact(artifactId, version, groupId, type);
            }
        }
        String artifactId = getParam(TYPE_PARENT + "." + ARTIFACT_ID);
        String version = getParam(TYPE_PARENT + "." + VERSION);
        String groupId = getParam(TYPE_PARENT + "." + GROUP_ID);
        String type = getParam(TYPE_PARENT + "." + TYPE, TYPE_ZIP);
        if (StringUtils.isNotBlank(artifactId)) {
            if (artifact != null) {
                throw new MojoExecutionException("Distro properties cannot define both a " + TYPE_DISTRO + " and a " + TYPE_PARENT + " property");
            }
            if (StringUtils.isBlank(version) || StringUtils.isBlank(groupId)) {
                throw new MojoExecutionException("You must specify a " + TYPE_PARENT + " groupId and version if you specify and artifactId");
            }
            artifact = new Artifact(artifactId, version, groupId, type);
        }
        return DistroHelper.normalizeArtifact(artifact, null);
    }

    public void saveTo(File path) throws MojoExecutionException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(path, DISTRO_FILE_NAME));
            SortedProperties sortedProperties = new SortedProperties();
            sortedProperties.putAll(properties);
            sortedProperties.store(out, null);
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(out);
        }
    }

    // This is no longer needed or used, keeping it here until unit test dependencies and coverage is resolved
    // It has been moved to PropertyUtils and changed to a new implementation.  It is applied in DistibutionBuilder.
    @Deprecated
    public void resolvePlaceholders(Properties projectProperties) throws MojoExecutionException {
        PropertiesUtils.resolvePlaceholders(properties, projectProperties);
    }

    public List<String> getExclusions() {
        String exclusions = getParam("exclusions");
        if(exclusions == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(exclusions.split(","));
    }

    public void removeProperty(String property) throws MojoExecutionException {
        if (!properties.containsKey(property)) {
            throw new MojoExecutionException("The property " + property + " was not found in the distro");
        }
        properties.remove(property);
    }

    public void addExclusion(String exclusion) {
        String exclusions = getParam("exclusions");
        if (exclusions == null){
            properties.setProperty("exclusions", exclusion);
            return;
        }

        properties.setProperty("exclusions", exclusions + "," + exclusion);
    }
}
