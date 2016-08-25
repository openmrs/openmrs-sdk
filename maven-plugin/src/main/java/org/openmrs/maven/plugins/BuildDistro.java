package org.openmrs.maven.plugins;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Version;
import org.openmrs.maven.plugins.utility.DistroHelper;

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


    /**
     * @parameter expression="${distro}"
     */
    private String distro;

    /**
     * DB dump script to import
     *
     * @parameter expression="${dbSql}"
     */
    private String dbSql;


    public static final String DUMP_PREFIX = "CREATE DATABASE IF NOT EXISTS `openmrs`;\n\n USE `openmrs`;\n\n";

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        File targetDirectory = new File(System.getenv("user.dir"), "distro");
        targetDirectory.mkdir();

        Server server = getDefaultServer();
        server.setServerDirectory(targetDirectory);
        server.save();

        DistroProperties distroProperties = distro != null ? distroHelper.retrieveDistroProperties(distro) : null;

        if (distroProperties != null) {
            Artifact distro = DistroHelper.parseDistroArtifact(this.distro);
            server.setVersion(distro.getVersion());
            server.setDistroArtifactId(distro.getArtifactId());
            server.setDistroGroupId(distro.getGroupId());
        } else {
            wizard.promptForRefAppVersionIfMissing(server, versionsHelper);
            if(DistroHelper.isRefapp2_3_1orLower(server.getDistroArtifactId(), server.getVersion())){
                distroProperties = new DistroProperties(server.getVersion());
            } else {
                distroProperties = distroHelper.downloadDistroProperties(targetDirectory, server);
            }
        }

        if(distroProperties == null) throw new IllegalArgumentException("Distro "+distro+" could not be retrieved");

        wizard.showMessage("Downloading modules...");

        moduleInstaller.installCoreModules(server, false, distroProperties);

        wizard.showMessage("Creating Docker Compose configuration...");

        String distroName = adjustImageName(distroProperties.getName());
        String distroVersion = adjustImageName(distroProperties.getServerVersion());
        writeDockerCompose(targetDirectory, distroName, distroVersion);
        copyBuildDistroResource("setenv.sh", new File(targetDirectory, "setenv.sh"));
        //Remember to change this invocation if Adam's PR is merged
        int majorVersion = new Version(distroProperties.getPlatformVersion()).getMajorVersion();
        if(majorVersion == 1){
            copyBuildDistroResource("Dockerfile-jre7", new File(targetDirectory, "Dockerfile"));
        } else {
            copyBuildDistroResource("Dockerfile-jre8", new File(targetDirectory, "Dockerfile"));
        }
        if(dbSql == null) dbSql = distroProperties.getSqlScriptPath();
        resolveAndCopyDbDump(dbSql, targetDirectory, server);
        renameWebApp(targetDirectory);

        wizard.showMessage("Creating zip file...");

        try {
            ZipFile zipFile = new ZipFile(new File(targetDirectory.getParentFile(), distroName+"-"+distroVersion));
            ZipParameters parameters = new ZipParameters();
            parameters.setIncludeRootFolder(false);
            zipFile.addFolder(targetDirectory, parameters);
        } catch (ZipException e) {
            throw new RuntimeException("Failed to create zip file", e);
        }
        FileUtils.deleteQuietly(targetDirectory);

        wizard.showMessage("Finished!");
    }

    private void writeDockerCompose(File targetDirectory, String distro, String version) {
        URL composeUrl = getClass().getClassLoader().getResource("build-distro" + File.separator + "docker-compose.yml");
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

    private void resolveAndCopyDbDump(String sqlScriptPath, File targetDirectory, Server server) throws MojoExecutionException {
        File dbdump = new File(targetDirectory, "dbdump"+File.separator+"dump.sql");
        InputStream stream = null;
        File extractedSqlFile = null;

        try {
            dbdump.getParentFile().mkdirs();
            dbdump.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SQL dump file");
        }
        try(FileWriter writer = new FileWriter(dbdump)) {

            writer.write(DUMP_PREFIX);

            if(sqlScriptPath == null){
                //import default dump if no sql script specified
                sqlScriptPath = DEFAULT_SQL_DUMP;
            }

            if(sqlScriptPath.startsWith(Server.CLASSPATH_SCRIPT_PREFIX)){
                String sqlScript = sqlScriptPath.replace(Server.CLASSPATH_SCRIPT_PREFIX, "");
                URL resourceUrl = getClass().getClassLoader().getResource(sqlScript);
                if (resourceUrl != null) {
                    stream = resourceUrl.openStream();
                } else {
                    Artifact distroArtifact = new Artifact(server.getDistroArtifactId(), server.getVersion(), server.getDistroGroupId(), "jar");
                    extractedSqlFile =  distroHelper.extractFileFromDistro(server.getServerDirectory(), distroArtifact, sqlScript);
                    stream = new FileInputStream(extractedSqlFile);
                }
            } else {
                File scriptFile = new File(sqlScriptPath);
                if(scriptFile.exists()){
                    stream = new FileInputStream(scriptFile);
                } else {
                    throw new IllegalArgumentException("Invalid db script: "+sqlScriptPath);
                }
            }

            int c;
            while((c = stream.read()) != -1){
                writer.write(c);
            }
            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create dump file", e);
        } finally {
            if(stream != null){
                IOUtils.closeQuietly(stream);
            }
            if(extractedSqlFile != null && extractedSqlFile.exists()){
                FileUtils.deleteQuietly(extractedSqlFile);
            }
        }
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
