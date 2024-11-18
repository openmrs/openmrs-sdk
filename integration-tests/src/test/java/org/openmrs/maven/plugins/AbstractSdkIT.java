package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingFileOrDirectory;
import static org.junit.Assert.assertNotNull;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersionInDisstro;
import static org.openmrs.maven.plugins.SdkMatchers.hasPlatformVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasWarVersion;


@RunWith(BlockJUnit4ClassRunner.class)
public abstract class AbstractSdkIT {

    /**
     * contains name of directory in project's target dir, where integration tests are conducted
     */
    static final String TEST_DIRECTORY = "integration-test";
    static final String MOJO_OPTION_TMPL = "-D%s=\"%s\"";
    protected static final String BATCH_ANSWERS = "batchAnswers";

    protected final ArrayDeque<String> batchAnswers = new ArrayDeque<>();

    /**
     * maven utility for integration tests
     */
    Verifier verifier;

    /**
     * test directory, contains mock files and files created during tests
     */
    File testDirectory;

    Path testDirectoryPath;

    File distroFile;

    public String resolveSdkArtifact() throws MojoExecutionException {
        Properties sdk = new Properties();
        try (InputStream sdkPom = getClass().getClassLoader().getResourceAsStream("sdk.properties")) {
            sdk.load(sdkPom);
        }
        catch (IOException e) {
           throw new MojoExecutionException(e.getMessage());
        }
        return sdk.get("groupId")+":"+sdk.get("artifactId")+":"+sdk.get("version");
    }

