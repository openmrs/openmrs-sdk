package org.openmrs.maven.plugins.git;

public class GithubPrRequest {
    /**
     * credentials:username
     */
    private final String username;
    /**
     * credentials:password
     */
    private final String password;
    /**
     * body of description in PR
     */
    private final String description;
    /**
     * title of PR
     */
    private final String title;
    /**
     * The name of the branch where your changes are implemented.
     * For cross-repository pull requests in the same network, namespace head with a user like this: username:branch.
     */
    private final String head;
    /**
     * The name of the branch you want your changes pulled into. This should be an existing branch on the current repository.
     */
    private final String base;
    /**
     * Openmrs repository, to which PR will be sent
     */
    private final String repository;

    private GithubPrRequest(String username, String password, String description, String title, String head, String base, String repository) {
        this.username = username;
        this.password = password;
        this.description = description;
        this.title = title;
        this.head = head;
        this.base = base;
        this.repository = repository;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getHead() {
        return head;
    }

    public String getBase() {
        return base;
    }

    public String getRepository() {
        return repository;
    }

    public static class Builder {
        private String username;
        private String password;
        private String description;
        private String title;
        private String head;
        private String base;
        private String repository;

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setHead(String head) {
            this.head = head;
            return this;
        }

        public Builder setBase(String base) {
            this.base = base;
            return this;
        }

        public Builder setRepository(String repository) {
            this.repository = repository;
            return this;
        }

        public GithubPrRequest execute() {
            return new GithubPrRequest(username, password, description, title, head, base, repository);
        }
}
}
