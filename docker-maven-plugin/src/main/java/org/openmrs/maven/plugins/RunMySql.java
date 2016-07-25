package org.openmrs.maven.plugins;

import com.github.dockerjava.api.model.Container;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.sql.DriverManager;
import java.util.List;

import static org.openmrs.maven.plugins.CreateMySql.DEFAULT_MYSQL_EXPOSED_PORT;

/**
 * @goal run-mysql
 * @requiresProject false
 */
public class RunMySql extends AbstractDockerMojo {

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter expression="${containerId}"
     */
    protected String containerId;

    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter expression="${port}"
     */
    protected String port;

    /**
     * username to connect with db
     *
     * @parameter expression="${username}"
     */
    protected String username;

    /**
     * password to connect with db
     *
     * @parameter expression="${password}"
     */
    protected String password;

    @Override
    public void executeTask() throws MojoExecutionException, MojoFailureException {
        Container container = null;

        if (StringUtils.isNotBlank(containerId)) {
            container = findContainerById(containerId);
        } else {
            container = findContainerByLabel(DEFAULT_MYSQL_CONTAINER_ID);
        }

        if (container == null) {
            throw new MojoExecutionException("Failed to start container - container with given id " + containerId + " doesn't exist");
        }

        if (!container.getStatus().contains("Up")) {
            docker.startContainerCmd(container.getId()).exec();

            if (StringUtils.isBlank(port)) port = DEFAULT_MYSQL_EXPOSED_PORT;
            if (StringUtils.isBlank(username)) username = "root";
            if (StringUtils.isBlank(password)) password = DEFAULT_MYSQL_EXPOSED_PORT;

            //wait until MySQL is ready for connections, usually takes miliseconds,
            //but if there is automatically created connection after start-up, it may be refused
            long start = System.currentTimeMillis();
            showMessage("\nInitializing MySQL DB...\n");
            while (System.currentTimeMillis() - start < 30000) {
                try {
                    DriverManager.getConnection("jdbc:mysql://localhost:" + port + "/", username, password);
                    //breaks only if connection is established
                    showMessage("\nFinished initializing MySQL DB\n");
                    return;
                } catch (Exception e) {
                    //do nothing, iterate again
                }
            }
            throw new MojoExecutionException("Failed to initialize MySQL DB in container");
        }
    }
}
