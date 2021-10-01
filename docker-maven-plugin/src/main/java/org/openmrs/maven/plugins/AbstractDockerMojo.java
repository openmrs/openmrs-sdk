package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

abstract class AbstractDockerMojo extends AbstractMojo {

    protected static final String DEFAULT_MYSQL_CONTAINER = "openmrs-sdk-mysql-v3-2";
    protected static final String DEFAULT_MYSQL_EXPOSED_PORT = "3308";
    protected static final String DEFAULT_MYSQL_PASSWORD = "Admin123";
    protected static final String MYSQL_5_6 = "mysql:5.6";
    protected static final String DEFAULT_MYSQL_DB_URI = "jdbc:mysql://localhost:" + DEFAULT_MYSQL_EXPOSED_PORT + "/";
    protected static final String API_VERSION = "1.18";

    private static final String NOT_LINUX_UNABLE_TO_CONNECT_MESSAGE = "\n\n\nCould not connect to Docker at " +
            "%s\n\n Please make sure Docker is running.\n\n If you are using 'Docker Toolbox', " +
            "please make sure you run the SDK command\n from the 'Docker  Toolbox' terminal.\n\n" +
            "If your Docker is running, try resetting the Docker host by running setup with -DdockerHost parameter.\n\n" +
            "You can also set it manually by adding -DdockerHost=\"tcp://correct/url\"";

    private static final String LINUX_UNABLE_TO_CONNECT_MESSAGE = "\n\n\nCould not connect to Docker at %s.\n\n" +
            " If the Docker host URL is not correct, please reset it by running setup with the -DdockerHost" +
            " parameter\n or set it manually by adding -DdockerHost=\"tcp://correct/url\"";

    /**
     * Option to override default dockerHost
     *
     * @parameter  property="dockerHost"
     */
    protected String dockerHost;

    protected DockerClient docker;

    @Override
    public void execute() throws MojoExecutionException {
        resolveDocker();
        executeTask();
        try {
            docker.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to close Docker client");
        }
    }

    public abstract void executeTask() throws MojoExecutionException;

    protected void resolveDocker() throws MojoExecutionException {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(API_VERSION);

        if (StringUtils.isNotBlank(dockerHost)) {
            try {
                new URI(dockerHost);
            } catch (URISyntaxException e) {
                throw new MojoExecutionException("Docker host \"" + dockerHost + "\" is not a valid URI.", e);
            }

            configBuilder.withDockerHost(dockerHost);
        }

        DockerClientConfig config = configBuilder.build();

        docker = DockerClientBuilder.getInstance(config).withDockerHttpClient(
                new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).build()
        ).build();

        try {
            docker.infoCmd().exec();
        } catch (Exception e) {
            if (SystemUtils.IS_OS_LINUX) {
                throw new MojoExecutionException(String.format(LINUX_UNABLE_TO_CONNECT_MESSAGE, dockerHost), e);
            }
            else {
                throw new MojoExecutionException(String.format(NOT_LINUX_UNABLE_TO_CONNECT_MESSAGE, dockerHost), e);
            }
        }
    }

    protected Container findContainer(String id){
        List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();

        for (Container container : containers) {
            if (container.getId().equals(id)) {
                return container;
            } else {
                List<String> containerNames = Arrays.asList(container.getNames());
                // on Linux name is prepended with '/'
                if (containerNames.contains(id) || containerNames.contains("/" + id)) {
                    return container;
                } else if (container.getLabels().containsKey(id)) {
                    return container;
                }
            }
        }

        return null;
    }

    protected void showMessage(String message) {
        System.out.println("\n" + message);
    }
}


