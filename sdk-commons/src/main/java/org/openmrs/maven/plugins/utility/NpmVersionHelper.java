package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.maven.plugins.model.PackageJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NpmVersionHelper {
	
	private static final Logger log = LoggerFactory.getLogger(NpmVersionHelper.class);
	
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Retrieves the resolved version of an NPM package based on the supplied semver range.
	 * <p>
	 * This method runs the {@code npm pack --dry-run --json <package>@<version>}  command to get the exact
	 * version of the package that satisfies the specified semver range.
	 *
	 * @param packageJson The PackageJson object containing the name of the package.
	 * @param versionRange The semver range to resolve the version against.
	 * @return The resolved version of the package that satisfies the semver range.
	 * @throws RuntimeException if the command fails or the resolved version cannot be determined.
	 */
	public String getResolvedVersionFromNpmRegistry(PackageJson packageJson, String versionRange) {
		try {
			String packageName = packageJson.getName();
			JsonNode jsonArray = getPackageMetadata(versionRange, packageName);
			if (jsonArray.isEmpty()) {
				throw new RuntimeException("No versions found for the specified range: " + versionRange);
			}
			
			JsonNode jsonObject = jsonArray.get(0);
			return jsonObject.get("version").asText();
		}
		catch (IOException | InterruptedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException("Error retrieving resolved version from NPM", e);
		}
	}

	public List<String> getPackageVersions(String packageName, int limit) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder()
					.command("npm", "view", packageName, "versions", "--dry-run", "--json").redirectErrorStream(true);

			Process process = processBuilder.start();

			StringBuilder output = new StringBuilder();
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					output.append(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return new ArrayList<>();
			}
			JsonNode jsonNode = objectMapper.readTree(output.toString());

			return StreamSupport.stream(jsonNode.spliterator(), false).map(JsonNode::asText)
					.sorted(Collections.reverseOrder()).limit(limit).collect(Collectors.toList());

		} catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
	
	private static JsonNode getPackageMetadata(String versionRange, String packageName) throws IOException, InterruptedException {
		if (packageName == null || packageName.isEmpty()) {
			throw new IllegalArgumentException("Package name cannot be null or empty");
		}
		
		ProcessBuilder processBuilder = new ProcessBuilder()
		        .command("npm", "pack", "--dry-run", "--json", packageName + "@" + versionRange).redirectErrorStream(true)
		        .inheritIO();
		Process process = processBuilder.start();
		
		// Read the command output
		StringBuilder outputBuilder = new StringBuilder();
		char[] buffer = new char[4096];
		try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
			int read;
			while ((read = reader.read(buffer)) >= 0) {
				outputBuilder.append(buffer, 0, read);
			}
		}
		
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new RuntimeException(
			        "npm pack --dry-run --json command failed with exit code " + exitCode + ". Output: " + outputBuilder);
		}
		
		return objectMapper.readTree(outputBuilder.toString());
	}
}
