package org.openmrs.maven.plugins.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

    protected Properties properties;

    public Properties getModuleProperties(){
        Properties moduleProperties = new Properties();
        for(Object keyObject: getAllKeys()){
            String key = keyObject.toString();
            if(key.startsWith(TYPE_OMOD) || key.startsWith(TYPE_WAR)){
                moduleProperties.put(key, getParam(key));
            }
        }
        return moduleProperties;
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
        List<Artifact> artifactList = new ArrayList<>();
        for (Object keyObject: getAllKeys()) {
            String key = keyObject.toString();
            String artifactType = getArtifactType(key);
            if(artifactType.equals(TYPE_OMOD)) {
                artifactList.add(new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE), "omod"));
            }
        }
        return  artifactList;
    }

    public List<Artifact> getWarArtifacts(){
        List<Artifact> artifactList = new ArrayList<>();
        for (Object keyObject: getAllKeys()) {
            String key = keyObject.toString();
            String artifactType = getArtifactType(key);
            if(artifactType.equals(TYPE_WAR)) {
                artifactList.add(new Artifact(checkIfOverwritten(key, ARTIFACT_ID), getParam(key), checkIfOverwritten(key, GROUP_ID), checkIfOverwritten(key, TYPE)));
            }
        }
        return  artifactList;
    }

    protected Set<Object> getAllKeys(){
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

    private String checkIfOverwritten(String key, String param) {
        String newKey = key + "." + param;
        if (getParam(newKey) != null) {
            return getParam(newKey);
        } else {
            if (param.equals(ARTIFACT_ID)) {
                return extractArtifactId(key);
            } else if (param.equals(GROUP_ID)) {
                if (getArtifactType(key).equals(TYPE_WAR)) { //for openmrs.war use org.openmrs.web groupId
                    return Artifact.GROUP_WEB;
                } else if(getArtifactType(key).equals(TYPE_OMOD)){
                    return Artifact.GROUP_MODULE;
                }else {
                    return "";
                }

            } else if (param.equals(TYPE)) {
                if(getArtifactType(key).equals(TYPE_OMOD)){
                    return TYPE_JAR;
                }else if(getArtifactType(key).equals(TYPE_WAR)){
                    return TYPE_WAR;
                }else {
                    return "";
                }
            } else {
                return "";
            }
        }
    }

    private String extractArtifactId(String key){
        String type = getArtifactType(key);
        StringBuilder stringBuilder = new StringBuilder(key.substring(key.indexOf(".")+1, key.length()));
        if(type.equals(TYPE_OMOD)) {
            stringBuilder.append("-");
            stringBuilder.append(type);
        } else if(type.equals(TYPE_WAR)){
            stringBuilder.append("-");
            stringBuilder.append("webapp");
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
        newModule = getArtifactWithStrippedArtifactId(newModule);
        if(!newModule.getGroupId().equals(Artifact.GROUP_MODULE)){
            setCustomModuleGroupId(newModule);
        }
        if(!newModule.getType().equals(TYPE_JAR)){
            setCustomModuleType(newModule);
        }
        setModule(newModule);

    }

    public void removeModuleProperties(Artifact artifact) {
        artifact = getArtifactWithStrippedArtifactId(artifact);
        if (getModuleArtifact(artifact.getArtifactId()) != null) {
            Properties newProperties = new Properties();
            newProperties.putAll(properties);
            for(Object keyObject: properties.keySet()){
                String key = keyObject.toString();
                if(key.equals(TYPE_OMOD+"."+artifact.getArtifactId())){
                    newProperties.remove(key);
                } else if(key.equals(TYPE_OMOD+"."+artifact.getArtifactId()+"."+TYPE)){
                    newProperties.remove(key);
                } else if(key.equals(TYPE_OMOD+"."+artifact.getArtifactId()+"."+GROUP_ID)){
                    newProperties.remove(key);
                }
            }
            properties = newProperties;
        }
    }

    private Artifact getArtifactWithStrippedArtifactId(Artifact artifact){
        String artifactId = artifact.getArtifactId();
        if(artifactId.endsWith("-omod")){
             artifact.setArtifactId(artifactId.substring(0, artifactId.indexOf("-")));
            return artifact;
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
        for(Object key: getAllKeys()){
            if (isBaseSdkProperty(key.toString())) {
                other.properties.put(key, properties.get(key));
            }
        }
        for(Object key: new ArrayList<>(other.getAllKeys())){
            if(isBaseSdkProperty(key.toString())){
                if(StringUtils.isBlank(getParam(key.toString()))){
                    other.properties.remove(key);
                }
            }
        }
    }

    private boolean isBaseSdkProperty(String key) {
        return  (key.startsWith(TYPE_OMOD) || key.startsWith(TYPE_WAR) || key.equals(NAME) || key.equals(VERSION));
    }


}
