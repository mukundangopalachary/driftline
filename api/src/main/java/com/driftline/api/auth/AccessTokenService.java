package com.driftline.api.auth;

import com.driftline.api.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues access tokens: ES256 JWTs, 1 day by default (decisions/0009, 0015). */
@Service
public class AccessTokenService {

	private final JwtEncoder encoder;
	private final String issuer;
	private final Duration ttl;

	public AccessTokenService(JwtEncoder encoder, AppProperties props) {
		this.encoder = encoder;
		this.issuer = props.jwt().issuer();
		this.ttl = props.jwt().accessTtl();
	}

	public IssuedAccessToken issue(UUID userId) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(ttl);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject(userId.toString())
			.id(UUID.randomUUID().toString()) // jti, used for the blacklist
			.issuedAt(now)
			.expiresAt(expiresAt)
			.build();
		String value = encoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.ES256).build(), claims))
			.getTokenValue();
		return new IssuedAccessToken(value, expiresAt, ttl);
	}

	public record IssuedAccessToken(String value, Instant expiresAt, Duration ttl) {
	}
}
