package org.openmrs.maven.plugins;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openmrs.maven.plugins.bintray.BintrayId;
import org.openmrs.maven.plugins.bintray.BintrayPackage;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;

public class SdkMatchers {

    public static Matcher<Server> serverHasVersion(final String version) {
        return new FeatureMatcher<Server, String>(equalTo(version), "server version", "server version") {
            @Override
            protected String featureValueOf(final Server actual) {
                return actual.getVersion();
            }
        };
    }
    public static Matcher<Server> serverHasName(final String name) {
        return new FeatureMatcher<Server, String>(equalTo(name), "distribution name", "distribution name") {
            @Override
            protected String featureValueOf(final Server actual) {
                return actual.getName();
            }
        };
    }
    public static Matcher<Server> hasUserOwa(final BintrayId owa) {
        return new FeatureMatcher<Server, List<BintrayId>>(hasItem(owa), "user owas", "server user owas") {
            @Override
            protected List<BintrayId> featureValueOf(Server actual) {
                return actual.getUserOWAs();
            }
        };
    }
    public static Matcher<BintrayPackage> hasOwner(final String owner) {
        return new FeatureMatcher<BintrayPackage, String>(equalTo(owner), "server version", "server version") {
            @Override
            protected String featureValueOf(final BintrayPackage actual) {
                return actual.getOwner();
            }
        };
    }
    public static Matcher<BintrayPackage> hasRepository(final String repository) {
        return new FeatureMatcher<BintrayPackage, String>(equalTo(repository), "server version", "server version") {
            @Override
            protected String featureValueOf(final BintrayPackage actual) {
                return actual.getRepository();
            }
        };
    }
    public static Matcher<File> hasNameStartingWith(final String namePrefix) {
        return new FeatureMatcher<File, String>(startsWith(namePrefix), "file with name", "file with name") {
            @Override
            protected String featureValueOf(final File actual) {
                return actual.getName();
            }
        };
    }
    public static Matcher<Server> hasPropertyEqualTo(final String propertyName, final String propertyValue) {
        return new FeatureMatcher<Server, String>(equalTo(propertyValue), "property value", "property value") {
            @Override
            protected String featureValueOf(final Server actual) {
                return actual.getCustomProperties().get(propertyName);
            }
        };
    }
    public static Matcher<Server> hasModuleVersion(final String artifactId, final String version){
        return new FeatureMatcher<Server, String>(equalTo(version), "module version", "module version") {
            @Override
            protected String featureValueOf(Server actual) {
                return actual.getParam("omod."+artifactId);
            }
        };
    }
    public static Matcher<DistroProperties> hasModuleVersionInDisstro(final String artifactId, final String version){
        return new FeatureMatcher<DistroProperties, String>(equalTo(version), "module version", "module version") {
            @Override
            protected String featureValueOf(DistroProperties actual) {
                return actual.getParam("omod."+artifactId);
            }
        };
    }
    public static Matcher<Server> hasPlatformVersion(final String version){
        return new FeatureMatcher<Server, String>(equalTo(version), "war version", "war version") {
            @Override
            protected String featureValueOf(Server actual) {
                return actual.getPlatformVersion();
            }
        };
    }
    public static Matcher<DistroProperties> hasWarVersion(final String version) {
        return new FeatureMatcher<DistroProperties, String>(equalTo(version), "war version", "war version") {
            @Override
            protected String featureValueOf(DistroProperties actual) {
                return actual.getPlatformVersion();
            }
        };
    }
    public static Matcher<Server> serverHasDebugPort(final String port) {
        return new FeatureMatcher<Server, String>(equalTo(port), "debug port", "debug port") {
            @Override
            protected String featureValueOf(final Server actual) {
                return actual.getDebugPort();
            }
        };
    }
}
