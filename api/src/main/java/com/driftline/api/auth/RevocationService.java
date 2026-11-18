package com.driftline.api.auth;

import com.driftline.api.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Revocation state in Redis (decisions/0009, 0012).
 *
 * Two kinds of entries:
 * - blacklist of single access tokens by jti, kept until the token would have expired anyway
 * - a per-user "tokens issued at or before this time are invalid" cutoff, used to revoke all of a
 *   user's access tokens at once (logout everywhere, rate limit, account deletion)
 *
 * Any Redis failure surfaces as an exception. Callers treat that as "reject" (fail closed).
 */
@Service
public class RevocationService {

	private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
	private static final String CUTOFF_PREFIX = "jwt:revoked-before:";

	private final StringRedisTemplate redis;
	private final RefreshTokenService refreshTokens;
	private final Duration accessTtl;

	public RevocationService(StringRedisTemplate redis, RefreshTokenService refreshTokens, AppProperties props) {
		this.redis = redis;
		this.refreshTokens = refreshTokens;
		this.accessTtl = props.jwt().accessTtl();
	}

	public boolean isRevoked(Jwt jwt) {
		if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jwt.getId()))) {
			return true;
		}
		String cutoff = redis.opsForValue().get(CUTOFF_PREFIX + jwt.getSubject());
		if (cutoff == null || jwt.getIssuedAt() == null) {
			return false;
		}
		// "<=": a token issued in the same second as the revoke counts as revoked. Safe side.
		return jwt.getIssuedAt().getEpochSecond() <= Long.parseLong(cutoff);
	}

	/** Blacklist one access token until it expires on its own. */
	public void revokeAccessToken(String jti, Instant expiresAt) {
		Duration remaining = Duration.between(Instant.now(), expiresAt);
		if (remaining.isNegative() || remaining.isZero()) {
			return;
		}
		redis.opsForValue().set(BLACKLIST_PREFIX + jti, "1", remaining);
	}

	/** Revoke all of a user's access tokens and delete all of their refresh tokens. */
	public void revokeAllForUser(UUID userId) {
		long now = Instant.now().getEpochSecond();
		// The cutoff only matters while older access tokens could still be alive. Small margin on top.
		redis.opsForValue().set(CUTOFF_PREFIX + userId, Long.toString(now), accessTtl.plusMinutes(5));
		refreshTokens.deleteAllForUser(userId);
	}
}
