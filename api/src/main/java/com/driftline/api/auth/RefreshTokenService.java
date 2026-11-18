package com.driftline.api.auth;

import com.driftline.api.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens (decisions/0009).
 *
 * The browser holds a random base64url token. The server stores only its SHA-256 hash, in Postgres
 * (record) and Redis (cache). Lookup order is Redis, then Postgres. If Redis is down the call fails,
 * it does not fall back to Postgres (fail closed, decisions/0012).
 */
@Service
public class RefreshTokenService {

	private static final String CACHE_PREFIX = "refresh:";
	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository repository;
	private final StringRedisTemplate redis;
	private final Duration ttl;
	private final SecureRandom random = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository repository, StringRedisTemplate redis, AppProperties props) {
		this.repository = repository;
		this.redis = redis;
		this.ttl = props.refreshTtl();
	}

	/** Create a refresh token for the user. Returns the raw token, which is never stored. */
	@Transactional
	public IssuedRefreshToken issue(UUID userId) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String hash = hash(raw);
		Instant expiresAt = Instant.now().plus(ttl);
		repository.save(new RefreshToken(userId, hash, expiresAt));
		redis.opsForValue().set(CACHE_PREFIX + hash, userId.toString(), ttl);
		return new IssuedRefreshToken(raw, expiresAt, ttl);
	}

	/** The user this token belongs to, if it is known and not expired. */
	@Transactional(readOnly = true)
	public Optional<UUID> validate(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String hash = hash(raw);
		String cached = redis.opsForValue().get(CACHE_PREFIX + hash);
		if (cached != null) {
			return Optional.of(UUID.fromString(cached));
		}
		// Cache miss: ask Postgres, then refill the cache for the time that is left.
		return repository.findByTokenHash(hash)
			.filter(t -> t.getExpiresAt().isAfter(Instant.now()))
			.map(t -> {
				Duration remaining = Duration.between(Instant.now(), t.getExpiresAt());
				redis.opsForValue().set(CACHE_PREFIX + hash, t.getUserId().toString(), remaining);
				return t.getUserId();
			});
	}

	@Transactional
	public void delete(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		String hash = hash(raw);
		repository.deleteByTokenHash(hash);
		redis.delete(CACHE_PREFIX + hash);
	}

	@Transactional
	public void deleteAllForUser(UUID userId) {
		List<String> hashes = repository.findTokenHashesByUserId(userId);
		repository.deleteByUserId(userId);
		if (!hashes.isEmpty()) {
			redis.delete(hashes.stream().map(h -> CACHE_PREFIX + h).toList());
		}
	}

	/**
	 * SHA-256 is enough here. The token is 32 random bytes, not a guessable password, so a slow
	 * password hash adds nothing (decisions/0009).
	 */
	static String hash(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public record IssuedRefreshToken(String value, Instant expiresAt, Duration ttl) {
	}
}
