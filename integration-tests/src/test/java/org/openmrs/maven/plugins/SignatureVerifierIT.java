package org.openmrs.maven.plugins;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.maven.plugins.utility.SignatureVerifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

public class SignatureVerifierIT {

	private static final String JAR_URL =
			"https://openmrs.jfrog.io/artifactory/public/org/openmrs/module/xforms-omod/5.0.0/xforms-omod-5.0.0.jar";

	private static final String ASC_URL = JAR_URL + ".asc";

	private static File jar;

	private static File asc;

	@BeforeClass
	public static void setup() throws IOException {
		Path cacheDir = new File("target/signature-fixtures").toPath();
		Files.createDirectories(cacheDir);
		jar = downloadIfMissing(JAR_URL, cacheDir.resolve("xforms-omod-5.0.0.jar").toFile());
		asc = downloadIfMissing(ASC_URL, cacheDir.resolve("xforms-omod-5.0.0.jar.asc").toFile());
		assumeNotNull("Could not fetch xforms 5.0.0 fixture (offline?); skipping", jar, asc);
	}

	@Test
	public void realSignedArtifact_verifiesAgainstPinnedKey() throws Exception {
		SignatureVerifier.verify(jar, asc);
	}

	@Test
	public void realSignedArtifact_tamperedFails() throws Exception {
		File tampered = File.createTempFile("xforms-tampered", ".jar");
		tampered.deleteOnExit();
		Files.copy(jar.toPath(), tampered.toPath(), StandardCopyOption.REPLACE_EXISTING);
		try (RandomAccessFile raf = new RandomAccessFile(tampered, "rw")) {
			long mid = raf.length() / 2;
			raf.seek(mid);
			int b = raf.read();
			raf.seek(mid);
			raf.write(b ^ 0x01);
		}
		try {
			SignatureVerifier.verify(tampered, asc);
			fail("Verification should have failed on tampered artifact");
		}
		catch (SecurityException expected) {
		}
	}

	private static File downloadIfMissing(String url, File target) {
		if (target.exists() && target.length() > 0) {
			return target;
		}
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(10_000);
			conn.setReadTimeout(30_000);
			conn.setInstanceFollowRedirects(true);
			if (conn.getResponseCode() != 200) {
				return null;
			}
			try (InputStream in = conn.getInputStream()) {
				Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			return target;
		}
		catch (IOException e) {
			return null;
		}
	}
}