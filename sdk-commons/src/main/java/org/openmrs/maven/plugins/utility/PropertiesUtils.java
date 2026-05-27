package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PropertiesUtils {

	private static final Logger log = LoggerFactory.getLogger(PropertiesUtils.class);

	/**
	 * Loads properties from a given file
	 *
	 * @param file the file to load properties from
	 * @return a properties object representing the properties for the file
	 * @throws MojoExecutionException if an exception occurs loading or reading the file
	 */
	public static Properties loadPropertiesFromFile(File file) throws MojoExecutionException {
		Properties properties = new Properties();
		loadPropertiesFromFile(file, properties);
		return properties;
	}

	/**
	 * Loads properties from a given file
	 *
	 * @param file       the file to load properties from
	 * @param properties the properties object to load the properties into
	 * @throws MojoExecutionException if an exception occurs loading or reading the file
	 */
	public static void loadPropertiesFromFile(File file, Properties properties) throws MojoExecutionException {
		if (file == null) {
			throw new MojoExecutionException("The file to load the properties from must be supplied");
		}

		try (InputStream in = Files.newInputStream(file.toPath())) {
			loadPropertiesFromInputStream(in, properties);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * Loads properties from a classpath resource
	 *
	 * @param resource the resource to load properties from
	 * @return a properties object representing the properties for the resource
	 * @throws MojoExecutionException if an exception occurs loading or reading the file
	 */
	public static Properties loadPropertiesFromResource(String resource) throws MojoExecutionException {
		Properties properties = new Properties();
		loadPropertiesFromResource(resource, properties);
		return properties;
	}

	/**
	 * Loads properties from a classpath resource into a Properties object
	 *
	 * @param resource the classpath resource to load
	 * @param properties the properties object ot load the properties into
	 * @throws MojoExecutionException if an exception occurs loading or reading the resource
	 */
	public static void loadPropertiesFromResource(String resource, Properties properties) throws MojoExecutionException {
		if (resource == null) {
			throw new MojoExecutionException("The resource to load the properties from must be supplied");
		}

		try (InputStream in = PropertiesUtils.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new MojoExecutionException("Could not load \"" + resource + "\" from the classpath");
			}

			loadPropertiesFromInputStream(in, properties);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * Loads properties from an input stream
	 *
	 * @param in the input stream to load properties from
	 * @return a properties object representing the properties for the resource
	 * @throws MojoExecutionException if an exception occurs loading or reading the file
	 */
	public static Properties loadPropertiesFromInputStream(InputStream in) throws MojoExecutionException {
		Properties properties = new Properties();
		loadPropertiesFromInputStream(in, properties);
		return properties;
	}

	/**
	 * Loads properties from an input stream into a Properties object
	 *
	 * @param in the input stream to load properties from
	 * @param properties the properties object to load the properties into
	 * @throws MojoExecutionException if an exception occurs reading or parsing the input stream
	 */
	public static void loadPropertiesFromInputStream(InputStream in, Properties properties) throws MojoExecutionException {
		if (in == null) {
			throw new MojoExecutionException("Cannot load properties from a null input stream.");
		}

		if (properties == null) {
			throw new MojoExecutionException("The properties object to load the properties into must not be null");
		}

		try {
			properties.load(in);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	public static void resolveMavenPropertyPlaceholders(Properties properties, MavenProject mavenProject) throws MojoExecutionException {
		if (properties != null && mavenProject != null) {
			Properties mavenProjectProperties = mavenProject.getProperties();
			mavenProjectProperties.setProperty("project.parent.version", mavenProject.getVersion());
			mavenProjectProperties.setProperty("project.version", mavenProject.getVersion());
			resolvePlaceholders(properties, mavenProjectProperties);
		}
	}

	public static void resolvePlaceholders(Properties properties, Properties replacementValues) throws MojoExecutionException {
		if (properties != null && replacementValues != null) {
			StrSubstitutor substitutor = new StrSubstitutor(replacementValues);
			for (String property : properties.stringPropertyNames()) {
				String originalValue = properties.getProperty(property);
				String replacement = substitutor.replace(originalValue);
				if (replacement.contains("${") && replacement.contains("}")) {
					throw new MojoExecutionException("Failed to resolve placeholders in property: " + property + " = " + originalValue);
				}
				properties.put(property, replacement);
			}
		}
	}

	/**
	 * Retrieves frontend properties from the specified URL that points to a SPA configuration file.
	 *
	 * @param url The URL to retrieve the frontend properties from.
	 * @return A Properties object containing the retrieved frontend properties.
	 */
	public static Properties getFrontendPropertiesFromSpaConfigUrl(String url) throws MojoExecutionException {
		Properties properties = new Properties();
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(url);
			HttpResponse response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() > 299) {
				throw new MojoExecutionException("Could not load frontend properties from: " + url);
			}

			HttpEntity entity = response.getEntity();
			try (InputStream inputStream = entity.getContent()) {
				properties = getFrontendPropertiesFromJson(inputStream);
			}
			catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return properties;
	}

	public static Properties getFrontendPropertiesFromJson(InputStream inputStream) throws IOException {
		String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		Properties properties = new Properties();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonData);
		if (jsonNode.has("coreVersion")) {
			properties.setProperty("spa.core", jsonNode.get("coreVersion").asText());
		}
		JsonNode frontendModules = jsonNode.get("frontendModules");
		Iterator<Map.Entry<String, JsonNode>> modulesIterator = frontendModules.fields();
		while (modulesIterator.hasNext()) {
			Map.Entry<String, JsonNode> moduleEntry = modulesIterator.next();
			String moduleName = moduleEntry.getKey();
			String moduleVersion = moduleEntry.getValue().asText();
			properties.setProperty("spa.frontendModules." + moduleName, moduleVersion);
		}
		return properties;
	}

	/**
	 * Generates configuration property based on the specified Artifact.
	 *
	 * @param artifact The Artifact object containing information about the configuration.
	 * @return A Properties object containing the generated configuration properties.
	 */
	public static Properties getConfigurationProperty(Artifact artifact) {
		Properties properties = new Properties();
		properties.setProperty("config." + artifact.getArtifactId(), artifact.getVersion());
		return properties;
	}

	/**
	 * Retrieves distro properties from the specified ZIP file.
	 *
	 * @param file The ZIP file to read the backend properties from.
	 * @return A Properties object containing the retrieved backend properties.
	 * @throws MojoExecutionException If an error occurs while reading the ZIP file.
	 */
	public static Properties getDistroProperties(File file) throws MojoExecutionException {
		Properties properties = new Properties();
		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if ("distro.properties".equals(zipEntry.getName())) {
					properties.load(zipFile.getInputStream(zipEntry));
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not read \"" + file.getAbsolutePath() + "\" " + e.getMessage(), e);
		}
		finally {
			file.delete();
		}
		return properties;
	}

	/**
	 * Retrieves the module property from the specified URL by parsing an XML document.
	 *
	 * @param url The URL to retrieve the XML document from.
	 * @return A Properties object containing the retrieved module property.
	 * @throws RuntimeException If an error occurs while parsing the XML document.
	 */
	public static Properties getModuleProperty(String url) {
		Properties properties = new Properties();
		try {
			Document spaModulePom = parseXMLFromURL(url);
			String artifactId = spaModulePom.getElementsByTagName("artifactId").item(0).getTextContent();
			String version = spaModulePom.getElementsByTagName("version").item(0).getTextContent();
			properties.setProperty("omod." + artifactId, version);
		}
		catch (ParserConfigurationException | IOException | SAXException e) {
			throw new RuntimeException(e);
		}
		return properties;
	}

	/**
	 * Retrieves the OpenMRS SDK properties from sdk.properties file
	 *
	 * @return A {@link Properties} object containing the OpenMRS SDK properties, or an empty {@link Properties} object
	 *         if the SDK properties file does not exist.
	 * @throws MojoExecutionException If there is an error during the execution of this method, such as file I/O errors.
	 */
	public static Properties getSdkProperties() throws MojoExecutionException {
		File sdkFile = Server.getServersPath().resolve(SDKConstants.OPENMRS_SDK_PROPERTIES).toFile();
		if (sdkFile.exists()){
			return loadPropertiesFromFile(sdkFile);
		}
		return new Properties();
	}

	/**
	 * Saves the provided properties to a file.
	 *
	 * @param properties The {@link Properties} to be saved to the file.
	 * @param file       The {@link File} where the properties will be saved.
	 * @throws MojoExecutionException If an {@link IOException} occurs during the file writing process,
	 *                               or if there is an issue with the provided properties or file.
	 */
	public static void savePropertiesChangesToFile(Properties properties, File file)
			throws MojoExecutionException {
		try (OutputStream fos = Files.newOutputStream(file.toPath())) {
			properties.store(fos, "SDK Properties file");
		}
		catch (IOException e) {
			throw new MojoExecutionException(
					"An exception occurred while saving properties to " + file.getAbsolutePath() + " " + e.getMessage(), e);
		}
	}

	/**
	 * Parses an XML document from the specified URL and returns the corresponding Document object.
	 *
	 * @param url The URL of the XML document.
	 * @return The parsed XML document as a Document object.
	 * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
	 * @throws IOException                  If an I/O error occurs while parsing the XML document.
	 * @throws SAXException                 If any parse errors occur during the parsing process.
	 */
	private static Document parseXMLFromURL(String url) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(url);
		document.getDocumentElement().normalize();
		return document;
	}
}
