package org.openmrs.maven.plugins;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @goal create-project
 * @requiresProject false
 *
 */
public class CreateProject extends AbstractTask {

    private static final String TYPE_PLATFORM = "Platform module";
    public static final String TYPE_REFAPP = "Reference Application module";

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        String type = wizard.promptForMissingValueWithOptions(
                "What kind of project would you like to create?", null, null,
                Arrays.asList(TYPE_PLATFORM, TYPE_REFAPP), null, null);
        if(type.equals(TYPE_REFAPP)){
            String archetypeVersion = getSdkVersion();
            executeMojo(
                    plugin(
                            groupId(SDKConstants.ARCH_GROUP_ID),
                            artifactId(SDKConstants.ARCH_ARTIFACT_ID),
                            version(SDKConstants.ARCH_VERSION)
                    ),
                    goal("generate"),
                    configuration(
                            element(name("archetypeCatalog"), SDKConstants.ARCH_CATALOG),
                            element(name("archetypeGroupId"), SDKConstants.ARCH_MODULE_GROUP_ID),
                            element(name("archetypeArtifactId"), SDKConstants.ARCH_MODULE_ARTIFACT_ID),
                            element(name("archetypeVersion"), archetypeVersion)
                    ),
                    executionEnvironment(mavenProject, mavenSession, pluginManager)
            );
        } else if(type.equals(TYPE_PLATFORM)){
            executeMojo(
                    plugin(
                            groupId(SDKConstants.WIZARD_GROUP_ID),
                            artifactId(SDKConstants.WIZARD_ARTIFACT_ID),
                            version(SDKConstants.WIZARD_VERSION)
                    ),
                    goal("generate"),
                    configuration(
                            element(name("archetypeCatalog"), SDKConstants.ARCH_CATALOG)
                    ),
                    executionEnvironment(mavenProject, mavenSession, pluginManager)
            );
        } else {
            throw new MojoExecutionException("Invalid project type");
        }
    }

    private String getSdkVersion() throws MojoExecutionException {
        InputStream sdkPom = CreateProject.class.getClassLoader().getResourceAsStream("sdk.properties");
        Properties sdk = new Properties();
        try {
            sdk.load(sdkPom);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(sdkPom);
        }
        return sdk.getProperty("version");
    }
}
