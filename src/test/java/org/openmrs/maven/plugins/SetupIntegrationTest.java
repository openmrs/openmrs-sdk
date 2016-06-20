package org.openmrs.maven.plugins;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class SetupIntegrationTest extends TestCase {
    public void testSetup() throws Exception{
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mock-project");
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.executeGoal("org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:2.1.3-SNAPSHOT:reset");
    }
}
