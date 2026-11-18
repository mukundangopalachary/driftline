package com.driftline.api;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/** Generates ES256 (P-256) key pairs in the same format the app reads: base64 of a PEM file. */
public final class TestKeys {

	public record Pair(KeyPair keyPair, String privateKeyB64, String publicKeyB64) {
	}

	private TestKeys() {
	}

	public static Pair generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
			generator.initialize(new ECGenParameterSpec("secp256r1"));
			KeyPair pair = generator.generateKeyPair();
			// The JDK encodes EC private keys as PKCS#8 and public keys as X.509, which is what the app expects.
			return new Pair(pair, wrap(pair.getPrivate().getEncoded(), "PRIVATE KEY"),
				wrap(pair.getPublic().getEncoded(), "PUBLIC KEY"));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String wrap(byte[] der, String type) {
		String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
		String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
		return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.US_ASCII));
	}
}
