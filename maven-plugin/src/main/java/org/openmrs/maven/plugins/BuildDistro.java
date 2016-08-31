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
     * @parameter expression="${dbSql}"
     */
    private String dbSql;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        File userDir = new File(System.getProperty("user.dir"));
        File targetDirectory = new File(userDir, ".temp");
        targetDirectory.mkdir();

        Server server = getDefaultServer();
        server.setServerDirectory(targetDirectory);
        server.save();

        Artifact distroArtifact = null;
        DistroProperties distroProperties = distro != null ? distroHelper.retrieveDistroProperties(distro) : null;

        File distroFile = new File(userDir, DistroProperties.DISTRO_FILE_NAME);

        if(distroProperties == null && distroFile.exists()){
            boolean useDistro = wizard.promptYesNo("Would you like to use openmrs-distro.properties file from current directory?");
            if(useDistro){
                distroProperties = new DistroProperties(distroFile);
            }
        }

        if (distroProperties == null && Project.hasProject(userDir)) {
            Project config = Project.loadProject(userDir);
            distroArtifact = DistroHelper.parseDistroArtifact(config.getGroupId()+":"+config.getArtifactId()+":"+config.getVersion());
            boolean useProject = wizard.promptYesNo("Would you like to create distro from project ("+distroArtifact+") in current directory?");
            if(useProject){
                wizard.showMessage("Building project...");
                new Build(this).cleanInstallServerProject(userDir);
                distroFile = distroHelper.extractFileFromDistro(targetDirectory, distroArtifact, DistroProperties.DISTRO_FILE_NAME);
                if(distroFile.exists()){
                    distroProperties = new DistroProperties(distroFile);
                } else {
                    throw new IllegalArgumentException("Couldn't find "+DistroProperties.DISTRO_FILE_NAME+" in "+distroArtifact);
                }
            }
        }

        if(distroProperties == null){
            wizard.promptForRefAppVersionIfMissing(server, versionsHelper, DISTRIBUTION_VERSION_PROMPT);
            if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
                distroProperties = new DistroProperties(server.getVersion());
            } else {
                distroProperties = distroHelper.downloadDistroProperties(targetDirectory, server);
                distroArtifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
            }
        }

        if(distroProperties == null) throw new IllegalArgumentException("Distro "+distro+" could not be retrieved");
        String distroName = buildDistro(targetDirectory, distroArtifact, distroProperties);
        targetDirectory.renameTo(new File(targetDirectory.getParent(), distroName));
        wizard.showMessage("Finished!");
    }
    
    private String buildDistro(File targetDirectory, Artifact distroArtifact, DistroProperties distroProperties) throws MojoExecutionException {
        InputStream dbDumpStream;
        wizard.showMessage("Downloading modules...");

        moduleInstaller.installModules(distroProperties.getWarArtifacts(), targetDirectory.getAbsolutePath());
        renameWebApp(targetDirectory);

        moduleInstaller.installModules(distroProperties.getModuleArtifacts(), targetDirectory.getAbsolutePath()+File.separator+"modules");

        wizard.showMessage("Creating Docker Compose configuration...");
        String distroName = adjustImageName(distroProperties.getName());
        String distroVersion = adjustImageName(distroProperties.getServerVersion());
        writeDockerCompose(targetDirectory, distroName, distroVersion);
        copyBuildDistroResource("setenv.sh", new File(targetDirectory, "setenv.sh"));
        copyBuildDistroResource("startup.sh", new File(targetDirectory, "startup.sh"));
        copyBuildDistroResource("wait-for-it.sh", new File(targetDirectory, "wait-for-it.sh"));
        copyDockerfile(targetDirectory, distroProperties);

        dbDumpStream = getSqlDumpStream(StringUtils.isNotBlank(dbSql) ? dbSql : distroProperties.getSqlScriptPath(), targetDirectory, distroArtifact);
        if(dbDumpStream != null){
            copyDbDump(targetDirectory, dbDumpStream);
        }
        //clean up extracted sql file
        cleanupSqlFiles(targetDirectory);

//        createZipFile(targetDirectory, distroName+"-"+distroVersion);
        return distroName+"-"+distroVersion;
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
        //Remember to change this invocation if Adam's PR is merged
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
            content = content.replaceAll("<version>", version);
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
        URL resourceUrl = getClass().getClassLoader().getResource("build-distro" + File.separator + resource);
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
