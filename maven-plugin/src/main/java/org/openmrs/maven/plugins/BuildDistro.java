package org.openmrs.maven.plugins;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DistroHelper;
import org.openmrs.maven.plugins.utility.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 *  @goal build-distro
 *  @requiresProject false
 */
public class BuildDistro extends AbstractTask {

    public static final String DEFAULT_SQL_DUMP = Server.CLASSPATH_SCRIPT_PREFIX + "openmrs-platform.sql";

    public static final String OPENMRS_WAR = "openmrs.war";

    public static final String DOCKER_COMPOSE_PATH = "build-distro/docker-compose.yml";

    public static final String README_PATH = "build-distro/README.md";

    public static final String DISTRIBUTION_VERSION_PROMPT = "You can build the following versions of distribution";

    public static final String DUMP_PREFIX = "CREATE DATABASE IF NOT EXISTS `openmrs`;\n\n USE `openmrs`;\n\n";

    public static final String DB_DUMP_PATH = "dbdump" + File.separator + "dump.sql";

    public static final String WAR_FILE_MODULES_DIRECTORY_NAME = "bundledModules";

    /**
     * @parameter expression="${distro}"
     */
    private String distro;

	/**
	 * @parameter expression="${dir}"
     */
    private String dir;

    /**
     * @parameter expression="${dbSql}"
     */
    private String dbSql;

    /**
     * @parameter expression="${bundled}" default-value="false"
     */
    private boolean bundled;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        File buildDirectory = getBuildDirectory();

        File userDir = new File(System.getProperty("user.dir"));

        Artifact distroArtifact = null;
        DistroProperties distroProperties = null;

