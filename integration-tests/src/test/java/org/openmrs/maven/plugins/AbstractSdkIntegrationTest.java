package org.openmrs.maven.plugins;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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

    public static String resolveSdkArtifact() throws MojoExecutionException {
        InputStream sdkPom = AbstractSdkIntegrationTest.class.getClassLoader().getResourceAsStream("sdk.properties");
        Properties sdk = new Properties();
        try {
            sdk.load(sdkPom);
        } catch (IOException e) {
           throw new MojoExecutionException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(sdkPom);
        }
        return sdk.get("groupId")+":"+sdk.get("artifactId")+":"+sdk.get("version");
    }

    @Before
    public void setup() throws Exception{
        testDirectory = ResourceExtractor.simpleExtractResources(getClass(), TEST_DIRECTORY);
        verifier = new Verifier(testDirectory.getAbsolutePath());

        testFilesToPersist = new ArrayList<File>(Arrays.asList(testDirectory.listFiles()));

        addTaskParam("interactiveMode","false");
        addTaskParam("openMRSPath",testDirectory.getAbsolutePath());
    }

    @After
    public void teardown() throws Exception {
        for(File file : testDirectory.listFiles()){
            if(!testFilesToPersist.contains(file)){
                FileUtils.deleteDirectory(file);
            }
        }
    }

    public static String setupTestServer() throws Exception{
        File testDir = ResourceExtractor.simpleExtractResources(AbstractSdkIntegrationTest.class, TEST_DIRECTORY);
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        String serverId = UUID.randomUUID().toString();

        addTaskParam(verifier, "serverId", serverId);
        addTaskParam(verifier, "distro", "referenceapplication:2.2");
        addTaskParam(verifier, "openMRSPath", testDir.getAbsolutePath());
        addTaskParam(verifier, "java.home", System.getProperty("java.home"));
        addTaskParam(verifier, "interactiveMode", "false");
        addMockDbSettings(verifier);
        String sdk = resolveSdkArtifact();
        verifier.executeGoal(sdk+":setup");
        return serverId;
    }

    public static void deleteTestServer(String serverId) throws Exception{
        File testDir = ResourceExtractor.simpleExtractResources(AbstractSdkIntegrationTest.class, TEST_DIRECTORY);
        FileUtils.deleteDirectory(testDir.getAbsolutePath()+"/"+serverId);
    }

    /**
     * Adds CLI parameter to executed Mojo in format -D'param'='value'
     * @param param name of parameter, eg. "serverId"
     * @param value value of parameter
     */
    public void addTaskParam(String param, String value){
       addTaskParam(verifier, param, value);
    }

    /**
     * Adds CLI parameter to executed Mojo in format -D'param'='value'
     * @param param name of parameter, eg. "serverId"
     * @param value value of parameter
     */
    public static void addTaskParam(Verifier verifier, String param, String value){
        verifier.addCliOption(String.format(MOJO_OPTION_TMPL, param, value));
    }

    /**
     * executes given goal in openmrs-sdk-maven-plugin
     * @param goal to be executed
     * @throws VerificationException
     */
    public void executeTask(String goal) throws Exception {
        String sdk = resolveSdkArtifact();
        verifier.executeGoal(sdk+":"+goal);
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
    protected void assertSuccess() throws Exception {
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
    }

    /**
     * check if server dir and installation.properties are created
     */
    protected void assertServerInstalled(String serverId) throws Exception{
        assertFilePresent(serverId);
        assertFilePresent(serverId+"/openmrs-server.properties");
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
        List<String> moduleFilenames = new ArrayList<String>();

        for(Artifact module : modules){
            moduleFilenames.add(module.getDestFileName());
        }
        assertModulesInstalled(serverId, moduleFilenames);
    }

    protected void addMockDbSettings() {
        addMockDbSettings(verifier);
    }

    protected static void addMockDbSettings(Verifier verifier) {
        addTaskParam(verifier, "dbDriver", "mysql");
        addTaskParam(verifier, "dbUser", "mysql");
        addTaskParam(verifier, "dbPassword", "mysql");
        addTaskParam(verifier, "dbUri", "@DBNAME@");
    }
}
