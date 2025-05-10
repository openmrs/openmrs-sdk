package org.openmrs.maven.plugins.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.maven.plugins.model.PackageJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	 * This method runs the `npm pack --dry-run --json <package>@<version>` command to get the exact
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
			JsonNode metadata = getPackageMetadata(packageName, versionRange);
			if (metadata.isEmpty()) {
				log.warn("No versions found for range: {}. Skipping.", versionRange);
				return null;
			}

			return metadata.get(0).get("version").asText();
		} catch (IOException | InterruptedException e) {
			log.error("Error retrieving resolved version: {}", e.getMessage());
			throw new RuntimeException("Error resolving version", e);
		}
	}

	private static JsonNode getPackageMetadata(String packageName, String versionRange) throws IOException, InterruptedException {
		if (packageName == null || packageName.isEmpty()) {
			throw new IllegalArgumentException("Package name cannot be null or empty");
		}

		ProcessBuilder processBuilder = new ProcessBuilder().command("npm", "pack", "--dry-run", "--json", packageName + "@" + versionRange)
				.redirectErrorStream(true);
		Process process = processBuilder.start();
		StringBuilder output = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			reader.lines().forEach(output::append);
		}

		if (process.waitFor() != 0) {
			throw new RuntimeException("npm pack failed. Output: " + output);
		}

		return objectMapper.readTree(output.toString());
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
}
