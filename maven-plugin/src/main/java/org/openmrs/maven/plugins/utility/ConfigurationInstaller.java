package org.openmrs.maven.plugins.utility;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ConfigurationInstaller {

    private final Wizard wizard;

    private final ModuleInstaller moduleInstaller;

    public ConfigurationInstaller(Wizard wizard, ModuleInstaller moduleInstaller) {
        this.wizard = wizard;
        this.moduleInstaller = moduleInstaller;
    }

    /**
     * Sets the configuration folder for the specified server using the provided distro properties.
     *
     * @param server           The server for which to set the configuration folder.
     * @param distroProperties The distro properties containing the configuration information.
     */
    public void setConfigurationFolder(Server server, DistroProperties distroProperties) throws MojoExecutionException {
        if(distroProperties.getConfigArtifacts().isEmpty()) {
            return;
        }
        File configDir = new File(server.getServerDirectory(), SDKConstants.OPENMRS_SERVER_CONFIGURATION);
        configDir.mkdir();
        downloadConfigurations(distroProperties, configDir);
        File distroJar = new File(server.getServerDirectory(),"openmrs-distro.jar");
        if (!distroJar.exists()) {
            return;
        }
        try {
            ZipFile zipFile = new ZipFile(distroJar);
            zipFile.extractAll(configDir.getPath());
            for (File file : Objects.requireNonNull(configDir.listFiles())) {
                if (file.getName().equals("openmrs_config")) {
                    FileUtils.copyDirectory(file, configDir);
                }
                file.delete();
            }
        }
        catch (ZipException | IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            distroJar.delete();
        }
    }

    /**
     * Downloads the configuration artifact specified in the distro properties and saves them in the provided config directory.
     *
     * @param distroProperties The distro properties containing the configuration artifacts to download.
     * @param configDir        The directory where the configuration files will be saved.
     * @throws MojoExecutionException If an error occurs while downloading the configuration files.
     */
    private void downloadConfigurations(DistroProperties distroProperties, File configDir) throws MojoExecutionException {
        List<Artifact> configs = distroProperties.getConfigArtifacts();
        wizard.showMessage("Downloading Configs...\n");
        if (!configs.isEmpty()) {
            for (Artifact config : configs) {
                config.setDestFileName("openmrs-distro.jar");
                wizard.showMessage("Downloading Config: " + config);
                moduleInstaller.installModule(config, configDir.getPath());
            }
        }
    }
}
