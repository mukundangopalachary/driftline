package com.driftline.api.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Reads the access token from the httpOnly cookie. The Authorization header is not accepted.
 *
 * Refresh and logout do not need a valid access token (it may be expired), so the token is not read
 * for those paths. Otherwise an expired cookie would be rejected before the endpoint could replace it.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

	@Override
	public String resolve(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (path.equals("/api/auth/refresh") || path.equals("/api/auth/logout")) {
			return null;
		}
		return readCookie(request, AuthCookies.ACCESS);
	}

	static String readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
