package org.openmrs.maven.plugins.model;

/**
 * Class for Server model
 */
public class Server {
    private String serverId;
    private String dbPath;
    private String dbDriver;
    private String dbUri;
    private String dbUser;
    private String dbPassword;

    public Server() {};

    public Server(String serverId, String dbPath, String dbDriver, String dbUri, String dbUser, String dbPassword) {
        this.serverId = serverId;

        this.dbPath = dbPath;
        this.dbDriver = dbDriver;
        this.dbUri = dbUri;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public String getDbUri() {
        return dbUri;
    }

    public void setDbUri(String dbUri) {
        this.dbUri = dbUri;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
