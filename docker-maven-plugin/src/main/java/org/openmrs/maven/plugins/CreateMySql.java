package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @goal create-mysql
 * @requiresProject false
 */
public class CreateMySql extends AbstractDockerMojo {

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter expression="${port}"
     */
    protected String port;

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter expression="${container}"
     */
    protected String container;

    /**
     * password for root user of mysql
     *
     * @parameter expression="${rootPassword}"
     */
    protected String rootPassword;

    /**
     * path to directory of data volume
     *
     * @parameter expression="${dataVolume}"
     */
    protected String dataVolume;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {

        if (StringUtils.isBlank(port)) port = DEFAULT_MYSQL_EXPOSED_PORT;
        if (StringUtils.isBlank(dataVolume)) dataVolume = prompt("Please specify the path to directory with data volume");
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
        List<Image> mysql = docker.listImagesCmd().withImageNameFilter(MYSQL_5_6).exec();
        return mysql.size() == 0;
    }

    private void createMysqlContainer(DockerClient docker) {
        if (container == null) container = DEFAULT_MYSQL_CONTAINER;

        ExposedPort tcp3306 = ExposedPort.tcp(3306);
        Ports portBindings = new Ports();
        portBindings.bind(tcp3306, new Ports.Binding("localhost", port));

        Map<String, String> labels = new HashMap<>();
        labels.put(container, "true");

        if(SystemUtils.IS_OS_WINDOWS){
            prepareVolumeWindowsPath();
        }

        Volume volume = new Volume("/var/lib/mysql");

        docker.createContainerCmd(MYSQL_5_6)
                .withExposedPorts(tcp3306)
                .withPortBindings(portBindings)
                .withName(container)
                .withEnv("MYSQL_ROOT_PASSWORD="+rootPassword)
                .withVolumes(volume)
                .withBinds(new Bind(dataVolume, volume))
                .withAttachStdout(true)
                .withLabels(labels)
                .exec();
    }

    private void prepareVolumeWindowsPath() {
        dataVolume =  "//"+dataVolume.replace("\\", "/").replace(":", "");
    }

    private void pullMySqlImage(DockerClient docker) {
        final CountDownLatch latch = new CountDownLatch(1);
        docker.pullImageCmd("mysql")
                .withTag("5.6")
                .exec(new ResultCallback<PullResponseItem>() {
                    @Override
                    public void onStart(Closeable closeable) {
                        showMessage("Started downloading mysql:5.6 image ...");
                    }

                    @Override
                    public void onNext(PullResponseItem object) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {
                        showMessage("Finished!");
                        latch.countDown();
                    }

                    @Override
                    public void close() throws IOException {

                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to create mysql container", e);
        }
    }
}
