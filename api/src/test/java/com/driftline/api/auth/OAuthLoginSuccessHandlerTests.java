package com.driftline.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.driftline.api.TestcontainersConfiguration;
import com.driftline.api.config.AppProperties;
import com.driftline.api.user.UserRepository;
import com.driftline.api.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/**
 * The GitHub round trip cannot run in a test, so this feeds the handler what GitHub would hand back
 * and checks what the browser and the database end up with.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OAuthLoginSuccessHandlerTests {

	@Autowired UserService userService;
	@Autowired UserRepository users;
	@Autowired AccessTokenService accessTokens;
	@Autowired RefreshTokenService refreshTokens;
	@Autowired RefreshTokenRepository refreshRepo;
	@Autowired AuthCookies cookies;
	@Autowired AppProperties props;

	private OAuthLoginSuccessHandler handler() {
		return new OAuthLoginSuccessHandler(userService, accessTokens, refreshTokens, cookies, props);
	}

	private static OAuth2AuthenticationToken githubLogin(long id, String login) {
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		var principal = new DefaultOAuth2User(authorities,
			Map.of("id", id, "login", login, "name", "Octo Cat", "avatar_url", "https://example.test/a.png"), "id");
		return new OAuth2AuthenticationToken(principal, authorities, "github");
	}

	@Test
	void firstLoginCreatesTheUserSetsBothCookiesAndRedirects() throws Exception {
		long githubId = ThreadLocalRandom.current().nextLong(1, 1_000_000_000L);
		var request = new MockHttpServletRequest();
		var session = new MockHttpSession();
		request.setSession(session);
		var response = new MockHttpServletResponse();

		handler().onAuthenticationSuccess(request, response, githubLogin(githubId, "octo-first"));

		var user = users.findByGithubId(githubId).orElseThrow();
		assertThat(user.getGithubLogin()).isEqualTo("octo-first");
		assertThat(response.getRedirectedUrl()).isEqualTo(props.frontendUrl() + "/");
		assertThat(session.isInvalid()).as("login handshake session is dropped").isTrue();

		List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
		String access = setCookies.stream().filter(c -> c.startsWith("access_token=")).findFirst().orElseThrow();
		String refresh = setCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();
		assertThat(access).contains("HttpOnly").contains("Secure").contains("SameSite=Lax").contains("Path=/;");
		assertThat(access).contains("Max-Age=86400"); // 1 day
		assertThat(refresh).contains("HttpOnly").contains("Secure").contains("SameSite=Lax").contains("Path=/api/auth");
		assertThat(refresh).contains("Max-Age=2592000"); // 30 days

		// Only the hash of the refresh token is stored.
		String rawRefresh = refresh.substring("refresh_token=".length(), refresh.indexOf(';'));
		assertThat(refreshRepo.findByTokenHash(RefreshTokenService.hash(rawRefresh))).isPresent();
		assertThat(refreshRepo.findByTokenHash(rawRefresh)).isEmpty();
	}

	@Test
	void secondLoginUpdatesTheSameUserInsteadOfDuplicating() throws Exception {
		long githubId = ThreadLocalRandom.current().nextLong(1, 1_000_000_000L);

		handler().onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(),
			githubLogin(githubId, "old-name"));
		handler().onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(),
			githubLogin(githubId, "new-name"));

		assertThat(users.findAll().stream().filter(u -> u.getGithubId() == githubId)).hasSize(1);
		assertThat(users.findByGithubId(githubId).orElseThrow().getGithubLogin()).isEqualTo("new-name");
	}
}
