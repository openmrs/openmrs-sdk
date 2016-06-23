package org.openmrs.maven.plugins;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openmrs.maven.plugins.model.Server;

import static org.hamcrest.CoreMatchers.equalTo;

public class SdkMatchers {

    public static Matcher<Server> serverHasVersion(final String version){
        return new FeatureMatcher<Server, String>(equalTo(version), "server version", "server version"){
            @Override
            protected String featureValueOf(final Server actual) {
                return actual.getVersion();
            }
        };
    }
}
