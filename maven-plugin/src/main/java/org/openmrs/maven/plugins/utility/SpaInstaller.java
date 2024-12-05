package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.BaseSdkProperties;
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

@Setter
public class SpaInstaller {
	
	static final String BAD_SPA_PROPERTIES_MESSAGE = "Distro properties file contains invalid 'spa.' elements. "
	        + "Please check the distro properties file and the specification. "
	        + "Parent properties cannot have their own values (i.e., if 'spa.foo.bar' exists, 'spa.foo' cannot be assigned a value). "
	        + "Duplicate properties are not allowed.";
	
	static final String NODE_VERSION = "20.17.0";
	
	static final String NPM_VERSION = "10.8.2";
	
	private NodeHelper nodeHelper;
	private ModuleInstaller moduleInstaller;
	private ContentHelper contentHelper;
	private Wizard wizard;

	private static final Logger logger = LoggerFactory.getLogger(SpaInstaller.class);

	public SpaInstaller() {}
	
	public SpaInstaller(MavenEnvironment mavenEnvironment) {
		this.nodeHelper = new NodeHelper(mavenEnvironment);
		this.moduleInstaller = new ModuleInstaller(mavenEnvironment);
		this.contentHelper = new ContentHelper(mavenEnvironment);
		this.wizard = mavenEnvironment.getWizard();
	}
	
	/**
	 * Installs the SPA Microfrontend application based on entries in the distro properties.
	 *
	 * @param appDataDir The application data directory
	 * @param distroProperties Non-null
	 * @throws MojoExecutionException
	 */
	public void installFromDistroProperties(File appDataDir, DistroProperties distroProperties) throws MojoExecutionException {
		installFromDistroProperties(appDataDir, distroProperties, false, null);
	}
	
	public void installFromDistroProperties(File appDataDir, DistroProperties distroProperties, boolean ignorePeerDependencies, Boolean overrideReuseNodeCache)
			throws MojoExecutionException {

		File buildTargetDir = new File(appDataDir, SDKConstants.OPENMRS_SERVER_FRONTEND);
		if (buildTargetDir.exists()) {
			try {
				FileUtils.deleteDirectory(buildTargetDir);
			}
			catch (IOException e) {
				throw new MojoExecutionException("Unable to delete existing " + SDKConstants.OPENMRS_SERVER_FRONTEND + " directory", e);
			}
		}

		Map<String, String> spaArtifactProperties = distroProperties.getSpaArtifactProperties();
		Map<String, String> spaBuildProperties = distroProperties.getSpaBuildProperties();
		if (!spaArtifactProperties.isEmpty() && !spaBuildProperties.isEmpty()) {
			throw new MojoExecutionException("You cannot specify both spa artifact and build properties");
		}

		// If a maven artifact is defined, then we download the artifact and unpack it
		List<Artifact> spaArtifacts = distroProperties.getSpaArtifacts();
		if (!spaArtifacts.isEmpty()) {
			if (spaArtifacts.size() > 1) {
				throw new MojoExecutionException("Only a single spa artifact is supported");
			}
			Artifact artifact = spaArtifacts.get(0);
			if (artifact.getGroupId() == null || artifact.getVersion() == null) {
				throw new MojoExecutionException("If specifying a spa.artifactId, you must also specify a spa.groupId and spa.version property");
			}
			wizard.showMessage("Installing SPA from Maven artifact: " + artifact);
			if (buildTargetDir.mkdirs()) {
				wizard.showMessage("Created " + SDKConstants.OPENMRS_SERVER_FRONTEND + " directory: " + buildTargetDir.getAbsolutePath());
			}
			String includes = spaArtifactProperties.get(BaseSdkProperties.INCLUDES);
			moduleInstaller.installAndUnpackModule(artifact, buildTargetDir, includes);
			wizard.showMessage("SPA successfully installed to " + buildTargetDir.getAbsolutePath());
			return;
		}

		// If no maven artifact is defined, then check if npm build configuration is defined

		// First pull any optional properties that may be used to specify the core, node, or npm versions
		// These properties are not passed to the build tool, but are used to specify the build execution itself
		String coreVersion = spaBuildProperties.remove("core");
		if (coreVersion == null) {
			coreVersion = "next";
		}
		String nodeVersion = spaBuildProperties.remove("node");
		if (nodeVersion == null) {
			nodeVersion = NODE_VERSION;
		}
		String npmVersion = spaBuildProperties.remove("npm");
		if (npmVersion == null) {
			npmVersion = NPM_VERSION;
		}

		// If there are no remaining spa properties, then no spa configuration has been provided
		if (spaBuildProperties.isEmpty()) {
			wizard.showMessage("No spa configuration found in the distro properties");
			return;
		}

		// If there are remaining spa properties, then build and install using node
		Map<String, Object> spaConfigJson = convertPropertiesToJSON(spaBuildProperties);

		File spaConfigFile = new File(appDataDir, "spa-build-config.json");
		writeJSONObject(spaConfigFile, spaConfigJson);

		Properties sdkProperties = getSdkProperties();
		boolean reuseNodeCache = (overrideReuseNodeCache != null) ? overrideReuseNodeCache : Boolean.parseBoolean(sdkProperties.getProperty("reuseNodeCache"));
		nodeHelper.installNodeAndNpm(nodeVersion, npmVersion, reuseNodeCache);

		String program = "openmrs@" + coreVersion;
		String legacyPeerDeps = ignorePeerDependencies ? "--legacy-peer-deps" : "";
		// print frontend tool version number
		nodeHelper.runNpx(String.format("%s --version", program), legacyPeerDeps);

		if (distroProperties.getContentArtifacts().isEmpty()) {
			nodeHelper.runNpx(String.format("%s assemble --target %s --mode config --config %s", program, buildTargetDir,
				spaConfigFile), legacyPeerDeps);
		} else {
			List<File> configFiles = contentHelper.collectFrontendConfigs(distroProperties);
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
	
	private String assembleWithFrontendConfig(String program, File buildTargetDir, List<File> configFiles, File spaConfigFile) {
        StringBuilder command = new StringBuilder();
		command.append(program)
				.append(" assemble --target ")
				.append(buildTargetDir)
				.append(" --mode config --config ")
				.append(spaConfigFile);

        for (File configFile : configFiles) {
            command.append(" --config-file ").append(configFile.getAbsolutePath());
        }

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
