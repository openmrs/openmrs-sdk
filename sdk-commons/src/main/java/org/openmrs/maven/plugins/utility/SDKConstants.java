package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class for handling static values
 */
public class SDKConstants {
    public static final String SDK_STATS_ENABLED_QUESTION = "Would you be willing to help us improve SDK by sending us once in a while anonymous " +
            "usage statistics (you can always change your mind by going to sdk-stats.properties and setting statsEnabled to false)";
    // dependency plugin
    public static final String DEPENDENCY_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    public static final String DEPENDENCY_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";
    public static final String DEPENDENCY_PLUGIN_VERSION = "3.2.0";
    // archetype plugin
    public static final String ARCHETYPE_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    public static final String ARCHETYPE_PLUGIN_ARTIFACT_ID = "maven-archetype-plugin";
    public static final String ARCHETYPE_PLUGIN_VERSION = "3.2.0";
    //help plugin
    public static final String HELP_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    public static final String HELP_PLUGIN_ARTIFACT_ID = "maven-help-plugin";
    public static final String HELP_PLUGIN_VERSION = "3.2.0";
    //docker plugin
    public static final String PLUGIN_DOCKER_ARTIFACT_ID = "openmrs-sdk-docker-maven-plugin";
    // frontend plugin
    public static final String FRONTEND_PLUGIN_GROUP_ID = "com.github.eirslett";
    public static final String FRONTEND_PLUGIN_ARTIFACT_ID = "frontend-maven-plugin";
    public static final String FRONTEND_PLUGIN_VERSION = "1.11.3";
    // exec maven plugin
    public final static String EXEC_PLUGIN_GROUP_ID = "org.codehaus.mojo";
    public final static String EXEC_PLUGIN_ARTIFACT_ID = "exec-maven-plugin";
    public final static String EXEC_PLUGIN_VERSION = "1.5.0";
    //archetypes artifactId
    public static final String REFAPP_ARCH_ARTIFACT_ID = "openmrs-sdk-archetype-module-refapp";
    public static final String PLATFORM_ARCH_ARTIFACT_ID = "openmrs-sdk-archetype-module-platform";
    public static final String CONTENT_PACKAGE_ARCH_ARTIFACT_ID = "openmrs-sdk-archetype-module-content";
    // tomcat plugin
    public static final String OPENMRS_TOMCAT_PLUGIN_GROUP_ID = "org.openmrs.maven.plugins";
    public static final String OPENMRS_TOMCAT7_PLUGIN_ARTIFACT_ID = "openmrs-sdk-tomcat7-maven-plugin";
    public static final String OPENMRS_TOMCAT9_PLUGIN_ARTIFACT_ID = "openmrs-sdk-tomcat9-maven-plugin";
    // default path to projects
    public static final String OPENMRS_SERVER_PATH = "openmrs";
    public static final String OPENMRS_SERVER_PROPERTIES = "openmrs-server.properties";
    public static final String OPENMRS_SDK_JDK_OPTION = "Which JDK would you like to use to run this server?";
    public static final String OPENMRS_SDK_JDK_CUSTOM = "Custom JDK path";
    public static final String OPENMRS_SDK_JDK_CUSTOM_INVALID = "JDK path is invalid";
    public static final String OPENMRS_SDK_PROPERTIES = "sdk.properties";
    public static final String OPENMRS_SERVER_MODULES = "modules";
    public static final String OPENMRS_SERVER_CONFIGURATION = "configuration";
    // properties names
    public static final String OPENMRS_SDK_PROPERTIES_JAVA_HOME_OPTIONS = "javaHomeOptions";
    // dbUri for different db
    public static final String URI_MYSQL = "jdbc:mysql://localhost:3306/@DBNAME@";
    public static final String URI_MYSQL_DOCKER = "jdbc:mysql://%s:3306/@DBNAME@";
    public static final String URI_POSTGRESQL = "jdbc:postgresql://localhost:5432/@DBNAME@";
    public static final String URI_H2 = "jdbc:h2:@APPLICATIONDATADIR@/database/@DBNAME@;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=-1";
    // dbDriver class for different db
    public static final String DRIVER_MYSQL_OLD = "com.mysql.jdbc.Driver";
    public static final String DRIVER_MYSQL = " com.mysql.cj.jdbc.Driver";
    public static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    public static final String DRIVER_H2 = "org.h2.Driver";
    //db Artifact data
    public static final Artifact H2_ARTIFACT = new Artifact("h2", "1.4.190", Artifact.GROUP_H2, Artifact.TYPE_JAR);
    // default settings path
    public static final String MAVEN_SETTINGS = "settings.xml";
    public static final String DB_NAME_TEMPLATE = "openmrs-%s";

