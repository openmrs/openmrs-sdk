package org.openmrs.maven.plugins.model;

import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.ServerConfig;

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
    private String interactiveMode;
    private String version;
    private ServerConfig serverConfig;
    private boolean includeDemoData;

    public static class ServerBuilder {
        private Server server = new Server();

        public ServerBuilder(ServerConfig serverConfig){
            server.setServerId(serverConfig.getParam(SDKConstants.PROPERTY_SERVER_ID));
            server.setVersion(serverConfig.getParam(SDKConstants.PROPERTY_PLATFORM));
            server.setDbDriver(serverConfig.getParam(SDKConstants.PROPERTY_DB_DRIVER));
            server.setDbUri(serverConfig.getParam(SDKConstants.PROPERTY_DB_URI));
            server.setDbUser(serverConfig.getParam(SDKConstants.PROPERTY_DB_USER));
            server.setDbPassword(serverConfig.getParam(SDKConstants.PROPERTY_DB_PASS));
            server.serverConfig = serverConfig;
        }
        public ServerBuilder(){}

        public ServerBuilder setInteractiveMode(String nestedInteractiveMode) {
            server.interactiveMode = nestedInteractiveMode;
            return this;
        }

        public ServerBuilder setVersion(String version) {
            server.version = version;
            return this;
        }

        public ServerBuilder setServerId(String serverId) {
            server.serverId = serverId;
            return this;
        }

        public ServerBuilder setDbDriver(String DbDriver) {
            server.dbDriver = DbDriver;
            return this;
        }

        public ServerBuilder setDbUri(String DbUri) {
            server.dbUri = DbUri;
            return this;
        }

        public ServerBuilder setDbUser(String DbUser) {
            server.dbUser = DbUser;
            return this;
        }

        public ServerBuilder setDbPassword(String DbPassword) {
            server.dbPassword = DbPassword;
            return this;
        }

        public ServerBuilder setDemoData(boolean is) {
            server.includeDemoData = is;
            return this;
        }

        public Server build() {
            return server;
        }
    }

    private Server() {};

    public ServerConfig getServerConfig(){
        return serverConfig;
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

    public String getInteractiveMode() { return interactiveMode; }

    public void setInteractiveMode(String interactiveMode) { this.interactiveMode = interactiveMode; }

    public String getVersion() { return version; }

    public void setVersion(String version) { this.version = version; }

    public boolean isIncludeDemoData() {
        return includeDemoData;
    }

    public void setIncludeDemoData(boolean includeDemoData) {
        this.includeDemoData = includeDemoData;
    }
}
