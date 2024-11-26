package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DistributionBuilder;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.maven.plugins.utility.SDKConstants.REFAPP_3X_PROMPT;

/**
 * Generates an openmrs-distro.properties file for a specific version of OpenMRS Distro
 */
@Mojo(name = "generate-distro", requiresProject = false)
public class GenerateDistro extends AbstractTask {

    private static final String GENERATE_DISTRO_PROMPT = "You can generate the distro.properties file for the following distributions";

    private static final String GET_VERSION_PROMPT = "You can generate the distro.properties file for the following versions";

    private static final Logger logger = LoggerFactory.getLogger(GenerateDistro.class);

    /**
     * Specify the location where the file should be saved. (Default to current location)
     */
    @Parameter(property = "output")
    private String outputLocation;


    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        List<String> options = new ArrayList<>();
        options.add(SDKConstants.REFAPP_2X_PROMPT);
        options.add(SDKConstants.REFAPP_3X_PROMPT);
        String choice = wizard.promptForMissingValueWithOptions(GENERATE_DISTRO_PROMPT, null, null, options);

        DistributionBuilder builder = new DistributionBuilder(getMavenEnvironment());
        Distribution distribution = null;

        Server server = new Server.ServerBuilder().build();

        try {
            server.setServerDirectory(Files.createTempDirectory("distro").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File outputFile = outputLocation != null ? new File(outputLocation) : new File(System.getProperty("user.dir"));
        Artifact artifact = null;
        switch (choice) {
            case SDKConstants.REFAPP_2X_PROMPT:
                artifact = wizard.promptForRefApp2xArtifact(versionsHelper);
                 break;
            case REFAPP_3X_PROMPT:
                artifact = wizard.promptForRefApp3xArtifact(versionsHelper);
        }
        distribution = builder.buildFromArtifact(artifact);
        if (distribution == null) {
            throw new MojoFailureException("Distribution could not be generated, the specified distribution could not be found");
        }
        distribution.getEffectiveProperties().saveTo(outputFile);
        logger.info(String.format("openmrs-distro.properties file created successfully at %s", outputFile.getAbsolutePath() + File.separator + DistroProperties.DISTRO_FILE_NAME));
    }
}