    // non-platform web app versions
    public static final String WEBAPP_ARTIFACT_ID = "openmrs-webapp";
    public static final String PLATFORM_ARTIFACT_ID = "platform";
    public static final Map<String,String> WEBAPP_VERSIONS = new HashMap<String, String>() {{
        put("2.0", "1.9.7");
        put("2.1", "1.10.0");
        put("2.2", "1.11.2");
    }};
    public static final String SETUP_DEFAULT_VERSION = "2.3";
    public static final String SETUP_DEFAULT_PLATFORM_VERSION = "1.11.5";

    public static final String REFAPP_2X_GROUP_ID = "org.openmrs.distro";
    public static final String REFAPP_2X_ARTIFACT_ID = "referenceapplication-package";
    public static final String REFAPP_2X_TYPE = "jar";

    public static final String REFAPP_3X_GROUP_ID = "org.openmrs";
    public static final String REFAPP_3X_ARTIFACT_ID = "distro-emr-configuration";
    public static final String REFAPP_3X_TYPE = "zip";

    public static final List<String> SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER = java.util.Arrays.asList("2.3.1", "2.2", "2.1");
    private final static String[] SUPPORTED_MODULE_EXTENSIONS = new String[]{Artifact.TYPE_WAR, Artifact.TYPE_JAR, Artifact.TYPE_OMOD};

    public static final String REFAPP_DISTRO = "referenceapplication-distro";

    public static final String DISTRO_PROPERTIES_NAME = "openmrs-distro.properties";
    public static final String DISTRO_PROPERTIES_NAME_SHORT = "distro.properties";

    // version keywords
    public static final String LATEST_VERSION_BATCH_KEYWORD = "LATEST";
    public static final String LATEST_SNAPSHOT_BATCH_KEYWORD = "LATEST-SNAPSHOT";

    public final static String NPM_VERSION = "^6.14.13";
    public final static String NODE_VERSION = "^14.17.0";
	public static final String RESET_SEARCH_INDEX_SQL = "DELETE FROM global_property WHERE property = 'search.indexVersion';";

	/**
     * Get core modules with required versions
     * @param version - openmrs version
     * @return
     */
    public static List<Artifact> getCoreModules(String version, boolean isPlatform) {
        final String webAppVersion = isPlatform ? version : WEBAPP_VERSIONS.get(version);
        if (webAppVersion == null) return null;
        return new ArrayList<Artifact>() {{
            add(new Artifact("openmrs-webapp", webAppVersion, Artifact.GROUP_WEB, Artifact.TYPE_WAR));
        }};
    }

    /**
     * Get module for downloading distro
     * @param version - distro (module) version
     * @return
     */
    public static Artifact getReferenceModule(String version) {
        Artifact artifact = new Artifact("referenceapplication-package", version, Artifact.GROUP_DISTRO, Artifact.TYPE_ZIP);
        artifact.setClassifier("distro");
        return artifact;
    }

    public static Artifact getDistroModule(String groupId, String artifactId, String version) {
        return new Artifact(artifactId, version, groupId);
    }

    public static Artifact getSDKInfo() throws MojoExecutionException {
        Properties properties = PropertiesUtils.loadPropertiesFromResource(SDKConstants.OPENMRS_SDK_PROPERTIES);
        return new Artifact(properties.getProperty("artifactId"), properties.getProperty("version"), properties.getProperty("groupId"));
    }
}
