package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

public class DockerHelper {

    public static String DOCKER_DEFAULT_CONTAINER_ID = "openmrs-sdk-mysql-v3-2";
    public static final String DOCKER_MYSQL_PORT = "3308";
    public static final String DOCKER_MYSQL_USERNAME = "root";
    public static final String DOCKER_MYSQL_PASSWORD = "Admin123";
    private static final String DOCKER_HOST_KEY = "dockerHost";
    public static final String DEFAULT_HOST_LINUX = "unix:///var/run/docker.sock";

    private static final String DOCKER_HOST_MSG_WINDOWS = "To use dockerized MySQL, You have to pass Docker Host URL to SDK. " +
            "You should run SDK from docker-machine command line, so SDK can connect to your Docker. Your individual docker host URL can be obtained by calling command" +
            "'docker-machine url'. Enter it here with prefix 'tcp://', eg: 'tcp://192.168.99.104:2376'"+
            "\nPlease specify %s.";

    private static final String DOCKER_HOST_MSG_LINUX = "To use dockerized MySQL, You have to pass Docker Host URL to SDK. " +
            "Authorization to access docker socket file is required. To ensure this, run command 'sudo usermod -aG docker ${USER}'." +
            "\nPlease specify %s";

    private static final String DOCKER_HOST_DEFAULT_LINUX = "unix:///var/run/docker.sock";

    private MavenProject mavenProject;
    private MavenSession mavenSession;
    private BuildPluginManager pluginManager;
    private Wizard wizard;

    public DockerHelper(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager, Wizard wizard){
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.wizard = wizard;
    }

    private Properties getSdkProperties(){
        File sdkFile = new File(Server.getServersPath(), SDKConstants.OPENMRS_SDK_PROPERTIES);
        Properties sdkProperties = new Properties();
        if(sdkFile.exists()){
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(sdkFile);
                sdkProperties.load(inStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load sdk.properties file from your openmrs directory", e);
            } finally {
                IOUtils.closeQuietly(inStream);
            }
        }
        return sdkProperties;
    }

    private void saveSdkProperties(Properties properties){
        File sdkFile = new File(Server.getServersPath(), SDKConstants.OPENMRS_SDK_PROPERTIES);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(sdkFile);
            properties.store(out, null);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save sdk.properties file from your openmrs directory", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public String getDockerHost(){
        Properties sdkProperties = getSdkProperties();
        return sdkProperties.getProperty(DOCKER_HOST_KEY);
    }

    public void saveDockerHost(String dockerHost){
        Properties sdkProperties = getSdkProperties();
        sdkProperties.setProperty(DOCKER_HOST_KEY, dockerHost);
        saveSdkProperties(sdkProperties);
    }

    public void createMySqlContainer(String container, String port) throws MojoExecutionException {
        String dockerHost = getDockerHost();

        dockerHost = promptForDockerHostIfMissing(dockerHost);

        Artifact sdkInfo = SDKConstants.getSDKInfo();

        wizard.showMessage("Creating '" + container + "' DB docker container...");
        executeMojo(
                plugin(
                        groupId(sdkInfo.getGroupId()),
                        artifactId(SDKConstants.PLUGIN_DOCKER_ARTIFACT_ID),
                        version(sdkInfo.getVersion())
                ),
                goal("create-mysql"),
                configuration(
                        element("dockerHost", dockerHost),
                        element("port", port),
                        element("container", container)),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );

        if(!dockerHost.equals(getDockerHost())){
            saveDockerHost(dockerHost);
        }
    }

    private String promptForDockerHostIfMissing(String dockerHost) {
        if(SystemUtils.IS_OS_LINUX){
            return wizard.promptForValueIfMissingWithDefault(DOCKER_HOST_MSG_LINUX, dockerHost, "docker host", DOCKER_HOST_DEFAULT_LINUX);
        } else {
            return wizard.promptForValueIfMissingWithDefault(DOCKER_HOST_MSG_WINDOWS, dockerHost, "docker host", "");
        }
    }

    public void runDbContainer(String container, String dbUri, String username, String password) throws MojoExecutionException {
        Artifact sdkInfo = SDKConstants.getSDKInfo();
        String dockerHost = getDockerHost();
        dockerHost = promptForDockerHostIfMissing(dockerHost);

        wizard.showMessage("Starting '" + container + "' DB docker container...");
        executeMojo(
                plugin(
                        groupId(sdkInfo.getGroupId()),
                        artifactId(SDKConstants.PLUGIN_DOCKER_ARTIFACT_ID),
                        version(sdkInfo.getVersion())
                ),
                goal("run-db"),
                configuration(
                        element("dockerHost", dockerHost),
                        element("container", container),
                        element("username", username),
                        element("password", password),
                        element("dbUri", dbUri)),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
