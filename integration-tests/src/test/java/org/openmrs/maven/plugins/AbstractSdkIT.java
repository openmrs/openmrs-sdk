package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
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
import org.openmrs.maven.plugins.utility.PropertiesUtils;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingFileOrDirectory;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasModuleVersionInDisstro;
import static org.openmrs.maven.plugins.SdkMatchers.hasPlatformVersion;
import static org.openmrs.maven.plugins.SdkMatchers.hasWarVersion;

@RunWith(BlockJUnit4ClassRunner.class)
public abstract class AbstractSdkIT {

    public final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * contains name of directory in project's target dir, where integration tests are conducted
     */
    protected static int counter = 0;
    protected static final String TEST_DIRECTORY = "integration-test";
    protected static final String MOJO_OPTION_TMPL = "-D%s=\"%s\"";
    protected static final String BATCH_ANSWERS = "batchAnswers";
    protected final ArrayDeque<String> batchAnswers = new ArrayDeque<>();

    /**
     * maven utility for integration tests
     */
    protected Verifier verifier;

    /**
     * test directory, contains mock files and files created during tests
     */
    protected File testDirectory;
    protected Path testDirectoryPath;
    protected File distroFile;
    protected Path testBaseDir;
    protected Path testResourceDir;
    protected File nodeCacheDir;
    protected boolean deleteTestArtifacts = false;

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

    protected void includeDistroPropertiesFile(String name, String version, Properties properties) throws Exception {
        File outputFile = testDirectoryPath.resolve(DistroProperties.DISTRO_FILE_NAME).toFile();
        properties.setProperty("name", name);
        properties.setProperty("version", version);
        PropertiesUtils.savePropertiesChangesToFile(properties, outputFile);
    }

    protected void includeDistroPropertiesFile(String... paths) throws Exception {
        Path sourcePath = testResourceDir.resolve(TEST_DIRECTORY);
        for (String path : paths) {
            sourcePath = sourcePath.resolve(path);
        }
        Path targetPath = testDirectoryPath.resolve(DistroProperties.DISTRO_FILE_NAME);
        FileUtils.copyFile(sourcePath.toFile(), targetPath.toFile());
    }

    protected void includePomFile(String... paths) throws Exception {
        Path sourcePath = testResourceDir.resolve(TEST_DIRECTORY);
        for (String path : paths) {
            sourcePath = sourcePath.resolve(path);
        }
        Path targetPath = testDirectoryPath.resolve("pom.xml");
        FileUtils.copyFile(sourcePath.toFile(), targetPath.toFile());
    }

    protected void addTestResources() throws Exception {
        includePomFile("pom.xml");
    }

