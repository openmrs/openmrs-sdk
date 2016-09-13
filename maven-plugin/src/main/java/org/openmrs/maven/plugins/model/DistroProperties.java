package org.openmrs.maven.plugins.model;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
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


    public DistroProperties(String version){
        properties = new Properties();
        try {
            loadPropertiesFromResource(createFileName(version));
        } catch (MojoExecutionException e) {
            e.printStackTrace();
        }
    }

    public DistroProperties(String name, String version){
        properties = new Properties();
        setName(name);
        setVersion("1.0");
        setPlatformVersion(version);
    }

    public DistroProperties(Properties properties){
        this.properties = properties;
    }

    public DistroProperties(File file) throws MojoExecutionException{
        this.properties = new Properties();
        loadPropertiesFromFile(file);
    }

    private String createFileName(String version){
        return String.format(DEAFAULT_FILE_NAME, version);
    }

    private void loadPropertiesFromFile(File file) throws MojoExecutionException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            properties.load(in);
            in.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void loadPropertiesFromResource(String fileName) throws MojoExecutionException {
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(fileName);
            properties.load(in);
            in.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(in);
        }
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

    public String getPropertyPromt(String propertyName){
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

    public void saveTo(File path) throws MojoExecutionException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(path, DISTRO_FILE_NAME));
            properties.store(out, null);
            out.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void resolvePlaceholders(Properties projectProperties){
        for(Map.Entry<Object, Object> property: properties.entrySet()){
            if(hasPlaceholder(property.getValue())){
                try{
                    Object placeholderValue = projectProperties.get(getPlaceholderKey((String)property.getValue()));
                    if(placeholderValue == null){
                        throw new IllegalArgumentException("Failed to resolve property placeholders in distro file, no property for key: "+property.getKey());
                    } else {
                            property.setValue(putInPlaceholder((String)property.getValue(), (String)placeholderValue));
                        }
                } catch(ClassCastException e){
                    throw new IllegalArgumentException("Property with key "+property.getKey()+" and value "+property.getValue()+"is not placeholder.");
                }
            }
        }
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
}
