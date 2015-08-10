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
    private String interactiveMode;
    private String version;
    private String filePath;
    private boolean includeDemoData;

    public static class ServerBuilder {
        private String nestedServerId;
        private String nestedDbDriver;
        private String nestedDbUri;
        private String nestedDbUser;
        private String nestedDbPassword;
        private String nestedInteractiveMode;
        private String nestedVersion;
        private String nestedFilePath;
        private boolean nestedDemoData;

        public ServerBuilder setInteractiveMode(String nestedInteractiveMode) {
            this.nestedInteractiveMode = nestedInteractiveMode;
            return this;
        }

        public ServerBuilder setVersion(String nestedVersion) {
            this.nestedVersion = nestedVersion;
            return this;
        }

        public ServerBuilder setServerId(String nestedServerId) {
            this.nestedServerId = nestedServerId;
            return this;
        }

        public ServerBuilder setDbDriver(String nestedDbDriver) {
            this.nestedDbDriver = nestedDbDriver;
            return this;
        }

        public ServerBuilder setDbUri(String nestedDbUri) {
            this.nestedDbUri = nestedDbUri;
            return this;
        }

        public ServerBuilder setDbUser(String nestedDbUser) {
            this.nestedDbUser = nestedDbUser;
            return this;
        }

        public ServerBuilder setDbPassword(String nestedDbPassword) {
            this.nestedDbPassword = nestedDbPassword;
            return this;
        }

        public ServerBuilder setFilePath(String nestedFilePath) {
            this.nestedFilePath = nestedFilePath;
            return this;
        }

        public ServerBuilder setDemoData(boolean is) {
            this.nestedDemoData = is;
            return this;
        }

        public Server build() {
            return new Server(nestedServerId, nestedVersion, nestedDbDriver, nestedDbUri, nestedDbUser, nestedDbPassword, nestedFilePath, nestedDemoData, nestedInteractiveMode);
        }
    }

    private Server() {};

    private Server(String serverId, String version, String dbDriver, String dbUri, String dbUser, String dbPassword, String filePath, boolean demoData, String interactiveMode) {
        this.serverId = serverId;
        this.version = version;
        this.dbDriver = dbDriver;
        this.dbUri = dbUri;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.filePath = filePath;
        this.includeDemoData = demoData;
        this.interactiveMode = interactiveMode;
    }

    public String getFilePath() { return filePath; }

    public void setFilePath(String filePath) { this.filePath = filePath; }

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
