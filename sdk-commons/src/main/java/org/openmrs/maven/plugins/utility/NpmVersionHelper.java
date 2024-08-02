package org.openmrs.maven.plugins.utility;

import org.openmrs.maven.plugins.model.PackageJson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class NpmVersionHelper {

    public String getLatestReleasedVersionFromNpmRegistry(PackageJson packageJson) {
        try {
            String packageName = packageJson.getName();
            if (packageName == null || packageName.isEmpty()) {
                throw new IllegalArgumentException("Package name cannot be null or empty");
            }

            URL url = new URL("https://registry.npmjs.org/" + packageName.replace("/", "%2F"));
            JSONObject json = getJson(url);
            return json.getJSONObject("dist-tags").getString("latest");

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving latest version from NPM ", e);
        }
    }

    private static JSONObject getJson(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        conn.disconnect();
        return new JSONObject(sb.toString());
    }
}
