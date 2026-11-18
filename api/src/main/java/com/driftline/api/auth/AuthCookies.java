package com.driftline.api.auth;

import com.driftline.api.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the two auth cookies (decisions/0009). Both are HttpOnly so page JavaScript cannot read them.
 * The refresh cookie is scoped to /api/auth so it is only sent to the refresh and logout endpoints.
 */
@Component
public class AuthCookies {

	public static final String ACCESS = "access_token";
	public static final String REFRESH = "refresh_token";
	static final String REFRESH_PATH = "/api/auth";

	private final boolean secure;

	public AuthCookies(AppProperties props) {
		this.secure = props.cookie().secure();
	}

	public ResponseCookie access(String value, Duration maxAge) {
		return build(ACCESS, value, "/", maxAge);
	}

	public ResponseCookie refresh(String value, Duration maxAge) {
		return build(REFRESH, value, REFRESH_PATH, maxAge);
	}

	public ResponseCookie clearAccess() {
		return build(ACCESS, "", "/", Duration.ZERO);
	}

	public ResponseCookie clearRefresh() {
		return build(REFRESH, "", REFRESH_PATH, Duration.ZERO);
	}

	private ResponseCookie build(String name, String value, String path, Duration maxAge) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.path(path)
			.maxAge(maxAge)
			.build();
	}
}
