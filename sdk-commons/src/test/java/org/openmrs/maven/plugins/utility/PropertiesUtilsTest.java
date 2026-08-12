package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class PropertiesUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void loadPropertiesFromFile_shouldReadNonAsciiCharactersEncodedAsUtf8() throws Exception {
        File file = tempFolder.newFile("content.properties");
        String content = "var.encounterType.MEDICATION_DISPENSED.name=Médicaments administrés\n";
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));

        Properties properties = PropertiesUtils.loadPropertiesFromFile(file);

        assertThat(properties.getProperty("var.encounterType.MEDICATION_DISPENSED.name"),
                equalTo("Médicaments administrés"));
    }

    @Test
    public void loadPropertiesFromFile_shouldStillHonorJavaPropertiesUnicodeEscapes() throws MojoExecutionException, java.io.IOException {
        File file = tempFolder.newFile("content.properties");
        // Written as a literal backslash-u escape, the classic Java Properties convention for
        // non-ASCII text, rather than as raw UTF-8 bytes. Must keep working after the fix.
        String content = "var.oncologyLocation=Butaro Hospital\nvar.escaped=caf\\u00e9\n";
        Files.write(file.toPath(), content.getBytes(StandardCharsets.US_ASCII));

        Properties properties = PropertiesUtils.loadPropertiesFromFile(file);

        assertThat(properties.getProperty("var.escaped"), equalTo("café"));
        assertThat(properties.getProperty("var.oncologyLocation"), equalTo("Butaro Hospital"));
    }

    @Test
    public void loadPropertiesFromFile_shouldStripLeadingUtf8ByteOrderMark() throws Exception {
        File file = tempFolder.newFile("content.properties");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "name=TEST EMR\n".getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(body, 0, combined, bom.length, body.length);
        Files.write(file.toPath(), combined);

        Properties properties = PropertiesUtils.loadPropertiesFromFile(file);

        assertThat(properties.getProperty("name"), equalTo("TEST EMR"));
    }

    @Test
    public void loadPropertiesFromFile_shouldFailLoudlyRatherThanSilentlyReplaceInvalidUtf8() throws Exception {
        File file = tempFolder.newFile("content.properties");
        // "café" saved by an editor using ISO-8859-1/cp1252, not UTF-8: the raw byte 0xE9 for
        // "é" is not a valid UTF-8 byte sequence on its own. Loading this must raise a clear
        // error rather than silently substituting the U+FFFD replacement character.
        byte[] invalidUtf8 = {'v', 'a', 'r', '.', 'x', '=', 'c', 'a', 'f', (byte) 0xE9, '\n'};
        Files.write(file.toPath(), invalidUtf8);

        try {
            PropertiesUtils.loadPropertiesFromFile(file);
            fail("Expected a MojoExecutionException for invalid UTF-8 content");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), containsString(file.getAbsolutePath()));
        }
    }

    @Test
    public void loadPropertiesFromResource_shouldNotDoubleWrapTheMissingResourceMessage() {
        try {
            PropertiesUtils.loadPropertiesFromResource("does/not/exist.properties");
            fail("Expected a MojoExecutionException for a missing classpath resource");
        } catch (MojoExecutionException e) {
            // The resource name should appear exactly once, not once from the "not found" check
            // and again from the surrounding catch block re-wrapping it.
            int firstIndex = e.getMessage().indexOf("does/not/exist.properties");
            int lastIndex = e.getMessage().lastIndexOf("does/not/exist.properties");
            assertThat("resource name should appear exactly once in: " + e.getMessage(), firstIndex, equalTo(lastIndex));
        }
    }

    @Test
    public void loadPropertiesFromFile_shouldNameTheFileOnAnyFailureNotJustDecodingFailures() {
        File missingFile = new File(tempFolder.getRoot(), "does-not-exist.properties");

        try {
            PropertiesUtils.loadPropertiesFromFile(missingFile);
            fail("Expected a MojoExecutionException for a missing file");
        } catch (MojoExecutionException e) {
            // Both failure paths (a decoding error and a plain I/O error, e.g. file not found)
            // should name the file the same way, rather than only one of the two branches doing so.
            assertThat(e.getMessage(), containsString(missingFile.getAbsolutePath()));
        }
    }
}
