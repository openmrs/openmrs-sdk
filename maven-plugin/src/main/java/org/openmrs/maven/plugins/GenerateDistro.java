package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Mojo(name = "generate-distro", requiresProject = false)
public class GenerateDistro extends AbstractTask {

    private static final String O2_DISTRIBUTION = "2.x Distribution";

    private static final String O3_DISTRIBUTION = "O3 Distribution";

    private static final String GENERATE_DISTRO_PROMPT = "You can generate the distro.properties file for the following distributions";

    private static final String GET_VERSION_PROMPT = "You can generate the distro.properties file for the following versions";

    private static final Logger logger = LoggerFactory.getLogger(GenerateDistro.class);

    @Parameter(property = "output")
    private String outputLocation;


    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        List<String> options = new ArrayList<>();
        options.add(O2_DISTRIBUTION);
        options.add(O3_DISTRIBUTION);
        String choice = wizard.promptForMissingValueWithOptions(GENERATE_DISTRO_PROMPT, null, null, options);

        DistroProperties distroProperties;

        Server server = new Server.ServerBuilder().build();

        try {
            server.setServerDirectory(Files.createTempDirectory("distro").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File outputFile = outputLocation != null ? new File(outputLocation) : new File(System.getProperty("user.dir"));
        switch (choice) {
            case O2_DISTRIBUTION:
                wizard.promptForRefAppVersionIfMissing(server, versionsHelper, GET_VERSION_PROMPT);
                if (DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())) {
                    distroProperties = new DistroProperties(server.getVersion());
                } else {
                    distroProperties = distroHelper.downloadDistroProperties(outputFile, server);
                }
                distroProperties.saveTo(outputFile);
                break;

            case O3_DISTRIBUTION:
                wizard.promptForO3RefAppVersionIfMissing(server, versionsHelper, GET_VERSION_PROMPT);
                Artifact artifact = new Artifact(server.getDistroArtifactId(), server.getVersion(),
                        server.getDistroGroupId(), "zip");
                Properties frontendProperties;
                if (new Version(server.getVersion()).higher(new Version("3.0.0-beta.16"))) {
                    frontendProperties = distroHelper.getFrontendProperties(server);
                }
                else {
                    frontendProperties = PropertiesUtils.getFrontendPropertiesFromSpaConfigUrl(
                            "https://raw.githubusercontent.com/openmrs/openmrs-distro-referenceapplication/"+ server.getVersion() +"/frontend/spa-build-config.json");
                }
                Properties configurationProperties = PropertiesUtils.getConfigurationProperty(artifact);
                File file = distroHelper.downloadDistro(server.getServerDirectory(), artifact);
                Properties backendProperties = PropertiesUtils.getDistroProperties(file);
                Properties spaModuleProperty = PropertiesUtils.getModuleProperty("https://raw.githubusercontent.com/openmrs/openmrs-module-spa/master/pom.xml");
                Properties allProperties = new Properties();
                allProperties.putAll(backendProperties);
                allProperties.putAll(spaModuleProperty);
                allProperties.putAll(frontendProperties);
                allProperties.putAll(configurationProperties);
                distroProperties = new DistroProperties(allProperties);
                distroProperties.saveTo(outputFile);
                break;
        }
        logger.info(String.format("openmrs-distro.properties file created successfully at %s", outputFile.getAbsolutePath() + File.separator + DistroProperties.DISTRO_FILE_NAME));
    }
}
