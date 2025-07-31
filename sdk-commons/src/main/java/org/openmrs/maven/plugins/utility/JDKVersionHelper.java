package org.openmrs.maven.plugins.utility;

public class JDKVersionHelper {
    public static int parseMajorVersion(String version) {
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.split("\\.")[1]);
        } else {
            return Integer.parseInt(version.split("\\.")[0]);
        }
    }
}
