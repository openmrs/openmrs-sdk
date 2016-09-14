package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class FetchIntegrationTest extends AbstractSdkIntegrationTest{

    private final static String FETCH_DIRECTORY_NAME = "fetch";

    private final static String BATCH_MODE_RESULT_FILENAME_OWA_MODULE = "owa-1.4-SNAPSHOT.omod";
    private final static String BATCH_MODE_NAME_OWA_MODULE = "owa";
    private final static String BATCH_MODE_VERSION_OWA_MODULE = "1.4.eeec2f";

    private final static String BATCH_MODE_RESULT_FILENAME_CONCEPTDICTIONARY = "conceptdictionary-1.0.0.zip";
    private final static String BATCH_MODE_NAME_OWA_CONCEPTDICTIONARY = "conceptdictionary";
    private final static String BATCH_MODE_VERSION_OWA_CONCEPTDICTIONARY = "1.0.0";

    private final static String INTERACTIVE_MODE_RESULT_FILENAME_WEBSERVICES = "webservices.rest-2.14-SNAPSHOT.b4ebc0.omod";
    private final static String ANSWER_PROJECT_TYPE = "Module";
    private final static String ANSWER_PROJECT_NAME = "openmrs-module-webservices.rest";
    private final static String ANSWER_PROJECT_VERSION = "2.14.b4ebc0";

    @Before
    public void createFetchDirectory() {
        File fetchDirectory = new File(testDirectory.getAbsolutePath() + "/" + FETCH_DIRECTORY_NAME);
        addTaskParam("dir", fetchDirectory.getAbsolutePath());
    }

    @Test
    public void fetch_shouldDownloadProjectInBatchModeWithVersionSpecifiedInProjectName() throws Exception{
        addTaskParam("artifactId", BATCH_MODE_NAME_OWA_MODULE + ":" + BATCH_MODE_VERSION_OWA_MODULE);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + BATCH_MODE_RESULT_FILENAME_OWA_MODULE);
    }

    @Test
    public void fetch_shouldDownloadProjectInBatchModeWithVersionSpecifiedInVersion() throws Exception{
        addTaskParam("owa", BATCH_MODE_NAME_OWA_CONCEPTDICTIONARY);
        addTaskParam("version", BATCH_MODE_VERSION_OWA_CONCEPTDICTIONARY);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + BATCH_MODE_RESULT_FILENAME_CONCEPTDICTIONARY);
    }

    @Test
    public void fetch_shouldDownloadProjectInInteractiveMode() throws Exception{
        addAnswer(ANSWER_PROJECT_TYPE);
        addAnswer(ANSWER_PROJECT_NAME);
        addAnswer(ANSWER_PROJECT_VERSION);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + INTERACTIVE_MODE_RESULT_FILENAME_WEBSERVICES);
    }
}
