package com.driftline.api.auth;

import com.driftline.api.user.User;
import com.driftline.api.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserRepository users;
	private final AccessTokenService accessTokens;
	private final RefreshTokenService refreshTokens;
	private final RevocationService revocation;
	private final JwtDecoder jwtDecoder;
	private final AuthCookies cookies;

	public AuthController(UserRepository users, AccessTokenService accessTokens, RefreshTokenService refreshTokens,
			RevocationService revocation, JwtDecoder jwtDecoder, AuthCookies cookies) {
		this.users = users;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.revocation = revocation;
		this.jwtDecoder = jwtDecoder;
		this.cookies = cookies;
	}

	public record MeResponse(UUID id, String githubLogin, String name, String avatarUrl) {
	}

	/** The signed-in user. 401 when not signed in. */
	@GetMapping("/me")
	public MeResponse me(Authentication authentication) {
		User user = users.findById(UUID.fromString(authentication.getName()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return new MeResponse(user.getId(), user.getGithubLogin(), user.getName(), user.getAvatarUrl());
	}

	/**
	 * Swap a valid refresh token for a new access token. The refresh token itself stays the same until it
	 * expires after 30 days (decisions/0009). Needs the CSRF header like any other POST.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<Void> refresh(HttpServletRequest request) {
		String raw = CookieBearerTokenResolver.readCookie(request, AuthCookies.REFRESH);
		Optional<UUID> userId = refreshTokens.validate(raw);
		if (userId.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
				.header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
				.build();
		}
		var access = accessTokens.issue(userId.get());
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookies.access(access.value(), access.ttl()).toString())
			.build();
	}

	/** Sign out this browser: blacklist its access token, delete its refresh token, clear the cookies. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		String access = CookieBearerTokenResolver.readCookie(request, AuthCookies.ACCESS);
		if (access != null) {
			try {
				Jwt jwt = jwtDecoder.decode(access);
				revocation.revokeAccessToken(jwt.getId(), jwt.getExpiresAt());
			}
			catch (RuntimeException e) {
				// Already invalid or expired. Nothing to blacklist.
			}
		}
		refreshTokens.delete(CookieBearerTokenResolver.readCookie(request, AuthCookies.REFRESH));
		return cleared();
	}

	/** Sign out everywhere: revoke every access token and delete every refresh token of this user. */
	@PostMapping("/logout-all")
	public ResponseEntity<Void> logoutAll(Authentication authentication) {
		revocation.revokeAllForUser(UUID.fromString(authentication.getName()));
		return cleared();
	}

	private ResponseEntity<Void> cleared() {
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
			.header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
			.build();
	}

	/** Redis is required for token checks. When it is down, reject (decisions/0009, 0012). */
	@ExceptionHandler(RedisConnectionFailureException.class)
	public ResponseEntity<Void> redisDown() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
	}
}
