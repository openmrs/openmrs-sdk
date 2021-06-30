package org.openmrs.maven.plugins.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerTest {

    Server server;

    @Before
    public void setUp(){
        server = new Server.ServerBuilder().build();
    }

    @Test
    public void testParseUserModules() throws Exception{
        String testUserModules = "org.openmrs.module/owa/1.4-SNAPSHOT," +
                "org.openmrs.module/uicommons/1.7,org.openmrs.module/webservices.rest/2.15-SNAPSHOT";
        Artifact owaModule = new Artifact("owa-omod","1.4-SNAPSHOT", "org.openmrs.module");

        server.setParam(Server.PROPERTY_USER_MODULES, testUserModules);
        List<Artifact> artifacts = server.getUserModules();
        assertThat(artifacts.size(), is(3));
        checkIfAnyArtifactMatches(artifacts, owaModule);
    }

    @Test(expected = MojoExecutionException.class)
    public void testParseUserModulesShouldThrowParseExc() throws Exception{
        String brokenUserModules = "org.openmrs.module/owa/1.4-SNAPSHOT," +
                "org.openmrs.module/uico7,org.openmrs.module/webservices.rest/2.15-SNAPSHOT";
        server.setParam(Server.PROPERTY_USER_MODULES, brokenUserModules);
        List<Artifact> artifacts = server.getUserModules();
    }

    private void checkIfAnyArtifactMatches(List<Artifact> artifacts, Artifact searched) {
        boolean foundMatch = false;
        for(Artifact artifact : artifacts){
            boolean eqArtifactId = artifact.getArtifactId().equals(searched.getArtifactId());
            boolean eqGroupId = artifact.getGroupId().equals(searched.getGroupId());
            boolean eqVersion = artifact.getVersion().equals(searched.getVersion());
            foundMatch = eqArtifactId&&eqGroupId&&eqVersion;
            if(foundMatch) {
                break;
            }
        }
        assertThat(foundMatch, is(true));
    }

    @Test
    public void testMergeArtifactList() {
        Artifact owa14 = new Artifact("owa", "1.4");
        Artifact appui15 = new Artifact("appui", "1.5");
        Artifact refapp2232 = new Artifact("refapp", "2232");
        Artifact groupDistroGroup = new Artifact("group", "222", "org.distro");

        List<Artifact> mainList = Arrays.asList(
                owa14,
                appui15,
                refapp2232,
                groupDistroGroup
        );

        Artifact owa15 = new Artifact("owa", "1.5");
        Artifact appui15snap = new Artifact("appui", "1.5-SNAPSHOT");
        Artifact platform2020 = new Artifact("platform module", "2020");
        Artifact refapp33 = new Artifact("refapp", "33");
        Artifact group3456 = new Artifact("group", "3456");

        List<Artifact> updated = Arrays.asList(
                owa15,
                appui15snap,
                platform2020,
                refapp33,
                group3456
        );

        List<Artifact> result = Server.mergeArtifactLists(mainList, updated);
        assertThat(result, hasItems(owa15, platform2020, refapp33, appui15snap, group3456, groupDistroGroup));
        assertThat(result, not(hasItems(owa14, appui15, refapp2232)));
    }

    @Test
    public void testGetMySqlUrlPort(){
        server.setDbUri(SDKConstants.URI_MYSQL+"234:/d2dd/d2:");
        String result = server.getMySqlPort();
        assertThat(result, is(equalTo("3306")));

        server.setDbUri(SDKConstants.URI_MYSQL.replace("localhost", "144.244.242.55.666"));
        String result2 = server.getMySqlPort();
        assertThat(result2, is(equalTo("3306")));
    }
}
