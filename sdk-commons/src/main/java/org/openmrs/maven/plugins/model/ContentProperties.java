package org.openmrs.maven.plugins.model;

import java.util.Properties;

/**
 * Represents a `content.properties` file that is included within a Content Package
 */
public class ContentProperties extends BaseSdkProperties {

    public ContentProperties(Properties properties) {
        this.properties = properties;
    }

}