        if (distro == null) {
            File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);
            if(distroFile.exists()){
                wizard.showMessage("Building distribution from the distro file at " + distroFile + "...\n");
                distroProperties = new DistroProperties(distroFile);
            } else if (Project.hasProject(userDir)) {
                Project config = Project.loadProject(userDir);
                distroArtifact = DistroHelper.parseDistroArtifact(config.getGroupId()+":"+config.getArtifactId()+":"+config.getVersion());

                wizard.showMessage("Building distribution from the source at " + userDir + "...\n");
                new Build(this).cleanInstallServerProject(userDir);
                distroFile = distroHelper.extractFileFromDistro(buildDirectory, distroArtifact, DistroProperties.DISTRO_FILE_NAME);

                if(distroFile.exists()){
                    distroProperties = new DistroProperties(distroFile);
                } else {
                    wizard.showMessage("Couldn't find " + DistroProperties.DISTRO_FILE_NAME + " in " + distroArtifact);
                }
            }
        } else if (StringUtils.isNotBlank(distro)){
            distroProperties = distroHelper.retrieveDistroProperties(distro);
        }

        if (distroProperties == null){
            Server server = new Server.ServerBuilder().build();

            wizard.promptForRefAppVersionIfMissing(server, versionsHelper, DISTRIBUTION_VERSION_PROMPT);
            if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
                distroProperties = new DistroProperties(server.getVersion());
            } else {
                distroProperties = distroHelper.downloadDistroProperties(buildDirectory, server);
                distroArtifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
            }
        }

        if (distroProperties == null) {
            throw new IllegalArgumentException("The distro you specified '" + distro + "' could not be retrieved");
        }

        String distroName = buildDistro(buildDirectory, distroArtifact, distroProperties);

        wizard.showMessage("The '" + distroName + "' distribution created! To start up the server run 'docker-compose up' from " + buildDirectory.getAbsolutePath() + "\n");
    }

    private File getBuildDirectory() {
        final File targetDir;
        if (StringUtils.isBlank(dir)) {
            String directory = wizard.promptForValueIfMissingWithDefault("Specify build directory for generated files (-Ddir, default: 'docker')", dir, "dir", "docker");

            targetDir = new File(directory);

            if (targetDir.exists()) {
                if (targetDir.isDirectory()) {
                    if (targetDir.list().length != 0) {
                        wizard.showMessage("The directory at '" + targetDir.getAbsolutePath() + "' is not empty. All its content will be lost.");
                        boolean chooseDifferent = wizard.promptYesNo("Would you like to choose a different directory?");
                        if (chooseDifferent) {
                            return getBuildDirectory();
                        }
                    }
                } else {
                    wizard.showMessage("The specified path '" + dir + "' is not a directory.");
                    return getBuildDirectory();
                }
            }

            dir = directory;
        } else {
            targetDir = new File(dir);
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        } else {
            try {
                FileUtils.cleanDirectory(targetDir);
            } catch (IOException e) {
                throw new IllegalStateException("Could not clean up directory", e);
            }
        }

        return targetDir;
    }

    private String buildDistro(File targetDirectory, Artifact distroArtifact, DistroProperties distroProperties) throws MojoExecutionException {
        InputStream dbDumpStream;
        wizard.showMessage("Downloading modules...\n");

        String distroName = adjustImageName(distroProperties.getName());
        File dockerImageDir = new File(targetDirectory, distroName + "-docker-image");

        moduleInstaller.installModules(distroProperties.getWarArtifacts(), dockerImageDir.getAbsolutePath());
        renameWebApp(dockerImageDir);

        if (bundled) {
            try {
                ZipFile warfile = new ZipFile(new File(dockerImageDir, OPENMRS_WAR));
                File tempDir = new File(dockerImageDir, "WEB-INF");
                moduleInstaller.installModules(distroProperties.getModuleArtifacts(),
                        new File(tempDir, WAR_FILE_MODULES_DIRECTORY_NAME).getAbsolutePath());
                ZipParameters parameters = new ZipParameters();
                warfile.addFolder(tempDir, parameters);
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to remove " + tempDir.getName() + " file", e);
                }
            } catch (ZipException e) {
                throw new RuntimeException("Failed to bundle modules into *.war file", e);
            }
        }
        else {
            moduleInstaller.installModules(distroProperties.getModuleArtifacts(),
                    new File(dockerImageDir, "modules").getAbsolutePath());
        }

        wizard.showMessage("Creating Docker Compose configuration...\n");
        String distroVersion = adjustImageName(distroProperties.getVersion());
        writeDockerCompose(targetDirectory, distroName, distroVersion);
        writeReadme(targetDirectory, distroName, distroVersion);
        copyBuildDistroResource("setenv.sh", new File(dockerImageDir, "setenv.sh"));
        copyBuildDistroResource("startup.sh", new File(dockerImageDir, "startup.sh"));
        copyBuildDistroResource("wait-for-it.sh", new File(dockerImageDir, "wait-for-it.sh"));
        copyDockerfile(dockerImageDir, distroProperties);
        distroProperties.saveTo(dockerImageDir);

        dbDumpStream = getSqlDumpStream(StringUtils.isNotBlank(dbSql) ? dbSql : distroProperties.getSqlScriptPath(), targetDirectory, distroArtifact);
        if(dbDumpStream != null) {
            copyDbDump(targetDirectory, dbDumpStream);
        }
        //clean up extracted sql file
        cleanupSqlFiles(targetDirectory);

