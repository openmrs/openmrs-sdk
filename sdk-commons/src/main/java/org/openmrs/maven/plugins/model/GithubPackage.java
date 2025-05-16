package org.openmrs.maven.plugins.model;

import lombok.Data;

/**
 * Represents a GitHub Package configuration as defined within a properties file
 */
@Data
public class GithubPackage {
    private String username;
    private String token;
    private String owner;
    private String repository;
    private String artifactKey;

}
