package com.driftline.api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit for authenticated requests (decisions/0009): more than N requests per minute from one user
 * revokes all of that user's tokens and ends the request with 401. Counters live in Redis (0012).
 * Unauthenticated requests are not limited. If Redis is down the request is rejected (503).
 *
 * Fixed one-minute windows. A burst across a window boundary can briefly count up to twice the limit.
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final StringRedisTemplate redis;
	private final RevocationService revocation;
	private final AuthCookies cookies;
	private final int limitPerMinute;

	public RateLimitFilter(StringRedisTemplate redis, RevocationService revocation, AuthCookies cookies,
			int limitPerMinute) {
		this.redis = redis;
		this.revocation = revocation;
		this.cookies = cookies;
		this.limitPerMinute = limitPerMinute;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
			chain.doFilter(request, response);
			return;
		}

		UUID userId = UUID.fromString(jwtAuth.getName());
		try {
			String key = "rl:" + userId + ":" + (Instant.now().getEpochSecond() / 60);
			Long count = redis.opsForValue().increment(key);
			if (count != null && count == 1) {
				redis.expire(key, Duration.ofMinutes(2));
			}
			if (count != null && count > limitPerMinute) {
				log.warn("Rate limit exceeded for user {}: {} requests this minute. Revoking tokens.", userId, count);
				revocation.revokeAllForUser(userId);
				response.addHeader(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString());
				response.addHeader(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		catch (RuntimeException e) {
			log.error("Rate limit store unavailable, rejecting request", e);
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}
		chain.doFilter(request, response);
	}
}
