package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.ContentPackage;
import org.openmrs.maven.plugins.model.ContentProperties;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.openmrs.maven.plugins.utility.SDKConstants.CONTENT_PROPERTIES_NAME;

/**
 * This class downloads and moves content backend config to respective configuration folders.
 */
public class ContentHelper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MavenEnvironment mavenEnvironment;

    public ContentHelper(MavenEnvironment mavenEnvironment) {
        this.mavenEnvironment = mavenEnvironment;
    }

    public ContentProperties getContentProperties(ContentPackage contentPackage) throws MojoExecutionException {
        Artifact artifact = contentPackage.getArtifact();
        log.debug("Retrieving content package: " + artifact);
        try (TempDirectory tempDirectory = TempDirectory.create(artifact.getArtifactId() + "-content-package")) {
            mavenEnvironment.getArtifactHelper().downloadArtifact(artifact, tempDirectory.getFile(), true);
            Properties properties = new Properties();
            File contentPropertiesFile = new File(tempDirectory.getFile(), CONTENT_PROPERTIES_NAME);
            if (contentPropertiesFile.exists()) {
                PropertiesUtils.loadPropertiesFromFile(contentPropertiesFile, properties);
            }
            else {
                log.warn("No " + CONTENT_PROPERTIES_NAME + " found in " + artifact);
            }
            return new ContentProperties(properties);
        }
    }

    /**
     * Returns all content packages defined in the distro properties, in the order in which they should be installed
     * If one content package declares another as a dependency within it's content.properties file, then the dependency
     * is returned before the dependent package in the list.
     * If no definitive order can be established, an exception is thrown
     */
    public List<ContentPackage> getContentPackagesInInstallationOrder(DistroProperties distroProperties) throws MojoExecutionException {
        List<ContentPackage> ret = new ArrayList<>();
        Set<String> alreadyAdded = new HashSet<>();
        Map<ContentPackage, ContentProperties> packages = new LinkedHashMap<>();
        for (ContentPackage contentPackage : distroProperties.getContentPackages()) {
            packages.put(contentPackage, getContentProperties(contentPackage));
        }
        int packagesRemaining = packages.size();
        while (packagesRemaining > 0) {;
            int packagesAtStart = packagesRemaining;
            for (ContentPackage contentPackage : packages.keySet()) {
                ContentProperties contentProperties = packages.get(contentPackage);
                boolean canInstall = !alreadyAdded.contains(contentPackage.getGroupIdAndArtifactId());
                for (ContentPackage dependentPackage : contentProperties.getContentPackages()) {
                    canInstall = canInstall && alreadyAdded.contains(dependentPackage.getGroupIdAndArtifactId());
                }
                if (canInstall) {
                    alreadyAdded.add(contentPackage.getGroupIdAndArtifactId());
                    ret.add(contentPackage);
                    packagesRemaining--;
                }
            }
            if (packagesRemaining == packagesAtStart) {
                throw new MojoExecutionException("Unable to order content packages due to unresolved dependencies.");
            }
        }
        return ret;
    }

    /**
     * This installs the backend configuration for all content packages defined in the distribution
     * Installation is done in the order in which they should be installed, with packages that are dependencies of other packages installed first
     */
    public void installBackendConfig(DistroProperties distroProperties, File installDir) throws MojoExecutionException {
        log.debug("Installing backend configuration for content packages in distribution");
        for (ContentPackage contentPackage : getContentPackagesInInstallationOrder(distroProperties)) {
            log.debug("Installing content package " + contentPackage.getGroupIdAndArtifactId());
            installBackendConfig(contentPackage, installDir);
        }
    }

    /**
     * The standard content package template places the backend config into a folder within the zip archive at `configuration/backend_configuration`
     * However, other initial content packages did not follow this pattern.
     * To account for these, this will also attempt to look for backend_config in other directories, if the preferred directories does not exist
     */
    public void installBackendConfig(ContentPackage contentPackage, File installDir) throws MojoExecutionException {
        log.debug("Installing backend configuration for " + contentPackage + " to " + installDir);
        Artifact artifact = contentPackage.getArtifact();
        try (TempDirectory tempDirectory = TempDirectory.create(artifact.getArtifactId() + "-content-package")) {
            mavenEnvironment.getArtifactHelper().downloadArtifact(artifact, tempDirectory.getFile(), true);

            // Get the backend directory.  If not found, fall back to some alternatives for compatibility with existing artifacts
            File backendDir = tempDirectory.getPath().resolve("configuration").resolve("backend_configuration").toFile();
            if (!backendDir.exists() || !backendDir.isDirectory()) {
                backendDir = tempDirectory.getPath().resolve("configs").resolve("backend_config").toFile();
            }
            if (!backendDir.exists() || !backendDir.isDirectory()) {
                backendDir = tempDirectory.getFile();
            }

            // If a namespace is passed in, then install backend configurations into namespaced subdirectories for each domain
            String namespace = contentPackage.getNamespace();
            boolean emptyNamespace = StringUtils.isBlank(namespace) || namespace.equals(".") || namespace.equals("/") || namespace.equals("false");
            if (emptyNamespace) {
                log.debug("Copying " + backendDir + " to " + installDir);
                FileUtils.copyDirectory(backendDir, installDir);
            }
            else {
                File[] configFiles = backendDir.listFiles();
                if (configFiles != null) {
                    for (File configFile : configFiles) {
                        if (configFile.isDirectory()) {
                            Path namespacedConfigDir = installDir.toPath().resolve(configFile.getName()).resolve(contentPackage.getNamespace());
                            Files.createDirectories(namespacedConfigDir);
                            log.debug("Copying " + configFile + " to " + namespacedConfigDir);
                            FileUtils.copyDirectory(configFile, namespacedConfigDir.toFile());
                        }
                        else {
                            log.debug("Copying " + configFile + " to " + installDir);
                            FileUtils.copyFile(configFile, installDir);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw new MojoExecutionException("Unable to install backend configuration to " + installDir, e);
        }
    }

    public List<File> installFrontendConfigs(ContentPackage contentPackage, File installDir) throws MojoExecutionException {
        log.debug("Installing frontend configuration for " + contentPackage + " to " + installDir);
        Artifact artifact = contentPackage.getArtifact();
        List<File> ret = new ArrayList<>();
        try (TempDirectory tempDirectory = TempDirectory.create(artifact.getArtifactId() + "-content-package")) {
            mavenEnvironment.getArtifactHelper().downloadArtifact(artifact, tempDirectory.getFile(), true);

            // Get the frontend directory.  If not found, fall back to some alternatives for compatibility with existing artifacts
            File frontendDir = tempDirectory.getPath().resolve("configuration").resolve("frontend_configuration").toFile();
            if (!frontendDir.exists() || !frontendDir.isDirectory()) {
                frontendDir = tempDirectory.getPath().resolve("configs").resolve("frontend_config").toFile();
            }

            // If a frontend directory is found, copy files within it to target directory, in a subdirectory for the current content package
            if (frontendDir.exists() && frontendDir.isDirectory()) {
                File targetDir = new File(installDir, artifact.getArtifactId());
                Files.createDirectory(targetDir.toPath());
                log.debug("Copying " + frontendDir + " to " + targetDir);
                FileUtils.copyDirectory(frontendDir, targetDir);
                ret = Arrays.asList(Objects.requireNonNull(targetDir.listFiles()));
            }
            else {
                log.warn("No frontend configuration found in content package");
            }

            return ret;
        }
        catch (IOException e) {
            throw new MojoExecutionException("Unable to install frontend configuration to " + installDir, e);
        }
    }
}
