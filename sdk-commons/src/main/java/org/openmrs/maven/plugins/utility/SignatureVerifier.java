package org.openmrs.maven.plugins.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.Security;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class SignatureVerifier {

	private static final String BC_PROVIDER = "BC";

	static {
		if (Security.getProvider(BC_PROVIDER) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static final String KEY_RESOURCE = "openmrs-signing-key.asc";

	private static final Set<String> TRUSTED_FINGERPRINTS;
	static {
		Set<String> s = new HashSet<>();
		s.add("CA12619FDE8CD6A93FAFE458A6F9608DCC73473F");
		TRUSTED_FINGERPRINTS = Collections.unmodifiableSet(s);
	}

	private static volatile PGPPublicKey trustedKey;

	static PGPPublicKey getTrustedKey() throws IOException, PGPException {
		PGPPublicKey k = trustedKey;
		if (k == null) {
			synchronized (SignatureVerifier.class) {
				k = trustedKey;
				if (k == null) {
					k = loadAndPin();
					trustedKey = k;
				}
			}
		}
		return k;
	}

	public static void verify(File artifact, File signatureFile) throws IOException, PGPException {
		verifyAgainst(artifact, signatureFile, getTrustedKey());
	}

	static void verifyAgainst(File artifact, File signatureFile, PGPPublicKey key) throws IOException, PGPException {
		PGPSignature sig;
		try (InputStream sigIn = PGPUtil.getDecoderStream(Files.newInputStream(signatureFile.toPath()))) {
			Object next = new JcaPGPObjectFactory(sigIn).nextObject();
			if (!(next instanceof PGPSignatureList)) {
				throw new SecurityException("Signature file " + signatureFile.getName() + " is not a PGP signature list");
			}
			PGPSignatureList sigs = (PGPSignatureList) next;
			if (sigs.isEmpty()) {
				throw new SecurityException("Signature file " + signatureFile.getName() + " contains no signatures");
			}
			sig = sigs.get(0);
		}

		sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(BC_PROVIDER), key);

		try (InputStream artifactIn = Files.newInputStream(artifact.toPath())) {
			byte[] buf = new byte[8192];
			int n;
			while ((n = artifactIn.read(buf)) != -1) {
				sig.update(buf, 0, n);
			}
		}

		if (!sig.verify()) {
			throw new SecurityException("Signature verification failed for " + artifact.getName());
		}
	}

	private static PGPPublicKey loadAndPin() throws IOException, PGPException {
		try (InputStream in = SignatureVerifier.class.getClassLoader().getResourceAsStream(KEY_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing bundled signing key resource: " + KEY_RESOURCE);
			}
			PGPPublicKeyRingCollection rings = new JcaPGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));

			PGPPublicKey master = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(rings.getKeyRings(), Spliterator.ORDERED), false)
					.flatMap(ring -> StreamSupport.stream(
							Spliterators.spliteratorUnknownSize(ring.getPublicKeys(), Spliterator.ORDERED), false))
					.filter(PGPPublicKey::isMasterKey)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No master public key found in " + KEY_RESOURCE));

			String fp = IntStream.range(0, master.getFingerprint().length)
					.mapToObj(i -> String.format("%02X", master.getFingerprint()[i]))
					.collect(Collectors.joining());

			if (!TRUSTED_FINGERPRINTS.contains(fp)) {
				throw new SecurityException(
						"Bundled signing key fingerprint " + fp + " is not in the pinned trust set. Refusing to load.");
			}
			return master;
		}
	}
}