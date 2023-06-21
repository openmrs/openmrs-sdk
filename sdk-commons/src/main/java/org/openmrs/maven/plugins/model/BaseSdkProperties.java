package org.openmrs.maven.plugins.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class BaseSdkProperties {

    protected static final String ARTIFACT_ID = "artifactId";
    protected static final String TYPE = "type";
    protected static final String GROUP_ID = "groupId";
    protected static final String TYPE_OMOD = "omod";
    protected static final String TYPE_WAR = "war";
    protected static final String TYPE_JAR = "jar";
    protected static final String NAME = "name";
    protected static final String VERSION = "version";
    protected static final String TYPE_DISTRO = "distro";
    protected static final String TYPE_OWA = "owa";
    protected static final String TYPE_SPA = "spa";
    protected static final String TYPE_CONFIG = "config";
    protected static final String TYPE_ZIP = "zip";

    protected Properties properties;

    public Properties getModuleAndWarProperties(List<Artifact> warArtifacts, List<Artifact> moduleArtifacts) {
        Properties properties = new Properties();
        warArtifacts.forEach(artifact ->  {

            stripArtifactId(artifact);

            if (!artifact.getType().equals(TYPE_WAR)) {
                properties.setProperty(TYPE_WAR + "." + artifact.getArtifactId() + "." + TYPE, artifact.getType());
            }

            if (!artifact.getGroupId().equals(Artifact.GROUP_WEB)) {
                properties.setProperty(TYPE_WAR + "." + artifact.getArtifactId() + "." + GROUP_ID, artifact.getGroupId());
            }

            properties.setProperty(TYPE_WAR + "." + artifact.getArtifactId(), artifact.getVersion());
        });

        moduleArtifacts.forEach(artifact -> {
            stripArtifactId(artifact);
            if (!artifact.getType().equals(TYPE_JAR)) {
                    properties.setProperty(TYPE_OMOD + "." + artifact.getArtifactId() + "." + TYPE, artifact.getType());
            }
            if (!artifact.getGroupId().equals(Artifact.GROUP_MODULE)) {
                properties.setProperty(TYPE_OMOD + "." + artifact.getArtifactId() + "." + GROUP_ID, artifact.getGroupId());
            }
            properties.setProperty(TYPE_OMOD + "." + artifact.getArtifactId(), artifact.getVersion());

        });

        return properties;
    }

    public String getPlatformVersion(){
        return getParam("war.openmrs");
    }

    public void setPlatformVersion(String version){
        properties.setProperty("war.openmrs", version);
    }

    public String getVersion(){
        return getParam("version");
    }

    public void setVersion(String version){
        properties.setProperty("version", version);
    }

    public String getName(){
        return getParam("name");
    }

    public void setName(String name){
        properties.setProperty("name", name);
    }

    public List<Artifact> getModuleArtifacts(){
        return getAllKeys().stream().map(Object::toString).filter(key -> getArtifactType(key).equals(TYPE_OMOD))
                .map(key -> new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE), "omod"))
                .collect(Collectors.toList());
    }

    public List<Artifact> getOwaArtifacts() {
        return getAllKeys().stream().map(Object::toString).filter(key -> key.startsWith(TYPE_OWA + "."))
                .map(key -> new Artifact(key.substring(TYPE_OWA.length() + 1), getParam(key), Artifact.GROUP_OWA, Artifact.TYPE_ZIP))
                .collect(Collectors.toList());
    }

    public Map<String, String> getSpaProperties() {
        return getAllKeys().stream().map(Object::toString).filter(key -> key.startsWith(TYPE_SPA + "."))
                .collect(Collectors.toMap(
                        key -> key.substring(TYPE_SPA.length() + 1),
                        this::getParam,
                        (oldValue, newValue) -> newValue,
                        HashMap::new
                ));
    }

    public List<Artifact> getWarArtifacts() {
        return getAllKeys().stream().map(Object::toString).filter(key -> getArtifactType(key).equals(TYPE_WAR))
                .map(key -> new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE)))
                .collect(Collectors.toList());
    }

	public List<Artifact> getConfigArtifacts() {
        return getAllKeys().stream().map(Object::toString).filter(key -> getArtifactType(key).equals(TYPE_CONFIG))
                .map(key -> new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE)))
                .collect(Collectors.toList());
	}

	protected Set<Object> getAllKeys() {
		return properties.keySet();
	}

    protected String getArtifactType(String key){
        String[] wordsArray = key.split("\\.");
        if(!(wordsArray[wordsArray.length-1].equals(TYPE) || wordsArray[wordsArray.length-1].equals(ARTIFACT_ID) || wordsArray[wordsArray.length-1].equals(GROUP_ID))){
            if(key.contains(".")){
                return key.substring(0, key.indexOf("."));
            }else {
                return "";
            }
        }else {
            return "";
        }
    }

    protected String checkIfOverwritten(String key, String param) {
        String newKey = key + "." + param;
        if (getParam(newKey) != null) {
            String setting = getParam(newKey);
            if (setting.equals("referenceapplication")) {
                setting = setting.concat("-");
                setting = setting.concat("package");
            }
            return setting;
        } else {
	        switch (param) {
		        case ARTIFACT_ID:
			        return extractArtifactId(key);
		        case GROUP_ID:
                    switch (getArtifactType(key)) {
                        case TYPE_WAR:  //for openmrs.war use org.openmrs.web groupId
                            return Artifact.GROUP_WEB;
                        case TYPE_OMOD:
                            return Artifact.GROUP_MODULE;
                        case TYPE_DISTRO:
	                    case TYPE_CONFIG:
                            return Artifact.GROUP_DISTRO;
                        default:
                            return "";
                    }

		        case TYPE:
                    switch (getArtifactType(key)) {
                        case TYPE_OMOD:
                        case TYPE_DISTRO:
                            return TYPE_JAR;
                        case TYPE_WAR:
                            return TYPE_WAR;
	                    case TYPE_CONFIG:
							return TYPE_ZIP;
                        default:
                            return "";
                    }
		        default:
			        return "";
	        }
        }
    }

    private String extractArtifactId(String key){
        String type = getArtifactType(key);
        StringBuilder stringBuilder = new StringBuilder(key.substring(key.indexOf(".") + 1));
        if(type.equals(TYPE_OMOD)) {
            stringBuilder.append("-");
            stringBuilder.append(type);
        } else if(type.equals(TYPE_WAR)){
            stringBuilder.append("-");
            stringBuilder.append("webapp");
        } // referenceapplication exclusive parser
        else if (key.equals("distro.referenceapplication")) {
            stringBuilder.append("-");
            stringBuilder.append("package");
        }

        return  stringBuilder.toString();
    }

    /**
     * get param from properties
     * @param key
     * @return
     */
    public String getParam(String key) {return properties.getProperty(key); }

    public Artifact getModuleArtifact(String artifactId){
        String key = TYPE_OMOD + "." + artifactId;
        if(StringUtils.isNotBlank(getParam(key))){
            return new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE));
        }
        return null;
    }

    public void setModuleProperties(Artifact newModule) {
        newModule = stripArtifactId(newModule);
        if(!newModule.getGroupId().equals(Artifact.GROUP_MODULE)){
            setCustomModuleGroupId(newModule);
        }
        if(!newModule.getType().equals(TYPE_JAR)){
            setCustomModuleType(newModule);
        }
        setModule(newModule);

    }

    public void removeModuleProperties(Artifact artifact) {
        artifact = stripArtifactId(artifact);
        String artifactId = artifact.getArtifactId();
        if (getModuleArtifact(artifactId) != null) {
            Properties newProperties = new Properties();
            newProperties.putAll(properties);
            properties.keySet().stream().map(Object::toString)
                    .filter(key -> key.equals(TYPE_OMOD + "." + artifactId) || key.equals(TYPE_OMOD + "." + artifactId + "." + TYPE) || key.equals(TYPE_OMOD + "." + artifactId + "." + GROUP_ID))
                    .forEach(newProperties::remove);
            properties = newProperties;
        }
    }

    /**
     * Removes `-omod` or `-webapp` suffix from artifact ID.
     *
     * @param artifact
     * @return The same artifact, mutated
     */
    private Artifact stripArtifactId(Artifact artifact) {
        String artifactId = artifact.getArtifactId();
        if (artifactId.endsWith("-omod") || artifactId.endsWith("-webapp")) {
            artifact.setArtifactId(artifactId.substring(0, artifactId.lastIndexOf("-")));
        }
        return artifact;
    }

    private void setModule(Artifact artifact) {
        properties.setProperty(TYPE_OMOD+"."+artifact.getArtifactId(), artifact.getVersion());
    }

    private void setCustomModuleType(Artifact artifact){
        properties.setProperty(TYPE_OMOD+"."+artifact.getArtifactId()+"."+TYPE, artifact.getType());
    }

    private void setCustomModuleGroupId(Artifact artifact){
        properties.setProperty(TYPE_OMOD+"."+artifact.getArtifactId()+"."+GROUP_ID, artifact.getGroupId());
    }

    public void synchronize(BaseSdkProperties other){
        getAllKeys().stream()
                .filter(key -> isBaseSdkProperty(key.toString()))
                .forEach(key -> other.properties.put(key, properties.get(key)));

        new ArrayList<>(other.getAllKeys()).stream()
                .filter(key -> isBaseSdkProperty(key.toString()))
                .filter(key -> StringUtils.isBlank(getParam(key.toString())))
                .forEach(key -> other.properties.remove(key));
    }

    private boolean isBaseSdkProperty(String key) {
        return  (key.startsWith(TYPE_OMOD) || key.startsWith(TYPE_WAR) || key.equals(NAME) || key.equals(VERSION));
    }


    public void setArtifacts(List<Artifact> warArtifacts, List<Artifact> moduleArtifacts) {
        moduleArtifacts.forEach(this::setModuleProperties);
        warArtifacts.forEach(warArtifact -> this.setPlatformVersion(warArtifact.getVersion()));
    }

}
