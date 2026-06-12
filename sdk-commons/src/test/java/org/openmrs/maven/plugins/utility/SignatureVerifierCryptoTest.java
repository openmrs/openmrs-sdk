package org.openmrs.maven.plugins.utility;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Date;

import static org.junit.Assert.fail;

public class SignatureVerifierCryptoTest {

	private static PGPKeyPair keyA;

	private static PGPKeyPair keyB;

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@BeforeClass
	public static void setUpKeys() throws Exception {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		keyA = generateRsaKeyPair();
		keyB = generateRsaKeyPair();
	}

	@Test
	public void happyPath() throws Exception {
		File artifact = writeFixture("hello world");
		File asc = sign(artifact, keyA);

		SignatureVerifier.verifyAgainst(artifact, asc, keyA.getPublicKey());
	}

	@Test
	public void tamperedArtifactFails() throws Exception {
		File artifact = writeFixture("the quick brown fox jumps over the lazy dog");
		File asc = sign(artifact, keyA);

		try (RandomAccessFile raf = new RandomAccessFile(artifact, "rw")) {
			raf.seek(raf.length() / 2);
			int b = raf.read();
			raf.seek(raf.length() / 2);
			raf.write(b ^ 0x01);
		}

		try {
			SignatureVerifier.verifyAgainst(artifact, asc, keyA.getPublicKey());
			fail("Verification should have failed on tampered artifact");
		}
		catch (SecurityException expected) {
		}
	}

	@Test
	public void wrongKeyFails() throws Exception {
		File artifact = writeFixture("signed-with-A, verified-with-B");
		File asc = sign(artifact, keyA);

		try {
			SignatureVerifier.verifyAgainst(artifact, asc, keyB.getPublicKey());
			fail("Verification should have failed against the wrong public key");
		}
		catch (SecurityException expected) {
		}
	}

	private File writeFixture(String content) throws Exception {
		File f = tmp.newFile();
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private File sign(File artifact, PGPKeyPair signer) throws Exception {
		File asc = tmp.newFile();
		PGPSignatureGenerator sigGen = new PGPSignatureGenerator(
				new JcaPGPContentSignerBuilder(signer.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256)
						.setProvider("BC"));
		sigGen.init(PGPSignature.BINARY_DOCUMENT, signer.getPrivateKey());

		try (InputStream in = Files.newInputStream(artifact.toPath())) {
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				sigGen.update(buf, 0, n);
			}
		}

		try (OutputStream out = new ArmoredOutputStream(new FileOutputStream(asc))) {
			sigGen.generate().encode(out);
		}
		return asc;
	}

	private static PGPKeyPair generateRsaKeyPair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		return new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());
	}
}