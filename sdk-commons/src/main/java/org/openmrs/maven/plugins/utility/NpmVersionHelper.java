package org.openmrs.maven.plugins.utility;

import org.json.JSONArray;
import org.openmrs.maven.plugins.model.PackageJson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NpmVersionHelper {

    private static final Logger log = LoggerFactory.getLogger(NpmVersionHelper.class);

    /**
     * Retrieves the resolved version of an NPM package based on the supplied semver range.
     * <p>
     * This method runs the `npm pack --dry-run --json <package>@<version>` command to
     * get the exact version of the package that satisfies the specified semver range.
     *
     * @param packageJson   The PackageJson object containing the name of the package.
     * @param versionRange  The semver range to resolve the version against.
     * @return              The resolved version of the package that satisfies the semver range.
     * @throws RuntimeException if the command fails or the resolved version cannot be determined.
     */
    public String getResolvedVersionFromNpmRegistry(PackageJson packageJson, String versionRange) {
        try {
            String packageName = packageJson.getName();
            JSONArray jsonArray = getJsonArray(versionRange, packageName);
            if (jsonArray.isEmpty()) {
                throw new RuntimeException("No versions found for the specified range: " + versionRange);
            }

            JSONObject jsonObject = jsonArray.getJSONObject(0);
            return jsonObject.getString("version");
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Error retrieving resolved version from NPM", e);
        }
    }

    private static JSONArray getJsonArray(String versionRange, String packageName) throws IOException, InterruptedException {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("npm", "pack", "--dry-run", "--json", packageName + "@" + versionRange)
                .redirectErrorStream(true)
                .inheritIO();
        Process process = processBuilder.start();

        // Read the command output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder outputBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            outputBuilder.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("npm pack --dry-run --json command failed with exit code " + exitCode);
        }
        return new JSONArray(outputBuilder.toString());
    }
}
