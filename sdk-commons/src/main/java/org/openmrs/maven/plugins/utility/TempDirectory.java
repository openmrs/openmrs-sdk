package org.openmrs.maven.plugins.utility;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Facilitates using temp files in try-with-resource blocks
 */
@Getter
public class TempDirectory implements AutoCloseable {

	private final File file;

	private TempDirectory(File file) {
		this.file = file;
	}

	public static TempDirectory create() throws MojoExecutionException {
		return create("");
	}

	public static TempDirectory create(String name) throws MojoExecutionException {
		try {
			File file = Files.createTempDirectory("openmrs-sdk-" + name).toFile();
			return new TempDirectory(file);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unable to create temp file", e);
		}
	}

	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}

	public Path getPath() {
		return file.toPath();
	}

	@Override
	public void close() {
		FileUtils.deleteQuietly(file);
	}
}
