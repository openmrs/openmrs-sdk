package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
@Ignore
public class FetchIntegrationTest extends AbstractSdkIntegrationTest{

    private final static String FETCH_DIRECTORY_NAME = "fetch";

    private final static String BATCH_MODE_RESULT_FILENAME_MODULE_OWA = "owa-1.5.omod";
    private final static String BATCH_MODE_ARTIFACT_ID_MODULE_OWA = "owa";
    private final static String BATCH_MODE_VERSION_MODULE_OWA = "1.5";

    private final static String BATCH_MODE_RESULT_FILENAME_OWA_CONCEPTDICTIONARY = "conceptdictionary-1.0.0.zip";
    private final static String BATCH_MODE_NAME_OWA_CONCEPTDICTIONARY = "conceptdictionary";
    private final static String BATCH_MODE_VERSION_OWA_CONCEPTDICTIONARY = "1.0.0";

    private final static String INTERACTIVE_MODE_RESULT_FILENAME_WEBSERVICES = "webservices.rest-2.16.omod";
    private final static String ANSWER_PROJECT_TYPE = "Module";
    private final static String ANSWER_ARTIFACT_ID = "webservices.rest";
    private final static String ANSWER_PROJECT_VERSION = "2.16";

    @Before
    public void createFetchDirectory() {
        File fetchDirectory = new File(testDirectory.getAbsolutePath() + "/" + FETCH_DIRECTORY_NAME);
        addTaskParam("dir", fetchDirectory.getAbsolutePath());
    }

    @Test
    public void fetch_shouldDownloadProjectInBatchModeWithVersionSpecifiedInProjectName() throws Exception{
        addTaskParam("artifactId", BATCH_MODE_ARTIFACT_ID_MODULE_OWA + ":" + BATCH_MODE_VERSION_MODULE_OWA);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + BATCH_MODE_RESULT_FILENAME_MODULE_OWA);
    }

    @Test
    public void fetch_shouldDownloadProjectInBatchModeWithVersionSpecifiedInVersion() throws Exception{
        addTaskParam("owa", BATCH_MODE_NAME_OWA_CONCEPTDICTIONARY);
        addTaskParam("version", BATCH_MODE_VERSION_OWA_CONCEPTDICTIONARY);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + BATCH_MODE_RESULT_FILENAME_OWA_CONCEPTDICTIONARY);
    }

    @Test
    public void fetch_shouldDownloadProjectInInteractiveMode() throws Exception{
        addAnswer(ANSWER_PROJECT_TYPE);
        addAnswer(ANSWER_ARTIFACT_ID);
        addAnswer(ANSWER_PROJECT_VERSION);
        executeTask("fetch");
        assertSuccess();
        assertFilePresent(FETCH_DIRECTORY_NAME + "/" + INTERACTIVE_MODE_RESULT_FILENAME_WEBSERVICES);
    }
}
