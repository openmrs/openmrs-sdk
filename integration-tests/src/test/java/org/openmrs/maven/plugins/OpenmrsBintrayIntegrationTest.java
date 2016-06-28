package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.bintray.BintrayId;
import org.openmrs.maven.plugins.bintray.BintrayPackage;
import org.openmrs.maven.plugins.bintray.OpenmrsBintray;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.openmrs.maven.plugins.SdkMatchers.hasOwner;
import static org.openmrs.maven.plugins.SdkMatchers.hasRepository;

public class OpenmrsBintrayIntegrationTest extends AbstractSdkIntegrationTest {

    OpenmrsBintray openmrsBintray;

    @Before
    public void setup() throws Exception {
        super.setup();
        openmrsBintray = new OpenmrsBintray();
    }
    @Test
    public void getAvailableOwaTest() throws Exception{
        List<BintrayId> results = openmrsBintray.getAvailableOWA();
        assertThat(results, hasSize(greaterThanOrEqualTo(1)));
    }
    @Test
    public void getOwaMetadataTest() throws Exception{
        BintrayPackage dictionary = openmrsBintray.getOwaMetadata("openmrs-owa-conceptdictionary");
        assertThat(dictionary, is(notNullValue()));
        assertThat(dictionary, hasOwner("openmrs"));
        assertThat(dictionary, hasRepository("owa"));
    }
    @Test
    public void downloadOwaTest() throws Exception{
        openmrsBintray.downloadOWA(
                testDirectory,
                "openmrs-owa-conceptdictionary",
                "1.0.0-beta.6");
        assertFilePresent("conceptdictionary" + File.separator + "index.html");
        assertFilePresent("conceptdictionary" + File.separator + "manifest.webapp");
    }
}
