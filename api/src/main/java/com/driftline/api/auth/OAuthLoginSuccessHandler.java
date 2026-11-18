package com.driftline.api.auth;

import com.driftline.api.config.AppProperties;
import com.driftline.api.user.User;
import com.driftline.api.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Runs after GitHub confirms the login (decisions/0006, 0011). Creates or updates the user, issues the
 * access token and refresh token as httpOnly cookies, and sends the browser back to the frontend.
 *
 * The OAuth handshake itself uses a short-lived server session for its state value (Spring's default).
 * It is invalidated here, so no session outlives login. After that the API is stateless.
 */
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final UserService users;
	private final AccessTokenService accessTokens;
	private final RefreshTokenService refreshTokens;
	private final AuthCookies cookies;
	private final String frontendUrl;

	public OAuthLoginSuccessHandler(UserService users, AccessTokenService accessTokens,
			RefreshTokenService refreshTokens, AuthCookies cookies, AppProperties props) {
		this.users = users;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.cookies = cookies;
		this.frontendUrl = props.frontendUrl();
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		OAuth2User principal = (OAuth2User) authentication.getPrincipal();
		Map<String, Object> attrs = principal.getAttributes();

		long githubId = ((Number) attrs.get("id")).longValue();
		User user = users.upsertFromGithub(
			githubId,
			(String) attrs.get("login"),
			(String) attrs.get("name"),
			(String) attrs.get("avatar_url"));

		var access = accessTokens.issue(user.getId());
		var refresh = refreshTokens.issue(user.getId());
		response.addHeader(HttpHeaders.SET_COOKIE, cookies.access(access.value(), access.ttl()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, cookies.refresh(refresh.value(), refresh.ttl()).toString());

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		response.sendRedirect(frontendUrl + "/");
	}
}
