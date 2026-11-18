package com.driftline.api.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Rejects revoked access tokens. If the revocation store cannot be read, the token is rejected too. */
public class RevocationValidator implements OAuth2TokenValidator<Jwt> {

	private final RevocationService revocation;

	public RevocationValidator(RevocationService revocation) {
		this.revocation = revocation;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		try {
			if (revocation.isRevoked(jwt)) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token has been revoked", null));
			}
			return OAuth2TokenValidatorResult.success();
		}
		catch (RuntimeException e) {
			// Redis down or unreadable: fail closed (decisions/0009, 0012).
			return OAuth2TokenValidatorResult.failure(
				new OAuth2Error("invalid_token", "Revocation check unavailable", null));
		}
	}
}
