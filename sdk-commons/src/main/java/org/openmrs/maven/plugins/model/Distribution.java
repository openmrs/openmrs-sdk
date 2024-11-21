package org.openmrs.maven.plugins.model;

import lombok.Data;

import java.io.File;

/**
 * Represents on OpenMRS distribution
 */
@Data
public class Distribution {
    private String name; // The name of the distribution
    private String version; // The version of the distribution
    private Distribution parent;  // The parent of this distribution, if any
    private Artifact artifact; // The Maven coordinates where this distribution can be found
    private String artifactPath; // The path within the Maven artifact where the distribution properties are found
    private String resourcePath; // Alternative distribution properties location, classpath resource within the SDK
    private File file; // Alternative distribution properties location, pointing to a file
    private DistroProperties properties; // The distribution properties defined within this distribution, not including parent properties
    private DistroProperties effectiveProperties; // The fully resolved and normalized distribution properties, including parent properties.
}
