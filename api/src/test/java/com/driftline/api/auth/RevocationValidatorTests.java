package com.driftline.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Redis down means reject (decisions/0009, 0012). */
class RevocationValidatorTests {

	private static Jwt jwt() {
		return Jwt.withTokenValue("t").header("alg", "ES256").subject("u").jti("j")
			.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
	}

	@Test
	void redisDownRejectsTheToken() {
		RevocationService revocation = mock(RevocationService.class);
		when(revocation.isRevoked(any())).thenThrow(new RedisConnectionFailureException("down"));

		assertThat(new RevocationValidator(revocation).validate(jwt()).hasErrors()).isTrue();
	}

	@Test
	void revokedTokenIsRejected() {
		RevocationService revocation = mock(RevocationService.class);
		when(revocation.isRevoked(any())).thenReturn(true);

		assertThat(new RevocationValidator(revocation).validate(jwt()).hasErrors()).isTrue();
	}

	@Test
	void cleanTokenIsAccepted() {
		RevocationService revocation = mock(RevocationService.class);
		when(revocation.isRevoked(any())).thenReturn(false);

		assertThat(new RevocationValidator(revocation).validate(jwt()).hasErrors()).isFalse();
	}
}
