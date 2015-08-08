package org.openmrs.maven.plugins.utility;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class for reading/writing .properties files
 */
public class PropertyManager {

    private static final String COMMA = ",";

    private Properties properties;
    private String path;

    private PropertyManager() { properties = new Properties(); }

    /**
     * Initialize
     * @param filePath - .properties file path
     */
    public PropertyManager(String filePath) throws MojoExecutionException {
        this();
        this.path = filePath;
        File config = new File(path);
        if (config.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(path);
                properties.load(in);
                in.close();
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
     * Set default properties
     */
    public void setDefaults() {
        properties.setProperty("install_method", "auto");
        properties.setProperty("connection.url", "jdbc:h2:@APPLICATIONDATADIR@/database/@DBNAME@;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=-1");
        properties.setProperty("connection.driver_class", "org.h2.Driver");
        properties.setProperty("connection.username", "sa");
        properties.setProperty("connection.password", "sa");
        properties.setProperty("database_name", "openmrs");
        properties.setProperty("has_current_openmrs_database", "true");
        properties.setProperty("create_database_user", "false");
        properties.setProperty("create_tables", "true");
        properties.setProperty("add_demo_data", "false");
        properties.setProperty("module_web_admin", "true");
        properties.setProperty("auto_update_database", "false");
        properties.setProperty("admin_user_password", "Admin123");
    }

    /**
     * Get param from properties
     * @param key - property key
     * @return - property value
     */
    public String getParam(String key) {
        return properties.getProperty(key);
    }

    /**
     * Set param to properties object (without applying)
     * @param key - property key
     * @param value - value to set
     */
    public void setParam(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Add value to value list for a selected key
     * @param key
     * @param value
     */
    public void addToValueList(String key, String value) {
        String beforeValue = properties.getProperty(key);
        if (beforeValue == null) beforeValue = value;
        else {
            List<String> values = new ArrayList<String>(Arrays.asList(beforeValue.split(COMMA)));
            for (String val: values) {
                if (val.equals(value)) return;
            }
            values.add(value);
            beforeValue = StringUtils.join(values.toArray(), COMMA);
        }
        properties.setProperty(key, beforeValue);
    }

    /**
     * Remove value from value list for a selected key
     * @param key
     * @param value
     */
    public void removeFromValueList(String key, String value) {
        String beforeValue = properties.getProperty(key);
        if (beforeValue == null) return;
        else {
            List<String> values = new ArrayList<String>(Arrays.asList(beforeValue.split(COMMA)));
            int indx = -1;
            for (String val: values) {
                if (val.equals(value)) {
                    indx = values.indexOf(val);
                    break;
                }
            }
            if (indx != -1) values.remove(indx);
            if (values.size() == 0) properties.remove(key);
            else {
                beforeValue = StringUtils.join(values.toArray(), COMMA);
                properties.setProperty(key, beforeValue);
            }

        }
    }

    /**
     * Write properties to file
     * @param path
     */
    public void apply(String path) throws MojoExecutionException {
    	replaceDbNameInDbUri();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            properties.store(out, null);
            out.close();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * It's a quick fix for OpenMRS, which doesn't pick up the database_name property correctly
     * and doesn't replace DBNAME with the specified value.
     */
	private void replaceDbNameInDbUri() {
	    String dbUri = getParam(SDKConstants.PROPERTY_DB_URI);
    	dbUri = dbUri.replace("@DBNAME@", getParam(SDKConstants.PROPERTY_DB_NAME));
    	setParam(SDKConstants.PROPERTY_DB_URI, dbUri);
    }

    /**
     * Save properties
     */
    public void apply() throws MojoExecutionException {
        apply(path);
    }

    /**
     * Set property and apply it
     * @param key - property key
     * @param value - value to set
     */
    public void applyParam(String key, String value) throws MojoExecutionException {
        setParam(key, value);
        apply();
    }
}
