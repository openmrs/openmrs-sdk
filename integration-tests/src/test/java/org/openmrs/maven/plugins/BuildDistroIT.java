package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class BuildDistroIT extends AbstractSdkIT {

    @Test
    public void buildDistro_shouldBuildFromDistroPropertiesInCurrentDirectory() throws Exception {
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);
        addTaskParam("dir", "target");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("target/docker-compose.yml");
        assertFilePresent("target/docker-compose.override.yml");
        assertFilePresent("target/docker-compose.prod.yml");
        assertFilePresent("target/.env");
        assertFilePresent("target/web/Dockerfile");
        assertFilePresent("target/web/setenv.sh");
        assertFilePresent("target/web/startup.sh");
        assertFilePresent("target/web/wait-for-it.sh");
        assertFilePresent("target/web/modules");
        assertFilePresent("target/web/modules/uiframework-3.6.omod");
        assertFilePresent("target/web/modules/uicommons-1.7.omod");
        assertFilePresent("target/web/modules/owa-1.4.omod");
        assertFilePresent("target/web/owa");
        assertFilePresent("target/web/openmrs.war");
        assertFilePresent("target/web/openmrs-distro.properties");
        assertFileContains("war.openmrs=1.11.5", "target", "web", "openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void buildDistro_shouldBuildFromDistroPropertiesInCurrentProject() throws Exception {
        Path sourcePath = testResourceDir.resolve(TEST_DIRECTORY).resolve("buildDistroIT");
        FileUtils.copyDirectory(sourcePath.toFile(), testDirectory);

        addTaskParam("dir", "distro");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");
        assertFileContains("war.openmrs=2.6.9", "distro", "web", "openmrs-distro.properties");
        assertFilePresent("distro", "web", "openmrs-distro.properties");
        assertFilePresent("distro", "web", "openmrs_core", "openmrs.war");
        assertFilePresent("distro", "web", "openmrs_modules");
        assertNumFilesPresent(12, Paths.get("distro", "web", "openmrs_modules"), null, ".omod");
        assertSuccess();
    }

    @Test
    public void buildDistro_shouldBuildFromRefapp23Artifact() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("referenceapplication/docker-compose.yml");
        assertFilePresent("referenceapplication/docker-compose.override.yml");
        assertFilePresent("referenceapplication/docker-compose.prod.yml");
        assertFilePresent("referenceapplication/.env");
        assertFilePresent("referenceapplication/web/Dockerfile");
        assertFilePresent("referenceapplication/web/setenv.sh");
        assertFilePresent("referenceapplication/web/startup.sh");
        assertFilePresent("referenceapplication/web/wait-for-it.sh");
        assertFilePresent("referenceapplication/web/modules");
        assertFilePresent("referenceapplication/web/openmrs.war");
        assertFilePresent("referenceapplication/web/openmrs-distro.properties");
        assertNumFilesPresent(36, Paths.get("referenceapplication", "web", "modules"), null, ".omod");
        assertFileContains("war.openmrs=1.11.5", "referenceapplication", "web", "openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void buildDistro_shouldBuildFromRefapp2xArtifact() throws Exception {
        addTaskParam("distro", "referenceapplication:2.13.0");
        addTaskParam("dir", "referenceapplication");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("referenceapplication/docker-compose.yml");
        assertFilePresent("referenceapplication/docker-compose.override.yml");
        assertFilePresent("referenceapplication/docker-compose.prod.yml");
        assertFilePresent("referenceapplication/.env");
        assertFilePresent("referenceapplication/web/Dockerfile");
        assertFilePresent("referenceapplication/web/openmrs-distro.properties");
        assertFilePresent("referenceapplication/web/openmrs_core/openmrs.war");
        assertFileContains("war.openmrs=2.5.9", "referenceapplication", "web", "openmrs-distro.properties");
        assertFilePresent("referenceapplication/web/openmrs_modules");
        assertNumFilesPresent(42, Paths.get("referenceapplication", "web", "openmrs_modules"), null, ".omod");
        assertFilePresent("referenceapplication/web/openmrs_owas");
        assertFilePresent("referenceapplication/web/openmrs_owas/SystemAdministration.owa");

        assertSuccess();
    }

    @Test
    public void buildDistro_shouldBuildFromRefapp3xArtifact() throws Exception {
        addTaskParam("distro", "referenceapplication:3.0.0");
        addTaskParam("dir", "referenceapplication");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("referenceapplication/docker-compose.yml");
        assertFilePresent("referenceapplication/docker-compose.override.yml");
        assertFilePresent("referenceapplication/docker-compose.prod.yml");
        assertFilePresent("referenceapplication/.env");
        assertFilePresent("referenceapplication/web/Dockerfile");
        assertFilePresent("referenceapplication/web/openmrs-distro.properties");
        assertFilePresent("referenceapplication/web/openmrs_core/openmrs.war");
        assertFileContains("war.openmrs=2.6.7", "referenceapplication", "web", "openmrs-distro.properties");
        assertFilePresent("referenceapplication/web/openmrs_modules");
        assertNumFilesPresent(24, Paths.get("referenceapplication", "web", "openmrs_modules"), null, ".omod");
        assertFileContains("omod.spa", "referenceapplication", "web", "openmrs-distro.properties");
        assertFilePresent("referenceapplication/web/openmrs_owas");
        assertNumFilesPresent(0, Paths.get("referenceapplication", "web", "openmrs_owas"), null, null);

        assertSuccess();
    }

    @Test
    public void testBundledBuildDistro() throws Exception {
        addTaskParam("distro", "referenceapplication:2.3.1");
        addTaskParam("dir", "referenceapplication-bundled");
        addTaskParam("bundled", "true");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("referenceapplication-bundled/web/Dockerfile");
        assertFilePresent("referenceapplication-bundled/docker-compose.yml");
        assertFilePresent("referenceapplication-bundled/docker-compose.override.yml");
        assertFilePresent("referenceapplication-bundled/docker-compose.prod.yml");
        assertFilePresent("referenceapplication-bundled/.env");
        assertFilePresent("referenceapplication-bundled/web/setenv.sh");
        assertFilePresent("referenceapplication-bundled/web/startup.sh");
        assertFilePresent("referenceapplication-bundled/web/wait-for-it.sh");
        assertFilePresent("referenceapplication-bundled/web/openmrs.war");
        assertFilePresent("referenceapplication-bundled/web/openmrs-distro.properties");

        assertFileNotPresent("referenceapplication-bundled/web/modules");
        assertFileNotPresent("referenceapplication-bundled/web/WEB-INF");

        assertZipEntryPresent("referenceapplication-bundled/web/openmrs.war",
         "WEB-INF/bundledModules");

        assertSuccess();
    }

    @Test
    public void testBuildDistroWithSpaBuildConfig() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-spa-build.properties");
        addTaskParam("dir", "target");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("target/docker-compose.yml");
        assertFilePresent("target/docker-compose.override.yml");
        assertFilePresent("target/docker-compose.prod.yml");
        assertFilePresent("target/.env");
        assertFilePresent("target/web/Dockerfile");
        assertFilePresent("target/web/setenv.sh");
        assertFilePresent("target/web/startup.sh");
        assertFilePresent("target/web/wait-for-it.sh");
        assertFilePresent("target/web/modules");
        assertFilePresent("target/web/frontend/index.html");
        assertFilePresent("target/web/owa");
        assertFilePresent("target/web/openmrs.war");
        assertFilePresent("target/web/openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void testBuildDistroWithSpaArtifacts() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-spa-artifacts.properties");
        addTaskParam("dir", "target");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");

        assertFilePresent("target/docker-compose.yml");
        assertFilePresent("target/web/openmrs_modules");
        assertFilePresent("target/web/openmrs_spa/index.html");
        assertFilePresent("target/web/openmrs_owas");
        assertFilePresent("target/web/openmrs_core/openmrs.war");
        assertFilePresent("target/web/openmrs-distro.properties");
        assertSuccess();
    }

    @Test
    public void testBuildDistroWithContentPackage() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-content-package.properties");
        addTaskParam("dir", "target");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");
        assertFilePresent( "target", "web", "openmrs_core", "openmrs.war");
        assertFilePresent("target", "web", "openmrs_config", "conceptclasses", "hiv", "conceptclasses.csv");
        assertFilePresent("target", "web", "openmrs_config", "conceptsources", "hiv", "conceptsources.csv");
        assertFilePresent("target", "web", "openmrs_config", "encountertypes", "hiv", "encountertypes.csv");
    }

    @Test
    public void testBuildDistroWithWithContentPackageWithNoNamespace() throws Exception {
        includeDistroPropertiesFile("openmrs-distro-content-package-no-namespace.properties");
        addTaskParam("dir", "target");
        addTaskParam("ignorePeerDependencies", "false");
        executeTask("build-distro");
        assertFilePresent( "target", "web", "openmrs_core", "openmrs.war");
        assertFilePresent("target", "web", "openmrs_config", "conceptclasses", "conceptclasses.csv");
        assertFilePresent("target", "web", "openmrs_config", "conceptsources", "conceptsources.csv");
        assertFilePresent("target", "web", "openmrs_config", "encountertypes", "encountertypes.csv");
    }

    @Test
    public void testBuildDistroWithMissingContentDependencies() throws Exception {
        setupContentPackage("testpackage1");

        includeDistroPropertiesFile("openmrs-distro-content-package-missing-dependencies.properties");
        addTaskParam("dir", "distro");
        addTaskParam("ignorePeerDependencies", "false");
        Exception exception = null;
        try {
            executeTask("build-distro");
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertLogContains("requires module org.openmrs.module:fhir2-omod version >=1.8.0");
        assertLogContains("requires module org.openmrs.module:webservices.rest-omod version >=2.42.0");
        assertLogContains("requires module org.openmrs.module:openconceptlab-omod version >=2.3.0");
        assertLogContains("requires module org.openmrs.module:initializer-omod version >=2.5.2");
        assertLogContains("requires module org.openmrs.module:o3forms-omod version >=2.2.0");
        assertLogContains("requires esm @openmrs/esm-patient-chart-app version >=8.1.0");
    }
}