    @Before
    public void setup() throws Exception {
        Path classesPath = Paths.get(Objects.requireNonNull(getClass().getResource("/")).toURI());
        testBaseDir = classesPath.resolveSibling("integration-test-base-dir");
        nodeCacheDir = testBaseDir.resolve("node-cache").toFile();
        testResourceDir = testBaseDir.resolve("test-resources");
        testDirectoryPath = testBaseDir.resolve(getClass().getSimpleName() + "_" + nextCounter());
        testDirectory = testDirectoryPath.toFile();
        FileUtils.deleteQuietly(testDirectory);
        if (!testDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create test directory: " + testDirectory);
        }
        addTestResources();
        verifier = new Verifier(testDirectory.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setSystemProperty("nodeCacheDir", nodeCacheDir.getAbsolutePath());
        addTaskParam("openMRSPath", testDirectory.getAbsolutePath());
        distroFile = new File(testDirectory, DistroProperties.DISTRO_FILE_NAME);
        deleteTestArtifacts = Boolean.parseBoolean(System.getProperty("deleteTestArtifacts"));
    }

    @After
    public void teardown() {
        verifier.resetStreams();
        if (deleteTestArtifacts) {
            log.debug("Removing test artifacts: " + testDirectory);
            FileUtils.deleteQuietly(testDirectory);
        }
        cleanAnswers();
    }

    static synchronized int nextCounter() {
        return counter++;
    }

    public String setupTestServer(String distro) throws Exception {
        Verifier setupServer = new Verifier(testDirectory.getAbsolutePath());
        setupServer.setSystemProperty("nodeCacheDir", nodeCacheDir.getAbsolutePath());
        String serverId = UUID.randomUUID().toString();
        try {
            addTaskParam(setupServer, "openMRSPath", testDirectory.getAbsolutePath());
            addTaskParam(setupServer, "distro", distro);
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
        }
        finally {
            verifier.resetStreams();
        }
        return serverId;
    }

    public void setupContentPackage(String contentPackageDir) throws Exception {
        Path sourcePath = testResourceDir.resolve(TEST_DIRECTORY).resolve("contentpackages").resolve(contentPackageDir);
        Path targetPath = testDirectoryPath.resolve("contentpackages").resolve(contentPackageDir);
        FileUtils.copyDirectory(sourcePath.toFile(), targetPath.toFile());
        Verifier setupContentPackage = new Verifier(targetPath.toFile().getAbsolutePath());
        setupContentPackage.setSystemProperty("nodeCacheDir", nodeCacheDir.getAbsolutePath());
        try {
            addAnswer("y");
            addTaskParam(setupContentPackage, BATCH_ANSWERS, getAnswers());
            setupContentPackage.executeGoal(resolveSdkArtifact() + ":build");
        }
        finally {
            cleanAnswers();
            verifier.resetStreams();
        }
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
     * asserts the number of files with the given prefix and suffix are present in the given directory
     */
    public void assertNumFilesPresent(int numExpected, Path path, String prefix, String suffix) {
        File directory = testDirectoryPath.resolve(path).toFile();
        File[] files = directory.listFiles((dir1, name) -> (prefix == null || name.startsWith(prefix)) && (suffix == null || name.endsWith(suffix)));
        int numFound = (files == null ? 0 : files.length);
        assertThat(numFound, equalTo(numExpected));
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

    protected void assertModulesInstalled(String serverId, String... filenames) {
        Path modulesRoot = testDirectoryPath.resolve(Paths.get(serverId, "modules"));
        for(String filename : filenames) {
            assertPathPresent(modulesRoot.resolve(filename));
        }
    }

    protected void assertOnlyModulesInstalled(String serverId, DistroProperties distroProperties) {
        List<Artifact> modules = distroProperties.getModuleArtifacts();
        String[] moduleFilenames = new String[modules.size()];

        for (int i = 0; i < modules.size(); i ++) {
            moduleFilenames[i] = modules.get(i).getDestFileName();
        }

        Path modulesRoot = testDirectoryPath.resolve(Paths.get(serverId, "modules"));
        File[] expectedFiles = modulesRoot.toFile().listFiles(f -> f.getName().endsWith(".omod"));
        assertNotNull(expectedFiles);
        assertThat(expectedFiles.length, equalTo(moduleFilenames.length));
        for(String filename : moduleFilenames) {
            assertPathPresent(modulesRoot.resolve(filename));
        }
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
        try (InputStream in = Files.newInputStream(new File(directory, "pom.xml").toPath())) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(in);
        }
    }

    protected static void saveTestPom(File directory, Model model) throws IOException {
        try (OutputStream out = Files.newOutputStream(new File(directory, "pom.xml").toPath())) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(out, model);
        }
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

    protected String getLogContents() throws Exception {
        File logFile = new File(testDirectory, "log.txt");
        assertTrue(logFile.exists());
        return FileUtils.readFileToString(logFile, "UTF-8");
    }

    protected void assertLogContains(String message) throws Exception {
        assertTrue(getLogContents().contains(message));
    }

    protected void assertLogDoesNotContain(String message) throws Exception {
        assertFalse(getLogContents().contains(message));
    }
}