//        createZipFile(targetDirectory, distroName+"-"+distroVersion);
        return distroName;
    }

    private void createZipFile(File targetDirectory, String zipFileName) {
        try {
            ZipFile zipFile = new ZipFile(new File(targetDirectory.getParentFile(), zipFileName+".zip"));
            ZipParameters parameters = new ZipParameters();
            parameters.setIncludeRootFolder(false);
            parameters.setRootFolderInZip(zipFileName);
            zipFile.addFolder(targetDirectory, parameters);
        } catch (ZipException e) {
            throw new RuntimeException("Failed to create zip file", e);
        }
    }

    private void copyDockerfile(File targetDirectory, DistroProperties distroProperties) {
        int majorVersion = new Version(distroProperties.getPlatformVersion()).getMajorVersion();
        if(majorVersion == 1){
            if (bundled) {
                copyBuildDistroResource("Dockerfile-jre7-bundled", new File(targetDirectory, "Dockerfile"));
            }
            else {
                copyBuildDistroResource("Dockerfile-jre7", new File(targetDirectory, "Dockerfile"));
            }
        } else {
            if (bundled) {
                copyBuildDistroResource("Dockerfile-jre8-bundled", new File(targetDirectory, "Dockerfile"));
            }
            else {
                copyBuildDistroResource("Dockerfile-jre8", new File(targetDirectory, "Dockerfile"));
            }
        }
    }

    /**
     * name of sql dump file is unknown, so wipe all files with 'sql' extension
     */
    private void cleanupSqlFiles(File targetDirectory) {
        File[] sqlFiles = targetDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql");
            }
        });
        for(File sql : sqlFiles){
            FileUtils.deleteQuietly(sql);
        }
    }

    private void writeDockerCompose(File targetDirectory, String distro, String version) {
        writeTemplatedFile(targetDirectory, distro, version, DOCKER_COMPOSE_PATH, "docker-compose.yml");
    }

    private void writeReadme(File targetDirectory, String distro, String version) {
        writeTemplatedFile(targetDirectory, distro, version, README_PATH, "README.md");
    }

    private void writeTemplatedFile(File targetDirectory, String distro, String version,
    String path, String filename) {
        URL composeUrl = getClass().getClassLoader().getResource(path);
        if(composeUrl == null){
            throw new RuntimeException("Failed to find file '"+ path + "' in classpath");
        }
        File compose = new File(targetDirectory, filename);
        try(InputStream inputStream = composeUrl.openStream();FileWriter composeWriter = new FileWriter(compose)){
            String content = IOUtils.toString(inputStream);
            content = content.replaceAll("<distro>", distro);
            composeWriter.write(content);
        } catch (IOException|NullPointerException e/*don't check if url is not null, because same error handling*/) {
            throw new RuntimeException("Failed to write " + filename + " file", e);
        }
    }

    private String adjustImageName(String part){
        return part.replaceAll("\\s+","").toLowerCase();
    }

    private void copyDbDump(File targetDirectory, InputStream stream) throws MojoExecutionException {
        File dbdump = new File(targetDirectory, DB_DUMP_PATH);
        try {
            dbdump.getParentFile().mkdirs();
            dbdump.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SQL dump file");
        }
        try(FileWriter writer = new FileWriter(dbdump)) {
            writer.write(DUMP_PREFIX);

            int c;
            while((c = stream.read()) != -1){
                writer.write(c);
            }
            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create dump file", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private InputStream getSqlDumpStream(String sqlScriptPath, File targetDirectory, Artifact distroArtifact) throws MojoExecutionException {
        InputStream stream = null;

        if(sqlScriptPath == null){
            //import default dump if no sql script specified
            sqlScriptPath = DEFAULT_SQL_DUMP;
        }

        try{
            if(sqlScriptPath.startsWith(Server.CLASSPATH_SCRIPT_PREFIX)){
                String sqlScript = sqlScriptPath.replace(Server.CLASSPATH_SCRIPT_PREFIX, "");
                URL resourceUrl = getClass().getClassLoader().getResource(sqlScript);
                if (resourceUrl != null) {
                    stream = resourceUrl.openStream();
                } else {
                    if(distroArtifact != null && distroArtifact.isValid()){
                        File extractedSqlFile =  distroHelper.extractFileFromDistro(targetDirectory, distroArtifact, sqlScript);
                        stream = new FileInputStream(extractedSqlFile);
                    }
                }
            } else {
                File scriptFile = new File(sqlScriptPath);
                if(scriptFile.exists()){
                    stream = new FileInputStream(scriptFile);
                } else {
                    throw new IllegalArgumentException("Invalid db script: "+sqlScriptPath);
                }
            }
        }catch(IOException e){
            throw new RuntimeException("Failed to open stream to sql dump script");
        }
        return stream;
    }

    private void copyBuildDistroResource(String resource, File target) {
        URL resourceUrl = getClass().getClassLoader().getResource("build-distro/docker-image/" + resource);
        try {
            FileUtils.copyURLToFile(resourceUrl, target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file from classpath: "+resourceUrl+" to "+target.getAbsolutePath());
        }
    }

    private void renameWebApp(File targetDirectory) {
        File openmrsWar = new File(targetDirectory, OPENMRS_WAR);
        File[] warFiles = targetDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".war");
            }
        });
        for (File file: warFiles) {
            System.out.println("file:" + file.getAbsolutePath());
        }
        System.out.println("target:" + targetDirectory);
        if(warFiles != null && warFiles.length == 1){
            boolean renameSuccess = warFiles[0].renameTo(openmrsWar);
            if(!renameSuccess){
                throw new RuntimeException("Failed to rename openmrs '.war' file");
            }
        } else {
            throw new RuntimeException("Distro should contain single war file");
        }
    }
}
