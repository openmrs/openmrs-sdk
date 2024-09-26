package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.openmrs.maven.plugins.utility.PropertiesUtils.getSdkProperties;

public class SpaInstaller {
	
	static final String BAD_SPA_PROPERTIES_MESSAGE = "Distro properties file contains invalid 'spa.' elements. "
	        + "Please check the distro properties file and the specification. "
	        + "Parent properties cannot have their own values (i.e., if 'spa.foo.bar' exists, 'spa.foo' cannot be assigned a value). "
	        + "Duplicate properties are not allowed.";
	
	static final String NODE_VERSION = "16.15.0";
	
	static final String NPM_VERSION = "8.5.5";
	
	static final String BUILD_TARGET_DIR = "frontend";	
	
	private final NodeHelper nodeHelper;
	
	private final DistroHelper distroHelper;
	
	private static final Logger logger = LoggerFactory.getLogger(SpaInstaller.class);
	
	public SpaInstaller(DistroHelper distroHelper, NodeHelper nodeHelper) {
		this.distroHelper = distroHelper;
		this.nodeHelper = nodeHelper;
	}
	
	/**
	 * Installs the SPA Microfrontend application based on entries in the distro properties.
	 *
	 * @param appDataDir The application data directory
	 * @param distroProperties Non-null
	 * @throws MojoExecutionException
	 */
	public void installFromDistroProperties(File appDataDir, DistroProperties distroProperties)
	        throws MojoExecutionException {
		
		installFromDistroProperties(appDataDir, distroProperties, false, null);
	}
	
	public void installFromDistroProperties(File appDataDir, DistroProperties distroProperties, boolean ignorePeerDependencies, Boolean overrideReuseNodeCache)
			throws MojoExecutionException {

		// We find all the lines in distro properties beginning with `spa` and convert these
		// into a JSON structure. This is passed to the frontend build tool.
		// If no SPA elements are present in the distro properties, the SPA is not installed.
		Map<String, String> spaProperties = distroProperties.getSpaProperties(distroHelper, appDataDir);		
		// Three of these properties are not passed to the build tool, but are used to specify the build execution itself
		String coreVersion = spaProperties.remove("core");
		if (coreVersion == null) {
			coreVersion = "next";
		}
		String nodeVersion = spaProperties.remove("node");
		if (nodeVersion == null) {
			nodeVersion = NODE_VERSION;
		}
		String npmVersion = spaProperties.remove("npm");
		if (npmVersion == null) {
			npmVersion = NPM_VERSION;
		}
		
		if (!spaProperties.isEmpty()) {
			Map<String, Object> spaConfigJson = convertPropertiesToJSON(spaProperties);

			File spaConfigFile = new File(appDataDir, "spa-build-config.json");
			writeJSONObject(spaConfigFile, spaConfigJson);

			Properties sdkProperties = getSdkProperties();
			boolean reuseNodeCache = (overrideReuseNodeCache != null) ? overrideReuseNodeCache : Boolean.parseBoolean(sdkProperties.getProperty("reuseNodeCache"));
			nodeHelper.installNodeAndNpm(nodeVersion, npmVersion, reuseNodeCache);
			File buildTargetDir = new File(appDataDir, BUILD_TARGET_DIR);

			String program = "openmrs@" + "5.8.0";
			String legacyPeerDeps = ignorePeerDependencies ? "--legacy-peer-deps" : "";
			// print frontend tool version number
			nodeHelper.runNpx(String.format("%s --version", program), legacyPeerDeps);
			
			List<File> configFiles = ContentHelper.collectFrontendConfigs(distroProperties);		
			
			if (configFiles.isEmpty()) {				
				nodeHelper.runNpx(
					String.format("%s assemble --target %s --mode config --config %s", program, buildTargetDir, spaConfigFile), legacyPeerDeps);
			}	else {				
                String assembleCommand = assembleWithFrontendConfig(program, buildTargetDir, configFiles, spaConfigFile); 
                nodeHelper.runNpx(assembleCommand, legacyPeerDeps); 
			}
			
			nodeHelper.runNpx(
				String.format("%s build --target %s --build-config %s", program, buildTargetDir, spaConfigFile), legacyPeerDeps);

			Path nodeCache = NodeHelper.tempDir;
			if (!reuseNodeCache) {
				try {
					if (nodeCache != null && nodeCache.toFile().exists()) {
						MoreFiles.deleteRecursively(nodeCache, RecursiveDeleteOption.ALLOW_INSECURE);
					}
				} catch (IOException e) {
					logger.error("Couldn't delete the temp file", e);
				}
			}
		}
	}
	
