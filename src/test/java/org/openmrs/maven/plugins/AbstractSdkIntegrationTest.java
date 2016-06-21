package org.openmrs.maven.plugins;

import com.google.common.collect.Lists;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public abstract class AbstractSdkIntegrationTest {

    /**
     * contains name of directory in project's target dir, where integration tests are conducted
     */
    static final String TEST_DIRECTORY = "/integration-test";
    static final String MOJO_OPTION_TMPL = "-D%s=%s";

    /**
     * contains files in test directory which are not created during tests and will not be cleaned up
     */
    List<File> testFilesToPersist;
    /**
     * maven utility for integration tests
     */
    Verifier verifier;
    /**
     * test directory, contains mock files and files created during tests
     */
    File testDirectory;

    @Before
    public void setUp() throws Exception{
        testDirectory = ResourceExtractor.simpleExtractResources(getClass(), TEST_DIRECTORY);
        verifier = new Verifier(testDirectory.getAbsolutePath());

        testFilesToPersist = new ArrayList<>(Arrays.asList(testDirectory.listFiles()));

        verifier.addCliOption("-o");
        addMojoParam("interactiveMode","false");
        addMojoParam("openMRSPath",testDirectory.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        for(File file : testDirectory.listFiles()){
            if(!testFilesToPersist.contains(file)){
                FileUtils.deleteDirectory(file);
            }
        }
    }

    /**
     * Adds CLI parameter to executed Mojo in format -D'param'='value'
     * @param param name of parameter, eg. "serverId"
     * @param value value of parameter
     */
    public void addMojoParam(String param, String value){
        verifier.addCliOption(String.format(MOJO_OPTION_TMPL, param, value));
    }

    /**
     * executes given goal in openmrs-sdk-maven-plugin
     * @param goal to be executed
     * @throws VerificationException
     */
    public void executeTask(String goal) throws VerificationException {
        verifier.executeGoal("org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:2.1.3-SNAPSHOT:"+goal);
    }

    public File getLogFile(){
        return new File(testDirectory, "log.txt");
    }

    /**
     * asserts that file with given path is present in test directory
     */
    public void assertFilePresent(String path){
        verifier.assertFilePresent(testDirectory.getAbsolutePath()+"/"+path);
    }
    /**
     * asserts that file with given path is not present in test directory
     */
    public void assertFileNotPresent(String path){
        verifier.assertFileNotPresent(testDirectory.getAbsolutePath()+"/"+path);
    }

    /**
     * check whether build ended with success
     */
    protected void assertSuccess() throws IOException {
        File log = getLogFile();
        String logContent = FileUtils.fileRead(log);
        assertThat(logContent, containsString("[INFO] BUILD SUCCESS"));
    }

    /**
     * check if server dir and installation.properties are created
     */
    protected void assertServerInstalled(String serverId) throws Exception{
        assertFilePresent(serverId);
        assertFilePresent(serverId+"/installation.properties");
    }

    protected void assertModulesInstalled(String serverId, String... filenames){
        assertModulesInstalled(serverId, Lists.newArrayList(filenames));
    }

    private void assertModulesInstalled(String serverId, List<String> filenames){
        for(String filename : filenames){
            assertFilePresent(serverId+"/modules/"+filename);
        }
    }

    protected void assertModulesInstalled(String serverId, DistroProperties distroProperties) {
        List<Artifact> modules = distroProperties.getModuleArtifacts();
        List<String> moduleFilenames = new ArrayList<>();

        for(Artifact module : modules){
            moduleFilenames.add(module.getDestFileName());
        }
        assertModulesInstalled(serverId, moduleFilenames);
    }
}
