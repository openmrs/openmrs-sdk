package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class for handling static values
 */
public class SDKConstants {
    // dependency plugin
    public static final String PLUGIN_DEPENDENCIES_GROUP_ID = "org.apache.maven.plugins";
    public static final String PLUGIN_DEPENDENCIES_ARTIFACT_ID = "maven-dependency-plugin";
    public static final String PLUGIN_DEPENDENCIES_VERSION = "2.8";
    // run plugin
    public static final String PLUGIN_JETTY_GROUP_ID = "org.eclipse.jetty";
    public static final String PLUGIN_JETTY_ARTIFACT_ID = "jetty-maven-plugin";
    public static final String PLUGIN_JETTY_VERSION = "9.0.4.v20130625";
    public static final String DB_NAME_TEMPLATE = "openmrs-%s";
    // archetype
    public static final String ARCH_CATALOG = "http://mavenrepo.openmrs.org/nexus/service/local/repositories/releases/content/archetype-catalog.xml";
    public static final String ARCH_GROUP_ID = "org.apache.maven.plugins";
    public static final String ARCH_ARTIFACT_ID = "maven-archetype-plugin";
    public static final String ARCH_VERSION = "2.3";
    // archetype module options
    public static final String ARCH_MODULE_GROUP_ID = "org.openmrs.maven.archetypes";
    public static final String ARCH_MODULE_ARTIFACT_ID = "maven-archetype-openmrs-module-2.x";
    public static final String ARCH_MODULE_VERSION = "1.2";
    // plugin module wizard
    public static final String WIZARD_GROUP_ID = "org.openmrs.maven.plugins";
    public static final String WIZARD_ARTIFACT_ID = "module-wizard-plugin";
    public static final String WIZARD_VERSION = "1.1.1";
    // default path to projects
    public static final String OPENMRS_MODULE_POM = "pom.xml";
    public static final String OPENMRS_SERVER_PATH = "openmrs";
    public static final String OPENMRS_SERVER_PROPERTIES = "openmrs-server.properties";
    public static final String OPENMRS_SDK_JDK_OPTION = "Which JDK would you like to use to run this server?";
    public static final String OPENMRS_SDK_JDK_CUSTOM = "Custom JDK path";
    public static final String OPENMRS_SDK_JDK_CUSTOM_INVALID = "JDK path is invalid";
    public static final String OPENMRS_SDK_PROPERTIES = "sdk.properties";
    public static final String OPENMRS_SERVER_MODULES = "modules";
    // properties names
    public static final String OPENMRS_SDK_PROPERTIES_JDK_HOME = "jdk.home";
    // dbUri for different db
    public static final String URI_MYSQL = "jdbc:mysql://localhost:3306/@DBNAME@";
    public static final String URI_POSTGRESQL = "jdbc:postgresql://localhost:5740/@DBNAME@";
    public static final String URI_H2 = "jdbc:h2:@APPLICATIONDATADIR@/database/@DBNAME@;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=-1";
    // dbDriver class for different db
    public static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
    public static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    public static final String DRIVER_H2 = "org.h2.Driver";
    //db Artifact data
    public static final Artifact H2_ARTIFACT = new Artifact("h2", "1.4.190", Artifact.GROUP_H2, Artifact.TYPE_JAR);
    // default settings path
    public static final String MAVEN_SETTINGS = "settings.xml";
    public static final String TMP = "tmp";
    // non-platform web app versions
    public static final String WEBAPP_ARTIFACT_ID = "openmrs-webapp";
    public static final Map<String,String> WEBAPP_VERSIONS = new HashMap<String, String>() {{
        put("2.0", "1.9.7");
        put("2.1", "1.10.0");
        put("2.2", "1.11.2");
    }};
    public static final String SETUP_DEFAULT_VERSION = "2.3";
    public static final String SETUP_DEFAULT_PLATFORM_VERSION = "1.11.5";

    public static final String REFERENCEAPPLICATION_ARTIFACT_ID = "referenceapplication-package";
    public static final List<String> SUPPPORTED_REFAPP_VERSIONS_2_3_1_OR_LOWER = java.util.Arrays.asList("2.3.1", "2.2", "2.1");
    private final static String[] SUPPORTED_MODULE_EXTENSIONS = new String[]{Artifact.TYPE_WAR, Artifact.TYPE_JAR, Artifact.TYPE_OMOD};

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

    public static Artifact getSDKInfo() {
    	Properties properties = new Properties();
    	InputStream in = SDKConstants.class.getClassLoader().getResourceAsStream("sdk.properties");
    	try {
			properties.load(in);
			in.close();
			return new Artifact(properties.getProperty("artifactId"), properties.getProperty("version"), properties.getProperty("groupId"));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
    }
    public static boolean isExtensionSupported(String type) {
        return type.equals(SUPPORTED_MODULE_EXTENSIONS[0])
                || type.equals(SUPPORTED_MODULE_EXTENSIONS[1])
                || type.equals(SUPPORTED_MODULE_EXTENSIONS[2]);
    }
}