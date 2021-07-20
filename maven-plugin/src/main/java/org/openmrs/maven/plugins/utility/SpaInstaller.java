package org.openmrs.maven.plugins.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpaInstaller {

    static final String BAD_SPA_PROPERTIES_MESSAGE = "Distro properties file contains invalid 'spa.' elements. " +
            "Please check the distro properties file and the specification. " +
            "Parent properties cannot have their own values (i.e., if 'spa.foo.bar' exists, 'spa.foo' cannot be assigned a value). " +
            "Duplicate properties are not allowed.";
    static final String NODE_VERSION = "16.2.0";
    static final String NPM_VERSION = "7.13.0";
    static final String BUILD_TARGET_DIR = "frontend";

    private NodeHelper nodeHelper;

    private DistroHelper distroHelper;

    public SpaInstaller(DistroHelper distroHelper,
                        NodeHelper nodeHelper) {
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
    public void installFromDistroProperties(File appDataDir, DistroProperties distroProperties) throws MojoExecutionException {
        // We find all the lines in distro properties beginning with `spa` and convert these
        // into a JSON structure. This is passed to the microfrontend build tools.
        // If no SPA elements are present in the distro properties, the SPA is not installed.
        Map<String, String> spaProperties = distroProperties.getSpaProperties(distroHelper, appDataDir);
        if (!spaProperties.isEmpty()) {
            JsonObject spaConfigJson = convertPropertiesToJSON(spaProperties);
            File spaConfigFile = new File(appDataDir, "spa-build-config.json");
            writeJSONObject(spaConfigFile, spaConfigJson);
            nodeHelper.installNodeAndNpm(NODE_VERSION, NPM_VERSION);
            File buildTargetDir = new File(appDataDir, BUILD_TARGET_DIR);
            nodeHelper.runNpx("openmrs@next --version");  // print frontend tool version number
            nodeHelper.runNpx(String.format("openmrs@next build --target %s --build-config %s", buildTargetDir, spaConfigFile));
            nodeHelper.runNpx(String.format("openmrs@next assemble --target %s --mode config --config %s", buildTargetDir, spaConfigFile));
        }
    }

    private JsonObject convertPropertiesToJSON(Map<String, String> properties) throws RuntimeException {
        Set<String> foundPropertySetKeys = new HashSet<>();
        JsonObject result = new JsonObject();
        for (String dotDelimitedKeys : properties.keySet()) {
            String[] keys = dotDelimitedKeys.split("\\.");
            for (int i = 0; i < keys.length - 1; i++) {
                foundPropertySetKeys.add(StringUtils.join(Arrays.copyOfRange(keys, 0, i), "."));
            }
        }
        for (String dotDelimitedKeys : properties.keySet()) {
            if (foundPropertySetKeys.contains(dotDelimitedKeys)) {
                String badLine = "spa." + dotDelimitedKeys + "=" + properties.get(dotDelimitedKeys);
                throw new RuntimeException(BAD_SPA_PROPERTIES_MESSAGE +
                        " The following property is a parent property to another, and therefore cannot be assigned a value:\t\"" + badLine + "\"");
            }
            addPropertyToJSONObject(result, dotDelimitedKeys, properties.get(dotDelimitedKeys));
        }
        return result;
    }

    /**
     * Add a line from the properties file to the new JSON object. Creates nested objects as needed.
     * Known array-valued keys are parsed as comma-delimited arrays; e.g.,
     *   `configUrls=qux` becomes `{ "configUrls": ["qux"] }` because `configUrls` is known to be array-valued
     *
     * @param jsonObject the object being constructed
     * @param propertyKey
     * @param value
     */
    private void addPropertyToJSONObject(JsonObject jsonObject, String propertyKey, String value) {
        String[] keys = propertyKey.split("\\.");
        if (keys.length == 1) {
            if (jsonObject.has(keys[0])) {
                throw new RuntimeException(BAD_SPA_PROPERTIES_MESSAGE +
                        " Encountered this error processing a property containing the key '" + keys[0] + "' and with value " + value);
            }
            if ("configUrls".equals(keys[0])) {
                JsonArray arr = new JsonArray();
                for (String valueElement : value.split(",")) {
                    arr.add(valueElement);
                }
                jsonObject.add(keys[0], arr);
            } else {
                jsonObject.addProperty(keys[0], value);
            }
        } else {
            if (!jsonObject.has(keys[0])) {
                jsonObject.add(keys[0], new JsonObject());
            }
            Object childObject = jsonObject.get(keys[0]);
            if (!(childObject instanceof JsonObject)) {
                throw new RuntimeException(BAD_SPA_PROPERTIES_MESSAGE +
                        " Also please post to OpenMRS Talk and include this full message. If you are seeing this, there has been a programming error.");
            }
            String childKeys = StringUtils.join(Arrays.copyOfRange(keys, 1, keys.length), ".");
            addPropertyToJSONObject((JsonObject) childObject, childKeys, value);
        }
    }

    private static void writeJSONObject(File file, JsonObject jsonObject) throws MojoExecutionException {
        Gson gson = new Gson();
        try (FileWriter out = new FileWriter(file)) {
        	gson.toJson(jsonObject, out);
        }
        catch (IOException e) {
        	throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
