package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.semver4j.Semver;
import org.semver4j.SemverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

	private static final String CONTENT_PROPERTIES = "content.properties";

	private static final String CONTENT_PREFIX = "content.";

	static DistroHelper distroHelper;

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

		try (FileInputStream in = new FileInputStream(file)) {
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
		try (OutputStream fos = new FileOutputStream(file)) {
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

	/**
	 * Parses the content properties specified in the provided distro properties file and processes the corresponding content package ZIP files.
	 * <p>
	 * This method performs the following tasks:
	 * <ol>
	 *   <li>Creates a temporary directory to store downloaded ZIP files.</li>
	 *   <li>Iterates through all keys in the distro properties file.</li>
	 *   <li>Filters keys to only process those starting with {@code CONTENT_PREFIX}.</li>
	 *   <li>Downloads the ZIP files for each content package from the Maven repository and stores them in the temporary directory.</li>
	 *   <li>Processes each downloaded ZIP file to extract and validate the {@code content.properties} file contained within.</li>
	 *   <li>Handles errors and logs warnings if ZIP files are not found or if there are issues during processing.</li>
	 * </ol>
	 * <p>
	 * After processing, the temporary directory used for storing ZIP files is cleaned up to avoid leaving unnecessary files on the system.
	 *
	 * @param distroProperties The distro properties file containing information about content packages and their versions.
	 *                         This file is used to determine which content packages to download and process.
	 * @throws MojoExecutionException If an error occurs during the downloading or processing of content packages.
	 *                                This exception may be thrown due to issues with file operations, invalid content properties,
	 *                                or other runtime errors encountered during the execution of this method.
	 */
	public static void parseContentProperties(DistroProperties distroProperties) throws MojoExecutionException {
		File tempDirectory = null;
		try {
			tempDirectory = Files.createTempDirectory("content-packages").toFile();
			for (Object key : distroProperties.getAllKeys()) {
				String keyOb = key.toString();
				if (!keyOb.startsWith(CONTENT_PREFIX)) {
					continue;
				}

				String version = distroProperties.get(keyOb);
				String zipFileName = keyOb.replace(CONTENT_PREFIX, "") + "-" + version + ".zip";
				String artifactId = keyOb.replace(CONTENT_PREFIX, "");
				String groupId = checkIfOverwritten(keyOb, distroProperties);

				Artifact artifact = new Artifact(artifactId, version, groupId, "zip");
				File zipFile = distroHelper.downloadDistro(tempDirectory, artifact, zipFileName);

				if (zipFile == null) {
					log.warn("ZIP file not found for content package: {}", keyOb);
					continue;
				}

				processZipFile(zipFile, distroProperties);
			}

		} catch (IOException e) {
			throw new MojoExecutionException("Failed to process content packages", e);
		} finally {
			if (tempDirectory != null && tempDirectory.exists()) {
				try {
					FileUtils.deleteDirectory(tempDirectory);
				} catch (IOException e) {
					log.warn("Failed to delete temporary directory: {}", tempDirectory.getAbsolutePath(), e);
				}
			}
		}
	}

	private static void processZipFile(File contentPackageZipFile, DistroProperties distroProperties) throws MojoExecutionException {
		Properties contentProperties = new Properties();

		try (ZipFile zip = new ZipFile(contentPackageZipFile)) {
			boolean foundContentProperties = false;
			Enumeration<? extends ZipEntry> entries = zip.entries();

			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.getName().equals(CONTENT_PROPERTIES)) {
					foundContentProperties = true;

					try (InputStream inputStream = zip.getInputStream(zipEntry)) {
						contentProperties.load(inputStream);
						log.info("content.properties file found in {} and parsed successfully.", contentPackageZipFile.getName());

						if (contentProperties.getProperty("name") == null || contentProperties.getProperty("version") == null) {
							throw new MojoExecutionException(
									"Content package name or version not specified in content.properties in " + contentPackageZipFile.getName());
						}

						processContentProperties(contentProperties, distroProperties, contentPackageZipFile.getName());
					}
					break;
				}
			}

			if (!foundContentProperties) {
				throw new MojoExecutionException("No content.properties file found in ZIP file: " + contentPackageZipFile.getName());
			}

		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error reading content.properties from ZIP file: " + contentPackageZipFile.getName() + ": " + e.getMessage(), e);
		}
	}

	private static void processContentProperties(Properties contentProperties, DistroProperties distroProperties, String zipFileName) throws MojoExecutionException {
		for (String dependency : contentProperties.stringPropertyNames()) {
			if (dependency.startsWith("omod.") || dependency.startsWith("owa.") || dependency.startsWith("war")
					|| dependency.startsWith("spa.frontendModule") || dependency.startsWith("content.")) {
				String versionRange = contentProperties.getProperty(dependency);
				String distroVersion = distroProperties.get(dependency);

				if (distroVersion == null) {
					String latestVersion = findLatestMatchingVersion(dependency, versionRange);
					if (latestVersion == null) {
						throw new MojoExecutionException(
								"No matching version found for dependency " + dependency + " in " + zipFileName);
					}
					distroProperties.add(dependency, latestVersion);
				} else {
					checkVersionInRange(dependency, versionRange, distroVersion, contentProperties.getProperty("name"));
				}
			}
		}
	}

	private static String findLatestMatchingVersion(String dependency, String versionRange) {
		return distroHelper.findLatestMatchingVersion(dependency, versionRange);
	}

	/**
	 * Checks if the version from distro.properties satisfies the range specified in content.properties.
	 * Throws an exception if there is a mismatch.
	 *
	 * @param contentDependencyKey The key of the content dependency.
	 * @param contentDependencyVersionRange The version range specified in content.properties.
	 * @param distroPropertyVersion The version specified in distro.properties.
	 * @param contentPackageName The name of the content package.
	 * @throws MojoExecutionException If the version does not fall within the specified range or if the
	 *             range format is invalid.
	 */
	private static void checkVersionInRange(String contentDependencyKey, String contentDependencyVersionRange, String distroPropertyVersion, String contentPackageName) throws MojoExecutionException {
		Semver semverVersion = new Semver(distroPropertyVersion);

		try {
			boolean inRange = semverVersion.satisfies(contentDependencyVersionRange.trim());
			if (!inRange) {
				throw new MojoExecutionException("Incompatible version for " + contentDependencyKey + " in content package "
						+ contentPackageName + ". Specified range: " + contentDependencyVersionRange
						+ ", found in distribution: " + distroPropertyVersion);
			}
		} catch (SemverException e) {
			throw new MojoExecutionException("Invalid version range format for " + contentDependencyKey
					+ " in content package " + contentPackageName + ": " + contentDependencyVersionRange, e);
		}
	}

	/**
	 * Checks if the groupId for a given dependency is overwritten in the distro properties.
	 *
	 * @param dependencyKey    The key of the dependency.
	 * @param distroProperties The distro properties file.
	 * @return The groupId to use for this dependency.
	 */
	private static String checkIfOverwritten(String dependencyKey, DistroProperties distroProperties) {
		String groupId = distroProperties.get(dependencyKey + ".groupId");
		return groupId != null ? groupId : "org.openmrs.content";
	}
}
