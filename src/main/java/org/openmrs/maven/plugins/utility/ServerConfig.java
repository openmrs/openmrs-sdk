package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Class for reading/writing .properties files
 */
public class ServerConfig {
	
	public static final String COMMA = ",";
	
	public static final String SLASH = "/";
	
	private Properties properties;
	
	private File file;
	
	public ServerConfig(File file, Properties properties) {
		this.properties = properties;
		this.file = file;
	}
	
	public static boolean hasServerConfig(File dir) {
		if (dir.exists()) {
			File properties = new File(dir, "installation.properties");
			return properties.exists();
		}
		
		return false;
	}
	
	public static ServerConfig createServerConfig(File dir) {
		Properties properties = new Properties();
		File config = new File(dir, "installation.properties");
		
		return new ServerConfig(config, properties);
	}
	
	public static ServerConfig loadServerConfig(File dir) throws MojoExecutionException {
		if (!hasServerConfig(dir)) {
			throw new IllegalArgumentException("Installation.properties file is missing");
		}
		
		Properties properties = new Properties();
		File config = new File(dir, "installation.properties");
		
		FileInputStream in = null;
		try {
			in = new FileInputStream(config);
			properties.load(in);
			in.close();
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		
		return new ServerConfig(config, properties);
	}
	
	public void delete() {
		file.delete();
	}
	
	/**
	 * Set default properties
	 */
	public void setDefaults() {
		properties.setProperty("install_method", "auto");
		properties.setProperty("connection.url",
		    "jdbc:h2:@APPLICATIONDATADIR@/database/@DBNAME@;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=-1");
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
	
	public boolean addWatchedProject(Project project) {
		Set<Project> watchedProjects = getWatchedProjects();
		if (watchedProjects.add(project)) {
			setWatchedProjects(watchedProjects);
			return true;
		} else {
			return false;
		}
	}

	private void setWatchedProjects(Set<Project> watchedProjects) {
	    List<String> list = new ArrayList<String>();
	    for (Project watchedProject : watchedProjects) {
	    	list.add(String.format("%s,%s,%s", watchedProject.getGroupId(),
	    	    watchedProject.getArtifactId(), watchedProject.getPath()));
	    }
	    properties.setProperty("watched.projects", StringUtils.join(list.iterator(), ";"));
    }
	
	public void clearWatchedProjects() {
		setWatchedProjects(new LinkedHashSet<Project>());
	}
	
	public Project removeWatchedProjectByExample(Project project) {
		Set<Project> watchedProjects = getWatchedProjects();
		if (watchedProjects.remove(project)) {
			return project;
		} else {
			for (Iterator<Project> it = watchedProjects.iterator(); it.hasNext();) {
	            Project candidate = it.next();
	            if (candidate.matches(project)) {
	            	it.remove();
	            	setWatchedProjects(watchedProjects);
	            	
	            	return candidate;
	            }
            }
			return null;
		}
	}
	
	public Set<Project> getWatchedProjects() {
		String watchedProjectsProperty = properties.getProperty("watched.projects");
		if (StringUtils.isBlank(watchedProjectsProperty)) {
			return new LinkedHashSet<Project>();
		}
		
		Set<Project> watchedProjects = new LinkedHashSet<Project>();
		for (String watchedProjectProperty : watchedProjectsProperty.split(";")) {
			if (StringUtils.isBlank(watchedProjectProperty)) {
				continue;
			}
			
			String[] watchedProject = watchedProjectProperty.split(",");
			Project project = new Project(watchedProject[0], watchedProject[1], null, watchedProject[2]);
			watchedProjects.add(project);
		}
		return watchedProjects;
	}
	
	/**
	 * Get param from properties
	 * 
	 * @param key - property key
	 * @return - property value
	 */
	public String getParam(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * Set param to properties object (without applying)
	 * 
	 * @param key - property key
	 * @param value - value to set
	 */
	public void setParam(String key, String value) {
		properties.setProperty(key, value);
	}
	
	/**
	 * Add value to value list for a selected key
	 * 
	 * @param key
	 * @param value
	 */
	public void addToValueList(String key, String value) {
		String beforeValue = properties.getProperty(key);
		if (beforeValue == null)
			beforeValue = value;
		else {
			List<String> values = new ArrayList<String>(Arrays.asList(beforeValue.split(COMMA)));
			for (String val : values) {
				if (val.equals(value))
					return;
			}
			values.add(value);
			beforeValue = StringUtils.join(values.toArray(), COMMA);
		}
		properties.setProperty(key, beforeValue);
	}
	
	/**
	 * Remove value from value list for a selected key
	 * 
	 * @param key
	 * @param artifactId
	 */
	public void removeFromValueList(String key, String artifactId) {
		String beforeValue = properties.getProperty(key);
		if (beforeValue == null)
			return;
		else {
			List<String> values = new ArrayList<String>(Arrays.asList(beforeValue.split(COMMA)));
			int indx = -1;
			for (String val : values) {
				String[] params = val.split(SLASH);
				if (params[1].equals(artifactId)) {
					indx = values.indexOf(val);
					break;
				}
			}
			if (indx != -1)
				values.remove(indx);
			if (values.size() == 0)
				properties.remove(key);
			else {
				beforeValue = StringUtils.join(values.toArray(), COMMA);
				properties.setProperty(key, beforeValue);
			}
			
		}
	}
	
	/**
	 * Write properties to file
	 * 
	 * @param path
	 */
	public void saveTo(File path) throws MojoExecutionException {
		replaceDbNameInDbUri();
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(path);
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
	
	/**
	 * It's a quick fix for OpenMRS, which doesn't pick up the database_name property correctly and
	 * doesn't replace DBNAME with the specified value.
	 */
	private void replaceDbNameInDbUri() {
		String dbUri = getParam(SDKConstants.PROPERTY_DB_URI);
		dbUri = dbUri.replace("@DBNAME@", getParam(SDKConstants.PROPERTY_DB_NAME));
		setParam(SDKConstants.PROPERTY_DB_URI, dbUri);
	}
	
	/**
	 * Save properties
	 */
	public void save() throws MojoExecutionException {
		saveTo(file);
	}
	
	/**
	 * Set property and apply it
	 * 
	 * @param key - property key
	 * @param value - value to set
	 */
	public void applyParam(String key, String value) throws MojoExecutionException {
		setParam(key, value);
		save();
	}
}
