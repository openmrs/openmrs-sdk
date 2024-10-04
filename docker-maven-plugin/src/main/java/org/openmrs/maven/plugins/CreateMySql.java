package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.lang.StringUtils;
import com.github.dockerjava.api.command.ListImagesCmd;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * @goal create-mysql
 * @requiresProject false
 */
public class CreateMySql extends AbstractDockerMojo {

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter  property="port"
     */
    protected String port;

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter  property="container"
     */
    protected String container;

    /**
     * password for root user of mysql
     *
     * @parameter  property="rootPassword"
     */
    protected String rootPassword;

    @Override
    public void executeTask() throws MojoExecutionException {
        if (StringUtils.isBlank(port)) port = DEFAULT_MYSQL_EXPOSED_PORT;
        //root password may be blank but not null, if user wants to have empty password
        if (rootPassword == null) rootPassword = DEFAULT_MYSQL_PASSWORD;

        if (noMySqlImage(docker)) {
            pullMySqlImage(docker);
        }

        if (findContainer(DEFAULT_MYSQL_CONTAINER) == null) {
            createMysqlContainer(docker);
        }
    }

    private boolean noMySqlImage(DockerClient docker) {
        ListImagesCmd listImagesCmd = docker.listImagesCmd();
        listImagesCmd.getFilters().put("reference", Collections.singletonList(MYSQL_8_4_1));
        List<Image> mysql = listImagesCmd.exec();
        return mysql.isEmpty();
    }

    private void createMysqlContainer(DockerClient docker) {
        if (container == null) container = DEFAULT_MYSQL_CONTAINER;

        PortBinding portBinding = new PortBinding(new Ports.Binding("localhost", port), ExposedPort.tcp(3306));

        Map<String, String> labels = new HashMap<>();
        labels.put(container, "true");

        Volume volume = new Volume("/var/lib/mysql");
        Bind boundVolume = new Bind(container + "-data", volume);

        HostConfig hostConfig = new HostConfig()
                .withPortBindings(portBinding)
                .withBinds(boundVolume);

        docker.createContainerCmd(MYSQL_8_4_1)
                .withHostConfig(hostConfig)
                .withName(container)
                .withEnv("MYSQL_ROOT_PASSWORD="+rootPassword)
                .withAttachStdout(true)
                .withLabels(labels)
                .exec();
    }

    private void pullMySqlImage(DockerClient docker) throws MojoExecutionException {
        final CountDownLatch latch = new CountDownLatch(1);
        docker.pullImageCmd("mysql")
                .withTag("8.4.1")
                .exec(new ResultCallback<PullResponseItem>() {
                    @Override
                    public void onStart(Closeable closeable) {
                        showMessage("Started downloading mysql:8.4.1 image ...");
                    }

                    @Override
                    public void onNext(PullResponseItem object) {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        showMessage("Error downloading mysql:8.4.1 image : " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        showMessage("Finished!");
                        latch.countDown();
                    }

                    @Override
                    public void close() {

                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Failed to create mysql container " + e.getMessage(), e);
        }
    }
}