	public String assembleWithFrontendConfig(String program, File buildTargetDir, List<File> configFiles, File spaConfigFile) {
      
        StringBuilder command = new StringBuilder();
        command.append(String.format("%s assemble --target %s --mode config", program, buildTargetDir));
        for (File configFile : configFiles) {
            command.append(String.format(" --config-file %s", configFile.getAbsolutePath()));
        }
        command.append(String.format(" --config %s", spaConfigFile));
        return command.toString();
    }

	
	private Map<String, Object> convertPropertiesToJSON(Map<String, String> properties) throws MojoExecutionException {
		Set<String> foundPropertySetKeys = new HashSet<>();
		Map<String, Object> result = new LinkedHashMap<>();
		for (String dotDelimitedKeys : properties.keySet()) {
			String[] keys = dotDelimitedKeys.split("\\.");
			for (int i = 0; i < keys.length - 1; i++) {
				foundPropertySetKeys.add(StringUtils.join(Arrays.copyOfRange(keys, 0, i), "."));
			}
		}
		
		for (String dotDelimitedKeys : properties.keySet()) {
			if (foundPropertySetKeys.contains(dotDelimitedKeys)) {
				String badLine = "spa." + dotDelimitedKeys + "=" + properties.get(dotDelimitedKeys);
				throw new MojoExecutionException(BAD_SPA_PROPERTIES_MESSAGE
				        + " The following property is a parent property to another, and therefore cannot be assigned a value:\t\""
				        + badLine + "\"");
			}
			addPropertyToJSONObject(result, dotDelimitedKeys, properties.get(dotDelimitedKeys));
		}
		
		return result;
	}
	
	/**
	 * Add a line from the properties file to the new JSON object. Creates nested objects as needed.
	 * Known array-valued keys are parsed as comma-delimited arrays; e.g., `configUrls=qux` becomes
	 * `{ "configUrls": ["qux"] }` because `configUrls` is known to be array-valued
	 *
	 * @param jsonObject the object being constructed
	 * @param propertyKey
	 * @param value
	 */
	private void addPropertyToJSONObject(Map<String, Object> jsonObject, String propertyKey, String value)
	        throws MojoExecutionException {
		String[] keys = propertyKey.split("\\.");
		
		if (keys.length == 1) {
			if (jsonObject.containsKey(keys[0])) {
				throw new MojoExecutionException(
				        BAD_SPA_PROPERTIES_MESSAGE + " Encountered this error processing a property containing the key '"
				                + keys[0] + "' and with value " + value);
			}
			
			if ("configUrls".equals(keys[0])) {
				String[] urls = value.split(",");
				jsonObject.put(keys[0], urls);
			} else {
				jsonObject.put(keys[0], value);
			}
		} else {
			if (!jsonObject.containsKey(keys[0])) {
				jsonObject.put(keys[0], new LinkedHashMap<String, Object>());
			}
			
			Object childObject = jsonObject.get(keys[0]);
			if (!(childObject instanceof Map)) {
				throw new MojoExecutionException(BAD_SPA_PROPERTIES_MESSAGE
				        + " Also please post to OpenMRS Talk and include this full message. If you are seeing this, there has been a programming error.");
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> child = (Map<String, Object>) childObject;
			String childKeys = StringUtils.join(Arrays.copyOfRange(keys, 1, keys.length), ".");
			addPropertyToJSONObject(child, childKeys, value);
		}
	}
	
	private static void writeJSONObject(File file, Map<String, Object> jsonObject) throws MojoExecutionException {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(file, jsonObject);
		}
		catch (IOException e) {
			throw new MojoExecutionException(
			        "Exception while writing JSON to \"" + file.getAbsolutePath() + "\" " + e.getMessage(), e);
		}
	}
	
}
