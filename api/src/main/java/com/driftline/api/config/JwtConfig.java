package com.driftline.api.config;

import com.driftline.api.auth.RevocationService;
import com.driftline.api.auth.RevocationValidator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** ES256 signing and verification (decisions/0015). Keys come from configuration, never from code. */
@Configuration
public class JwtConfig {

	@Bean
	ECKey jwtSigningKey(AppProperties props) throws Exception {
		KeyFactory kf = KeyFactory.getInstance("EC");
		ECPrivateKey privateKey = (ECPrivateKey) kf.generatePrivate(
			new PKCS8EncodedKeySpec(pemBody(props.jwt().privateKeyB64())));
		ECPublicKey publicKey = (ECPublicKey) kf.generatePublic(
			new X509EncodedKeySpec(pemBody(props.jwt().publicKeyB64())));
		return new ECKey.Builder(Curve.P_256, publicKey).privateKey(privateKey).build();
	}

	@Bean
	JwtEncoder jwtEncoder(ECKey signingKey) {
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
	}

	@Bean
	JwtDecoder jwtDecoder(ECKey signingKey, AppProperties props, RevocationService revocation) {
		// Only ES256 is accepted. A token with any other algorithm (including "none") fails verification.
		var processor = new DefaultJWTProcessor<SecurityContext>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
			JWSAlgorithm.ES256, new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()))));
		// Expiry and issuer are checked by the Spring validators below.
		processor.setJWTClaimsSetVerifier((claims, context) -> {
		});
		var decoder = new NimbusJwtDecoder(processor);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
			JwtValidators.createDefaultWithIssuer(props.jwt().issuer()),
			new RevocationValidator(revocation)));
		return decoder;
	}

	/** The config holds a base64 string wrapping a PEM file. Unwrap it down to the DER bytes. */
	static byte[] pemBody(String b64OfPem) {
		String pem = new String(Base64.getDecoder().decode(b64OfPem.trim()), StandardCharsets.US_ASCII);
		String body = pem.replaceAll("-----(BEGIN|END)[A-Z ]+-----", "").replaceAll("\\s", "");
		return Base64.getDecoder().decode(body);
	}
}
