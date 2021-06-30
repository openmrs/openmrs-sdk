package org.openmrs.maven.plugins;

import com.github.dockerjava.api.model.Container;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.sql.DriverManager;

/**
 * @goal run-db
 * @requiresProject false
 */
public class RunDb extends AbstractDockerMojo {

    public static final String JDBC_MYSQL = "jdbc:mysql://";
    /**
     * port exposed by mysql container to connect with db
     *
     * @parameter property="container"
     */
    protected String container;

    /**
     * uri to connect with db
     *
     * @parameter  property="dbUri"
     */
    protected String dbUri;

    /**
     * username to connect with db
     *
     * @parameter  property="username"
     */
    protected String username;

    /**
     * password to connect with db
     *
     * @parameter  property="password"
     */
    protected String password;

    @Override
    public void executeTask() throws MojoExecutionException {
        Container dbContainer = null;

        if (StringUtils.isNotBlank(container)) {
            dbContainer = findContainer(container);
        }

        if (dbContainer == null) {
            throw new MojoExecutionException("Failed to find the '" + container + "' container. Run `docker ps` to see available containers.");
        }

        if (!dbContainer.getStatus().toLowerCase().contains("up")) {
            docker.startContainerCmd(dbContainer.getId()).exec();

            if (StringUtils.isBlank(dbUri)){
                dbUri = DEFAULT_MYSQL_DBURI;
            }
            dbUri = stripOffDbName(dbUri);

            if (StringUtils.isBlank(username)) username = "root";
            if (StringUtils.isBlank(password)) password = DEFAULT_MYSQL_PASSWORD;

            //wait until MySQL is ready for connections, usually takes miliseconds,
            //but if there is automatically created connection after start-up, it may be refused
            long start = System.currentTimeMillis();
            showMessage("Trying to connect to the DB...");
            while (System.currentTimeMillis() - start < 30000) {
                try {
                    DriverManager.getConnection(dbUri, username, password);
                    //breaks only if connection is established
                    showMessage("Connected to the DB.");
                    return;
                } catch (Exception e) {
                    //do nothing, iterate again
                }
            }
            throw new MojoExecutionException("Failed to connect to the DB in the '" + this.container + "' container at '" + dbUri + "'");
        }
    }

    public String stripOffDbName(String dbUri) {
        int startOfDbName = dbUri.indexOf("/", dbUri.indexOf("//") + 2);
        return dbUri.substring(0, startOfDbName);
    }
}
