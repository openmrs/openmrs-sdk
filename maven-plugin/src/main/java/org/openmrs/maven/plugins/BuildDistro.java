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

    public static final String DISTRIBUTION_VERSION_PROMPT = "You can build the following versions of distribution";

    public static final String DUMP_PREFIX = "CREATE DATABASE IF NOT EXISTS `openmrs`;\n\n USE `openmrs`;\n\n";

    public static final String DB_DUMP_PATH = "dbdump" + File.separator + "dump.sql";

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

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        File buildDirectory = getBuildDirectory();

        Server server = getDefaultServer();
        server.setServerDirectory(buildDirectory);
        server.save();

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

        moduleInstaller.installModules(distroProperties.getWarArtifacts(), targetDirectory.getAbsolutePath());
        renameWebApp(targetDirectory);

        moduleInstaller.installModules(distroProperties.getModuleArtifacts(), targetDirectory.getAbsolutePath()+File.separator+"modules");

        wizard.showMessage("Creating Docker Compose configuration...\n");
        String distroName = adjustImageName(distroProperties.getName());
        String distroVersion = adjustImageName(distroProperties.getVersion());
        writeDockerCompose(targetDirectory, distroName, distroVersion);
        copyBuildDistroResource("setenv.sh", new File(targetDirectory, "setenv.sh"));
        copyBuildDistroResource("startup.sh", new File(targetDirectory, "startup.sh"));
        copyBuildDistroResource("wait-for-it.sh", new File(targetDirectory, "wait-for-it.sh"));
        copyDockerfile(targetDirectory, distroProperties);

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
            copyBuildDistroResource("Dockerfile-jre7", new File(targetDirectory, "Dockerfile"));
        } else {
            copyBuildDistroResource("Dockerfile-jre8", new File(targetDirectory, "Dockerfile"));
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
        URL composeUrl = getClass().getClassLoader().getResource(DOCKER_COMPOSE_PATH);
        if(composeUrl == null){
            throw new RuntimeException("Failed to find file '"+DOCKER_COMPOSE_PATH+"' in classpath");
        }
        File compose = new File(targetDirectory, "docker-compose.yml");
        try(InputStream inputStream = composeUrl.openStream();FileWriter composeWriter = new FileWriter(compose)){
            String content = IOUtils.toString(inputStream);
            content = content.replaceAll("<distro>", distro);
            composeWriter.write(content);
        } catch (IOException|NullPointerException e/*don't check if url is not null, because same error handling*/) {
            throw new RuntimeException("Failed to write docker-compose.yml file", e);
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
        URL resourceUrl = getClass().getClassLoader().getResource("build-distro/" + resource);
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

    private Server getDefaultServer(){
        Server server = new Server.ServerBuilder().build();
        server.setDbDriver("com.mysql.jdbc.Driver");
        server.setDbName("openmrs");
        server.setDbPassword("Admin123");
        server.setDbUser("root");
        server.setDbUri("jdbc:mysql://mysql:3306/openmrs?autoReconnect=true&sessionVariables=storage_engine=InnoDB&useUnicode=true&characterEncoding=UTF-8");
        server.setIncludeDemoData(false);
        server.setParam("install_method", "auto");
        server.setParam("create_tables", "false");
        server.setParam("create_database_user", "false");
        server.setParam("has_current_openmrs-database", "true");
        server.setParam("auto_update_database", "false");
        server.setParam("module_web_admin", "true");
        server.setParam("admin_user_password", "Admin123");
        return server;
    }
}
