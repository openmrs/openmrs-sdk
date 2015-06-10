package org.openmrs.maven.plugins.utility;

import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for handling static values
 */
public class SDKConstants {
    // archetype
    public static final String ARCH_CATALOG = "http://mavenrepo.openmrs.org/nexus/service/local/repositories/releases/content/archetype-catalog.xml";
    public static final String ARCH_GROUP_ID = "org.apache.maven.plugins";
    public static final String ARCH_ARTIFACT_ID = "maven-archetype-plugin";
    public static final String ARCH_VERSION = "2.3";
    // archetype project options
    public static final String ARCH_PROJECT_GROUP_ID = "org.openmrs.maven.archetypes";
    public static final String ARCH_PROJECT_ARTIFACT_ID = "maven-archetype-openmrs-project";
    public static final String ARCH_PROJECT_VERSION = "1.0.1";
    // project options
    public static final String PROJECT_GROUP_ID = "org.openmrs.distro";
    public static final String PROJECT_PACKAGE = "org.openmrs";
    // archetype module options
    public static final String ARCH_MODULE_GROUP_ID = "org.openmrs.maven.archetypes";
    public static final String ARCH_MODULE_ARTIFACT_ID = "maven-archetype-openmrs-module-2.x";
    public static final String ARCH_MODULE_VERSION = "1.1";
    // plugin module wizard
    public static final String WIZARD_GROUP_ID = "org.openmrs.maven.plugins";
    public static final String WIZARD_ARTIFACT_ID = "module-wizard-plugin";
    public static final String WIZARD_VERSION = "1.1.1";
    // default path to projects
    public static final String OPENMRS_SERVER_PATH = "openmrs";
    public static final String OPENMRS_SERVER_PROPERTIES = "server" + File.separator + "installation.h2.properties";
    public static final String OPENMRS_SERVER_POM = "server" + File.separator + "pom.xml";
    // dbUri for different db
    public static final String URI_MYSQL = "jdbc:mysql://localhost:3131";
    public static final String URI_POSTGRESQL = "jdbc:postgresql://localhost:5740";
    public static final String URI_H2 = "jdbc:h2://localhost";
    // dbDriver class for different db
    public static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
    public static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    public static final String DRIVER_H2 = "org.h2.Driver";
    // module base for each version
    public static final List<Artifact> ARTIFACTS_2_0 = new ArrayList<Artifact>() {{
        // each item in constructor: artifactId,destFileName,groupId,type
        add(new Artifact("openmrs-webapp", "openmrs-${openMRSVersion}.war", "org.openmrs.web", "war"));
        add(new Artifact("referencemetadata-omod", "referencemetadata-${referencemetadataModuleVersion}.omod"));
        add(new Artifact("appframework-omod", "appframework-${appframeworkModuleVersion}.omod"));
        add(new Artifact("uiframework-omod", "uiframework-${uiframeworkModuleVersion}.omod"));
        add(new Artifact("logic-omod", "logic-${logicModuleVersion}.omod", "omod"));
        add(new Artifact("registrationcore-omod", "registrationcore-${registrationcoreModuleVersion}.omod"));
        add(new Artifact("registrationapp-omod", "registrationapp-${registrationappModuleVersion}.omod"));
        add(new Artifact("idgen-omod", "idgen-${idgenModuleVersion}.omod"));
        add(new Artifact("emrapi-omod", "emrapi-${emrapiModuleVersion}.omod"));
        add(new Artifact("providermanagement-omod", "providermanagement-${providermanagementModuleVersion}.omod"));
        add(new Artifact("uilibrary-omod", "uilibrary-${uilibraryModuleVersion}.omod"));
        add(new Artifact("uicommons-omod", "uicommons-${uicommonsModuleVersion}.omod"));
        add(new Artifact("referenceapplication-omod", "referenceapplication-${referenceapplicationModuleVersion}.omod"));
        add(new Artifact("calculation-omod", "calculation-${calculationModuleVersion}.omod"));
        add(new Artifact("reporting-omod", "reporting-${reportingModuleVersion}.omod"));
        add(new Artifact("metadatasharing-omod", "metadatasharing-${metadatasharingModuleVersion}.omod"));
        add(new Artifact("metadatamapping-omod", "metadatamapping-${metadatamappingModuleVersion}.omod"));
        add(new Artifact("serialization.xstream-omod", "serialization.xstream-${serialization.xstreamModuleVersion}.omod", "org.openmrs.module", "omod"));
        add(new Artifact("htmlwidgets-omod", "htmlwidgets-${htmlwidgetsModuleVersion}.omod"));
        add(new Artifact("htmlformentry-omod", "htmlformentry-${htmlformentryModuleVersion}.omod"));
        add(new Artifact("htmlformentry19ext-omod", "htmlformentry19ext-${htmlformentry19extModuleVersion}.omod"));
        add(new Artifact("htmlformentryui-omod", "htmlformentryui-${htmlformentryuiModuleVersion}.omod"));
        add(new Artifact("appui-omod", "appui-${appuiModuleVersion}.omod"));
        add(new Artifact("event-omod", "event-${eventModuleVersion}.omod", "org.openmrs"));
        add(new Artifact("coreapps-omod", "coreapps-${coreappsModuleVersion}.omod"));
        add(new Artifact("webservices.rest-omod", "webservices.rest-${webservices.restModuleVersion}.omod"));
        add(new Artifact("referencedemodata-omod", "referencedemodata-${referencedemodataModuleVersion}.omod"));
        add(new Artifact("namephonetics-omod", "namephonetics-${namephoneticsModuleVersion}.omod"));
        add(new Artifact("dataexchange-omod", "dataexchange-${dataexchangeModuleVersion}.omod"));
    }};
    public static final List<Artifact> ARTIFACTS_2_1 = new ArrayList<Artifact>() {{
        add(new Artifact("openmrs-webapp", "openmrs-${openMRSVersion}.war", "org.openmrs.web", "war"));
        add(new Artifact("referencemetadata-omod", "referencemetadata-${referencemetadataModuleVersion}.omod"));
        add(new Artifact("appframework-omod", "appframework-${appframeworkModuleVersion}.omod"));
        add(new Artifact("uiframework-omod", "uiframework-${uiframeworkModuleVersion}.omod"));
        // removed for v2.1
        //add(new Artifact("logic-omod", "logic-${logicModuleVersion}.omod", "omod"));
        // ----
        add(new Artifact("registrationcore-omod", "registrationcore-${registrationcoreModuleVersion}.omod"));
        add(new Artifact("registrationapp-omod", "registrationapp-${registrationappModuleVersion}.omod"));
        add(new Artifact("idgen-omod", "idgen-${idgenModuleVersion}.omod"));
        add(new Artifact("emrapi-omod", "emrapi-${emrapiModuleVersion}.omod"));
        add(new Artifact("providermanagement-omod", "providermanagement-${providermanagementModuleVersion}.omod"));
        add(new Artifact("uilibrary-omod", "uilibrary-${uilibraryModuleVersion}.omod"));
        add(new Artifact("uicommons-omod", "uicommons-${uicommonsModuleVersion}.omod"));
        add(new Artifact("referenceapplication-omod", "referenceapplication-${referenceapplicationModuleVersion}.omod"));
        add(new Artifact("calculation-omod", "calculation-${calculationModuleVersion}.omod"));
        add(new Artifact("reporting-omod", "reporting-${reportingModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("metadatadeploy-omod", "metadatadeploy-${metadatadeployVersion}.omod"));
        // ----
        add(new Artifact("metadatasharing-omod", "metadatasharing-${metadatasharingModuleVersion}.omod"));
        add(new Artifact("metadatamapping-omod", "metadatamapping-${metadatamappingModuleVersion}.omod"));
        add(new Artifact("serialization.xstream-omod", "serialization.xstream-${serialization.xstreamModuleVersion}.omod", "org.openmrs.module", "omod"));
        add(new Artifact("htmlwidgets-omod", "htmlwidgets-${htmlwidgetsModuleVersion}.omod"));
        add(new Artifact("htmlformentry-omod", "htmlformentry-${htmlformentryModuleVersion}.omod"));
        add(new Artifact("htmlformentry19ext-omod", "htmlformentry19ext-${htmlformentry19extModuleVersion}.omod"));
        add(new Artifact("htmlformentryui-omod", "htmlformentryui-${htmlformentryuiModuleVersion}.omod"));
        add(new Artifact("appui-omod", "appui-${appuiModuleVersion}.omod"));
        add(new Artifact("event-omod", "event-${eventModuleVersion}.omod", "org.openmrs"));
        add(new Artifact("coreapps-omod", "coreapps-${coreappsModuleVersion}.omod"));
        add(new Artifact("webservices.rest-omod", "webservices.rest-${webservices.restModuleVersion}.omod"));
        add(new Artifact("referencedemodata-omod", "referencedemodata-${referencedemodataModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("reportingrest-omod", "namephonetics-${namephoneticsModuleVersion}.omod"));
        // ----
        add(new Artifact("namephonetics-omod", "reportingrest-${reportingRestVersion}.omod"));
        add(new Artifact("dataexchange-omod", "dataexchange-${dataexchangeModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("allergyapi-omod", "allergyapi-${allergyapiVersion}.omod"));
        add(new Artifact("allergyui-omod", "allergyui-${allergyuiVersion}.omod"));
        add(new Artifact("formentryapp-omod", "formentryapp-${formentryappVersion}.omod"));
        add(new Artifact("atlas-omod", "atlas-${atlasVersion}.omod", "org.openmrs.web", "omod"));
        // ----
    }};
    public static final List<Artifact> ARTIFACTS_2_2 = new ArrayList<Artifact>() {{
        add(new Artifact("openmrs-webapp", "openmrs-${openMRSVersion}.war", "org.openmrs.web", "war"));
        add(new Artifact("referencemetadata-omod", "referencemetadata-${referencemetadataModuleVersion}.omod"));
        add(new Artifact("appframework-omod", "appframework-${appframeworkModuleVersion}.omod"));
        add(new Artifact("uiframework-omod", "uiframework-${uiframeworkModuleVersion}.omod"));
        // removed for v2.1
        //add(new Artifact("logic-omod", "logic-${logicModuleVersion}.omod", "omod"));
        // ----
        add(new Artifact("registrationcore-omod", "registrationcore-${registrationcoreModuleVersion}.omod"));
        add(new Artifact("registrationapp-omod", "registrationapp-${registrationappModuleVersion}.omod"));
        add(new Artifact("idgen-omod", "idgen-${idgenModuleVersion}.omod"));
        add(new Artifact("emrapi-omod", "emrapi-${emrapiModuleVersion}.omod"));
        add(new Artifact("providermanagement-omod", "providermanagement-${providermanagementModuleVersion}.omod"));
        add(new Artifact("uilibrary-omod", "uilibrary-${uilibraryModuleVersion}.omod"));
        add(new Artifact("uicommons-omod", "uicommons-${uicommonsModuleVersion}.omod"));
        add(new Artifact("referenceapplication-omod", "referenceapplication-${referenceapplicationModuleVersion}.omod"));
        add(new Artifact("calculation-omod", "calculation-${calculationModuleVersion}.omod"));
        add(new Artifact("reporting-omod", "reporting-${reportingModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("metadatadeploy-omod", "metadatadeploy-${metadatadeployVersion}.omod"));
        // ----
        add(new Artifact("metadatasharing-omod", "metadatasharing-${metadatasharingModuleVersion}.omod"));
        add(new Artifact("metadatamapping-omod", "metadatamapping-${metadatamappingModuleVersion}.omod"));
        add(new Artifact("serialization.xstream-omod", "serialization.xstream-${serialization.xstreamModuleVersion}.omod", "org.openmrs.module", "omod"));
        add(new Artifact("htmlwidgets-omod", "htmlwidgets-${htmlwidgetsModuleVersion}.omod"));
        add(new Artifact("htmlformentry-omod", "htmlformentry-${htmlformentryModuleVersion}.omod"));
        add(new Artifact("htmlformentry19ext-omod", "htmlformentry19ext-${htmlformentry19extModuleVersion}.omod"));
        add(new Artifact("htmlformentryui-omod", "htmlformentryui-${htmlformentryuiModuleVersion}.omod"));
        add(new Artifact("appui-omod", "appui-${appuiModuleVersion}.omod"));
        add(new Artifact("event-omod", "event-${eventModuleVersion}.omod", "org.openmrs"));
        add(new Artifact("coreapps-omod", "coreapps-${coreappsModuleVersion}.omod"));
        add(new Artifact("webservices.rest-omod", "webservices.rest-${webservices.restModuleVersion}.omod"));
        add(new Artifact("referencedemodata-omod", "referencedemodata-${referencedemodataModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("reportingrest-omod", "namephonetics-${namephoneticsModuleVersion}.omod"));
        // ----
        // removed in v2.2
        //add(new Artifact("namephonetics-omod", "reportingrest-${reportingRestVersion}.omod"));
        // ----
        add(new Artifact("dataexchange-omod", "dataexchange-${dataexchangeModuleVersion}.omod"));
        // added for v2.1
        add(new Artifact("allergyapi-omod", "allergyapi-${allergyapiVersion}.omod"));
        add(new Artifact("allergyui-omod", "allergyui-${allergyuiVersion}.omod"));
        add(new Artifact("formentryapp-omod", "formentryapp-${formentryappVersion}.omod"));
        add(new Artifact("atlas-omod", "atlas-${atlasVersion}.omod", "org.openmrs.web", "omod"));
        // ----
        // added for v2.2
        add(new Artifact("appointmentscheduling-omod", "appointmentscheduling-${appointmentschedulingVersion}.omod"));
        add(new Artifact("appointmentschedulingui-omod", "appointmentschedulingui-${appointmentschedulinguiVersion}.omod"));
        add(new Artifact("chartsearch-omod", "chartsearch-${chartsearchVersion}.omod", null));
        // ----
    }};
    // final module base
    public static final HashMap<String, List<Artifact>> ARTIFACTS = new HashMap<String, List<Artifact>>() {{
        put("2.0", ARTIFACTS_2_0);
        put("2.1", ARTIFACTS_2_1);
        put("2.2", ARTIFACTS_2_2);
    }};
}