package com.driftline.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.driftline.api.user.User;
import com.driftline.api.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Small helpers shared by the auth tests. */
final class TestSupport {

	private TestSupport() {
	}

	static User newUser(UserRepository users) {
		long githubId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
		return users.save(new User(githubId, "octo-" + githubId, "Octo Cat", "https://example.test/a.png"));
	}

	static Cookie access(String token) {
		return new Cookie(AuthCookies.ACCESS, token);
	}

	static Cookie refresh(String token) {
		return new Cookie(AuthCookies.REFRESH, token);
	}

	static MockHttpServletRequestBuilder getMe(String accessToken) {
		return get("/api/auth/me").cookie(access(accessToken));
	}

	/** A POST that carries a valid CSRF token the way the frontend sends it: cookie plus header. */
	static MockHttpServletRequestBuilder postWithCsrf(String url, Cookie... cookies) {
		Cookie[] all = new Cookie[cookies.length + 1];
		System.arraycopy(cookies, 0, all, 0, cookies.length);
		all[cookies.length] = new Cookie("XSRF-TOKEN", "test-csrf-token");
		return post(url).cookie(all).header("X-XSRF-TOKEN", "test-csrf-token");
	}
}
