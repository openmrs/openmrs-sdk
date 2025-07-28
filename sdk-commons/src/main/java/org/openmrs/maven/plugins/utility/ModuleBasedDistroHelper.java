package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ModuleBasedDistroHelper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, Artifact> resolved = new HashMap<>();

    private final Map<String, Artifact> unResolved = new HashMap<>();

    private final List<String> groupIds = Arrays.asList(Artifact.GROUP_MODULE, "org.openmrs");

    private final List<String> types = Arrays.asList("jar", "omod");

    private final MavenEnvironment mavenEnvironment;

    private final File tempWorkingDir;

    public ModuleBasedDistroHelper(MavenEnvironment mavenEnvironment) {
        this.mavenEnvironment = mavenEnvironment;
        File userDir = new File(System.getProperty("user.dir"));
        if (Project.hasProject(userDir)) {
            this.tempWorkingDir = new File(mavenEnvironment.getMavenProject().getBuild().getDirectory(), "moduleBasedArtifactsTempDir");
            tempWorkingDir.mkdirs();
        } else {
            this.tempWorkingDir = new File(userDir, "moduleBasedArtifactsTempDir");
        }
    }

    public Properties generateDitributionPropertiesFromModules(Artifact ...modules) {
        Properties properties = new Properties();
        properties.put("name", "Module based distro");
        properties.put("version", "100.0.0-SNAPSHOT");
        properties.put("war.openmrs", "2.7.4");
        properties.put("war.openmrs.groupId", "org.openmrs.web");
        properties.put("db.h2.supported", "false");

        for (Artifact module : modules) {
            resolve(module.getGroupId(), module.getArtifactId(), module.getVersion());     
        }
        for (Artifact artifact : resolved.values()) {
            String keyBase = "omod." + artifact.getArtifactId().replace("-omod", "");
            properties.setProperty(keyBase, artifact.getVersion());

            // Only include groupId if not the default
            if (!Artifact.GROUP_MODULE.equals(artifact.getGroupId())) {
                properties.setProperty(keyBase + ".groupId", artifact.getGroupId());
            }

            // Only include type if it's not "jar"
            if (artifact.getType() != null && !Artifact.TYPE_JAR.equalsIgnoreCase(artifact.getType())) {
                properties.setProperty(keyBase + ".type", artifact.getType());
            }
        }
        printResults();
        return properties;
    }
    
    public void resolve(String groupId, String artifactId, String version) {
        resolveRecursive(groupId, artifactId, version);
        resolved.keySet().forEach(unResolved::remove);
        FileUtils.deleteQuietly(tempWorkingDir);
    }

    private void resolveRecursive(String groupId, String artifactId, String version) {
        
        List<Artifact> dependencies = new ArrayList<>();
        
        try {
            // Ensure artifactId ends with -omod
            artifactId = artifactId.contains("-omod") ? artifactId : artifactId + "-omod";

            log.info("Resolving: groupId={}, artifactId={}, version={}", groupId, artifactId, version);

            Artifact current = resolved.get(artifactId);
            if (current != null && compareVersions(current.getVersion(), version) >= 0) {
                log.info("Already resolved {} at same or higher version ({} >= {})", artifactId, current.getVersion(), version);
                return;
            } else if (current == null) {
                // ensure it is valid version by it being greater than 0
                compareVersions("0", version);
            }

            File jarFile = downloadJarAndUpdateResolved(groupId, artifactId, version);

            dependencies = parseDependenciesFromJar(jarFile);
        } catch(Exception e) {
            unResolved.put(artifactId, new Artifact(artifactId, version, groupId));
            log.warn("Failed to resolve: groupId={}, artifactId={}, version={}", groupId, artifactId, version);
            log.warn("Reason: {}: {}", e.getClass(), e.getMessage());
            return;
        }
        for (Artifact dep : dependencies) {
            resolveRecursive(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
        }
    }

    private File downloadJarAndUpdateResolved(String groupId, String artifactId, String version) throws Exception {
        ArtifactHelper artifactHelper = new ArtifactHelper(mavenEnvironment);
        boolean downloaded = false;
        for (String gId : groupIds) {
            for (String type : types) {
                try {
                    artifactHelper.downloadArtifact(
                        new Artifact(artifactId, version, gId, type),
                        tempWorkingDir,
                        false
                    );
                    // Only add the artefact if it can be downloaded
                    resolved.put(artifactId, new Artifact(artifactId, version, gId, type));
                    downloaded = true;
                    break; // success, stop trying
                } catch (Exception ignored) {
                    // keep trying next combination
                }
            }
            if (downloaded) break;
        }

        if (!downloaded) {
            throw new RuntimeException("Failed to download artifact: " + groupId + ":" + artifactId + ":" + version);
        }

        for (String ext : types) {
            File downloadedArtifact = new File(tempWorkingDir, artifactId.replace("-omod", "") + "-" + version + "." + ext);
            if (downloadedArtifact.exists()) {
                return downloadedArtifact;
            }
        }
        throw new RuntimeException("Could not determine file extension for artifact: " + groupId + ":" + artifactId + ":" + version);
    }

    private List<Artifact> parseDependenciesFromJar(File jarFile) throws Exception {
        List<Artifact> dependencies = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry entry = jar.getEntry("config.xml");
            if (entry == null) return dependencies;

            try (InputStream in = jar.getInputStream(entry)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                // Prevent external entities and DTDs
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
                
                DocumentBuilder builder = dbf.newDocumentBuilder();
                builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

                Document doc = builder.parse(in);
                
                NodeList requiredModules = doc.getElementsByTagName("require_module");

                for (int i = 0; i < requiredModules.getLength(); i++) {
                    Element el = (Element) requiredModules.item(i);
                    String uid = el.getTextContent().trim();
                    String version = el.getAttribute("version").trim();

                    if (!uid.contains(".")) continue;

                    String artifactId = "";
                    String groupId = "";
                    if (uid.startsWith("org.openmrs.module.")) {
                        artifactId = uid.substring("org.openmrs.module.".length());
                        groupId = "org.openmrs.module";
                    } else if (uid.startsWith("org.openmrs.")) {
                        artifactId = uid.substring("org.openmrs.".length());
                        groupId = "org.openmrs";
                    } else {
                        throw new IllegalArgumentException("Unsupported UID format: " + uid);
                    }
                    dependencies.add(new Artifact(artifactId, version, groupId));
                }
            }
        }
        return dependencies;
    }

    private int compareVersions(String v1, String v2) {
        String[] qualifiers = {"alpha", "beta", "SNAPSHOT"};

        String base1 = v1.split("-(?=alpha|beta|SNAPSHOT)")[0];
        String base2 = v2.split("-(?=alpha|beta|SNAPSHOT)")[0];

        String q1 = v1.contains("-") ? v1.substring(v1.indexOf("-") + 1) : "";
        String q2 = v2.contains("-") ? v2.substring(v2.indexOf("-") + 1) : "";

        String[] parts1 = base1.split("\\.");
        String[] parts2 = base2.split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }

        // If base versions equal, compare qualifiers
        if (!q1.equals(q2)) {
            if (q1.isEmpty()) return 1; // release > pre-release
            if (q2.isEmpty()) return -1;

            int i1 = Arrays.asList(qualifiers).indexOf(q1);
            int i2 = Arrays.asList(qualifiers).indexOf(q2);
            return Integer.compare(i1, i2);
        }

        return 0;
    }

    public void printResults() {
        if (resolved.size() != 0) {
            log.info("");
            log.info("=================================== Resolved Modules ===================================");
            log.info(String.format("%-30s | %-25s | %s", "Module ID", "Group ID", "Version"));
            log.info("----------------------------------------------------------------------------------------");
            resolved.values().forEach(m ->
                log.info(String.format("%-30s | %-25s | %s", m.getArtifactId().replaceAll("(-omod)$", ""), m.getGroupId(), m.getVersion()))
            );
        }
        if (unResolved.size() != 0) {
            log.info("");
            log.info("=================================== Unresolved Modules =================================");
            log.info(String.format("%-30s | %-25s | %s", "Module ID", "Group ID", "Version"));
            log.info("----------------------------------------------------------------------------------------");
            unResolved.values().forEach(m ->
                log.info(String.format("%-30s | %-25s | %s", m.getArtifactId().replaceAll("(-omod)$", ""), m.getGroupId(), m.getVersion()))
            );
        }
        log.info("");
    }
}