    void includeTestResource(String fileName) throws Exception {
        File source = getTestFile(TEST_DIRECTORY, fileName);
        File target = new File(testDirectory, fileName);
        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, testDirectory);
        }
        else {
            FileUtils.copyFile(source, target);
        }
    }

    void includeDistroPropertiesFile(String fileName) throws Exception {
        File source = getTestFile(TEST_DIRECTORY, fileName);
        File target = new File(testDirectory, DistroProperties.DISTRO_FILE_NAME);
        FileUtils.copyFile(source, target);
    }

    void addTestResources() throws Exception {
        includeTestResource("pom.xml");
        includeDistroPropertiesFile(DistroProperties.DISTRO_FILE_NAME);
    }

    @Before
    public void setup() throws Exception {
        String tempDirPath = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
        String executionDirName = "openmrs-sdk-" + getClass().getSimpleName() + "-" + UUID.randomUUID();
        testDirectoryPath = Paths.get(tempDirPath, executionDirName);
        testDirectory = testDirectoryPath.toFile();
        if (!testDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create test directory: " + testDirectory);
        }
        ResourceExtractor.extractResourcePath(getClass(), "/" + TEST_DIRECTORY, testDirectory, true);
        addTestResources();
        verifier = new Verifier(testDirectory.getAbsolutePath());
        verifier.setAutoclean(false);
        addTaskParam("openMRSPath", testDirectory.getAbsolutePath());
        distroFile = new File(testDirectory, DistroProperties.DISTRO_FILE_NAME);
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(testDirectory);
        cleanAnswers();
    }

    public String setupTestServer() throws Exception{
        Verifier setupServer = new Verifier(testDirectory.getAbsolutePath());
        String serverId = UUID.randomUUID().toString();

        addTaskParam(setupServer, "openMRSPath", testDirectory.getAbsolutePath());
        addTaskParam(setupServer, "distro", "referenceapplication:2.2");
        addTaskParam(setupServer, "debug", "1044");
        addMockDbSettings(setupServer);

        addAnswer(serverId);
        addAnswer(System.getProperty("java.home"));
        addAnswer("8080");
        addTaskParam(setupServer, BATCH_ANSWERS, getAnswers());
        cleanAnswers();

        String sdk = resolveSdkArtifact();
        setupServer.executeGoal(sdk + ":setup");
        assertFilePresent(serverId, "openmrs-server.properties");
        new File(testDirectory, "log.txt").delete();
        return serverId;
    }

    private void cleanAnswers() {
        batchAnswers.clear();
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
     * Clears all parameters to execute Mojo
     */
    public void clearParams(){
        verifier.setCliOptions(new ArrayList<String>());
    }

    /**
     * executes given goal in openmrs-sdk-maven-plugin
     * @param goal to be executed
     * @throws VerificationException
     */
    public void executeTask(String goal) throws Exception {
        addTaskParam(BATCH_ANSWERS, getAnswers());
        addTaskParam("testMode", "true");
        String sdk = resolveSdkArtifact();
        verifier.executeGoal(sdk+":"+goal);
    }

    public File getLogFile(){
        return new File(testDirectory, "log.txt");
    }

    protected void assertPlatformUpdated(String serverId, String version) throws MojoExecutionException {
        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, hasPlatformVersion(version));
        assertThat(server.getDistroProperties(), hasWarVersion(version));
    }

    protected void assertModuleUpdated(String serverId, String artifactId, String version) throws MojoExecutionException {
        Server.setServersPath(testDirectory.getAbsolutePath());
        Server server = Server.loadServer(serverId);
        assertThat(server, hasModuleVersion(artifactId, version));
        assertThat(server.getDistroProperties(), hasModuleVersionInDisstro(artifactId, version));
    }

    public File getTestFile(String file) {
        return testDirectoryPath.resolve(file).toFile();
    }

    public File getTestFile(String... paths) {
        Path resolvedPath = testDirectoryPath;
        for (String path : paths) {
            resolvedPath = resolvedPath.resolve(path);
        }
        return getTestFile(resolvedPath);
    }

    public File getTestFile(Path path) {
        return testDirectoryPath.resolve(path).toFile();
    }

    /**
     * asserts that file with given path is present in test directory
     */
    public void assertFilePresent(String path) {
        Path resolvedPath = testDirectoryPath.resolve(path).toAbsolutePath();
        assertPathPresent(resolvedPath);
    }

    public void assertFilePresent(String... paths) {
        Path resolvedPath = testDirectoryPath.toAbsolutePath();
        for (String path : paths) {
            resolvedPath = resolvedPath.resolve(path);
        }

        assertPathPresent(resolvedPath);
    }

    public void assertFileContains(String text, String... paths) throws IOException {
        Path resolvedPath = testDirectoryPath.toAbsolutePath();
        for (String path : paths) {
            resolvedPath = resolvedPath.resolve(path);
        }
        String jsContents = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
        assertThat(jsContents, Matchers.containsString(text));
    }

    public void assertPathPresent(Path path) {
        assertThat("Expected " + path + " to be an existing file or directory",
                path.toFile(), anExistingFileOrDirectory());
    }
    /**
     * asserts that file with given path is not present in test directory
     */
    public void assertFileNotPresent(String path) {
        Path resolvedPath = testDirectoryPath.resolve(path).toAbsolutePath();
        assertPathNotPresent(resolvedPath);
    }

    public void assertFileNotPresent(String... paths) {
        Path resolvedPath = testDirectoryPath.toAbsolutePath();
        for (String path : paths) {
            resolvedPath = resolvedPath.resolve(path);
        }

        assertPathNotPresent(resolvedPath);
    }

    public void assertPathNotPresent(Path path) {
        assertThat("Expected " + path + " to not exist",
                path.toFile(), not(anExistingFileOrDirectory()));
    }

    public void assertZipEntryPresent(String path, String zipEntryName) throws Exception {
        File file = new File(testDirectory.getAbsolutePath(), path);
        ZipFile zipFile = new ZipFile(file);
        assertNotNull(zipFile.getEntry(zipEntryName));
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
    protected void assertServerInstalled(String serverId) {
        assertFilePresent(serverId);
        assertFilePresent(serverId, SDKConstants.OPENMRS_SERVER_PROPERTIES);
    }

    protected void assertModulesInstalled(String serverId, String... filenames){
        Path modulesRoot = testDirectoryPath.resolve(Paths.get(serverId, "modules"));
        for(String filename : filenames){
            assertPathPresent(modulesRoot.resolve(filename));
        }
    }

    protected void assertModulesInstalled(String serverId, DistroProperties distroProperties) {
        List<Artifact> modules = distroProperties.getModuleArtifacts();
        String[] moduleFilenames = new String[modules.size()];

        for (int i = 0; i < modules.size(); i ++) {
            moduleFilenames[i] = modules.get(i).getDestFileName();
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
        addTaskParam(verifier, "dbSql", "null");
    }

    protected static Model getTestPom(File directory) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileInputStream(new File(directory, "pom.xml")));
    }

    protected static void saveTestPom(File directory, Model model) throws IOException {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileOutputStream(new File(directory, "pom.xml")), model);
    }

    protected void addAnswer(String answer){
        batchAnswers.add(answer);
    }

    protected String getAnswers() {
        String answers = StringUtils.join(batchAnswers.iterator(), ",");
        answers = StringUtils.removeEnd(answers, "]");
        answers = StringUtils.removeStart(answers, "[");
        return answers;
    }
}
