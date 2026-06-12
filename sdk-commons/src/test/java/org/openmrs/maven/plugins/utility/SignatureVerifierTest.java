package org.openmrs.maven.plugins.utility;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class SignatureVerifierTest {

	private static final String EXPECTED_FINGERPRINT = "CA12619FDE8CD6A93FAFE458A6F9608DCC73473F";

	@Test
	public void bundledKeyMatchesPinnedFingerprint() throws Exception {
		PGPPublicKey key = SignatureVerifier.getTrustedKey();

		assertThat("trusted key should load", key, notNullValue());

		StringBuilder fp = new StringBuilder();
		for (byte b : key.getFingerprint()) {
			fp.append(String.format("%02X", b));
		}
		assertThat(fp.toString(), equalTo(EXPECTED_FINGERPRINT));
	}
}