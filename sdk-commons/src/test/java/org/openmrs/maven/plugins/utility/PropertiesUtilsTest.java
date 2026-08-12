package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
}
