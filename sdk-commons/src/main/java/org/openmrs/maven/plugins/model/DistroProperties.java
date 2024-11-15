package org.openmrs.maven.plugins.model;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromFile;
import static org.openmrs.maven.plugins.utility.PropertiesUtils.loadPropertiesFromResource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class DistroProperties extends BaseSdkProperties {

    public static final String PROPERTY_PROMPT_KEY = "property.%s.prompt";
    private static final String DEAFAULT_FILE_NAME = "openmrs-distro-%s.properties";
    public static final String DISTRO_FILE_NAME = "openmrs-distro.properties";
    private static final String DB_SQL = "db.sql";
    public static final String PROPERTY_DEFAULT_VALUE_KEY = "property.%s.default";
    public static final String PROPERTY_KEY = "property.%s";

	private static final Logger log = LoggerFactory.getLogger(DistroProperties.class);

    public DistroProperties(String version){
        properties = new Properties();
        try {
            loadPropertiesFromResource(createFileName(version), properties);
        } catch (MojoExecutionException e) {
	        log.error(e.getMessage(), e);
        }
    }

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

    private String createFileName(String version){
        return String.format(DEAFAULT_FILE_NAME, version);
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
        for(Object key: getAllKeys()){
            if(key.toString().startsWith("property.")){
                propertiesNames.add(extractPropertyName(key.toString()));
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
        for (Object keyObject: getAllKeys()) {
            String key = keyObject.toString();
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

    public List<Artifact> getModuleArtifacts(DistroHelper distroHelper, File directory) throws MojoExecutionException {
        List<Artifact> childArtifacts = getModuleArtifacts();
        List<Artifact> parentArtifacts = new ArrayList<>();

        Artifact artifact = getParentDistroArtifact();
        if (artifact != null) {
            DistroProperties distroProperties = distroHelper.downloadDistroProperties(directory, artifact);
            parentArtifacts.addAll(distroProperties.getModuleArtifacts(distroHelper, directory));
        }
        return mergeArtifactLists(childArtifacts, parentArtifacts);
    }

    public List<Artifact> getOwaArtifacts(DistroHelper distroHelper, File directory) throws MojoExecutionException {
        List<Artifact> childArtifacts = getOwaArtifacts();
        List<Artifact> parentArtifacts = new ArrayList<>();

        Artifact artifact = getParentDistroArtifact();
        if (artifact != null) {
            DistroProperties distroProperties = distroHelper.downloadDistroProperties(directory, artifact);
            parentArtifacts.addAll(distroProperties.getOwaArtifacts(distroHelper, directory));
        }
        return mergeArtifactLists(childArtifacts, parentArtifacts);
    }

    public Map<String, String> getSpaProperties(DistroHelper distroHelper, File directory) throws MojoExecutionException {
        Map<String, String> spaProperties = getSpaProperties();

        Artifact artifact = getParentDistroArtifact();
        if (artifact != null) {
            DistroProperties distroProperties = distroHelper.downloadDistroProperties(directory, artifact);
            spaProperties.putAll(distroProperties.getSpaProperties(distroHelper, directory));
        }
        return spaProperties;
    }

    public List<Artifact> getWarArtifacts(DistroHelper distroHelper, File directory) throws MojoExecutionException{
        List<Artifact> childArtifacts = getWarArtifacts();
        List<Artifact> parentArtifacts = new ArrayList<>();

        Artifact artifact = getParentDistroArtifact();
        if (artifact != null) {
            DistroProperties distroProperties = distroHelper.downloadDistroProperties(directory, artifact);
            parentArtifacts.addAll(distroProperties.getWarArtifacts(distroHelper, directory));
        }
        return mergeArtifactLists(childArtifacts, parentArtifacts);
    }

    public String getPlatformVersion(DistroHelper distroHelper, File directory) throws MojoExecutionException{
        Artifact artifact = getParentDistroArtifact();
        if (artifact != null) {
            DistroProperties distroProperties = distroHelper.downloadDistroProperties(directory, artifact);
            return distroProperties.getPlatformVersion(distroHelper, directory);
        }
        return getPlatformVersion();
    }

    private List<Artifact> mergeArtifactLists(List<Artifact> childArtifacts, List<Artifact> parentArtifacts) {
        List<Artifact> artifactList = new ArrayList<>(childArtifacts);
        for (Artifact parentArtifact : parentArtifacts) {
            boolean found = false;
            for (Artifact childArtifact : childArtifacts) {
                boolean isGroupIdMatch = childArtifact.getGroupId().equals(parentArtifact.getGroupId());
                boolean isArtifactIdMatch = childArtifact.getArtifactId().equals(parentArtifact.getArtifactId());
                boolean isTypeMatch = childArtifact.getType().equals(parentArtifact.getType());
                if (isGroupIdMatch && isArtifactIdMatch && isTypeMatch) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                artifactList.add(parentArtifact);
            }
        }
        return artifactList;
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

    public void resolvePlaceholders(Properties projectProperties) throws MojoExecutionException {
        for(Map.Entry<Object, Object> property: properties.entrySet()){
            if(hasPlaceholder(property.getValue())){
                try{
                    Object placeholderValue = projectProperties.get(getPlaceholderKey((String)property.getValue()));
                    if(placeholderValue == null){
                        throw new MojoExecutionException(
                                "Failed to resolve property placeholders in distro file, no property for key \"" +
                                        property.getKey() + "\"");
                    } else {
                        property.setValue(putInPlaceholder((String)property.getValue(), (String)placeholderValue));
                    }
                } catch (ClassCastException e){
                    throw new MojoExecutionException("Property with key \"" + property.getKey() + "\" and value \"" +
                            property.getValue() + "\" is not placeholder.");
                }
            }
        }
    }

    public Set<Object> getAllKeys() {
        return properties.keySet();
    }

    public List<String> getExclusions() {
        String exclusions = getParam("exclusions");
        if(exclusions == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(exclusions.split(","));
    }

    public Set<String> getPropertyNames() {
        return properties.stringPropertyNames();
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

    public void addProperty(String property, String value) throws MojoExecutionException {
        properties.put(property, value);
    }

    private String getPlaceholderKey(String string){
        int startIndex = string.indexOf("${")+2;
        int endIndex = string.indexOf("}", startIndex);
        return string.substring(startIndex, endIndex);
    }

    private String putInPlaceholder(String value, String placeholderValue) {
        return value.replace("${"+getPlaceholderKey(value)+"}", placeholderValue);
    }

    private boolean hasPlaceholder(Object object){
        String asString;
        try{
            asString = (String) object;
        } catch(ClassCastException e){
            return false;
        }
        int index = asString.indexOf("{");
        return index != -1 && asString.substring(index).contains("}");
    }

    public boolean contains(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * Adds a dependency with the specified version to the properties.
     *
     * @param dependency The name of the dependency.
     * @param version The version of the dependency.
     */
    public void add(String dependency, String version) {
        if (StringUtils.isBlank(dependency) || StringUtils.isBlank(version)) {
            log.error("Dependency name or version cannot be blank");
            return;
        }

        if (dependency.startsWith("omod.") || dependency.startsWith("owa.") || dependency.startsWith("war")
                || dependency.startsWith("spa.frontendModule")) {
            properties.setProperty(dependency, version);
            log.info("Added dependency: {} with version: {}", dependency, version);
        }
    }

    public String get(String contentDependencyKey) {
        return properties.getProperty(contentDependencyKey);
    }

    @Override
    public String checkIfOverwritten(String key, String param) {
        return super.checkIfOverwritten(key, param);
    }

}
