package org.openmrs.maven.plugins.utility;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Used to represent a dependency that is required by a distribution
 */
@Data
@AllArgsConstructor
public class MissingDependency {
    private String dependentComponent;
    private String requiredType;
    private String requiredComponent;
    private  String requiredVersion;
    private String currentVersion;
}
