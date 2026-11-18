package com.driftline.api.auth;

import static com.driftline.api.auth.TestSupport.getMe;
import static com.driftline.api.auth.TestSupport.newUser;
import static com.driftline.api.auth.TestSupport.postWithCsrf;
import static com.driftline.api.auth.TestSupport.refresh;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driftline.api.TestcontainersConfiguration;
import com.driftline.api.user.User;
import com.driftline.api.user.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** The limit is lowered to 5 per minute so the test can cross it quickly. Production default is 300. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "driftline.rate-limit.requests-per-minute=5")
class RateLimitTests {

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired AccessTokenService accessTokens;
	@Autowired RefreshTokenService refreshTokens;

	/** Counters use fixed one-minute windows. Do not start a test in the last seconds of a window. */
	@BeforeEach
	void avoidWindowBoundary() throws InterruptedException {
		long second = Instant.now().getEpochSecond() % 60;
		if (second >= 55) {
			Thread.sleep((60 - second + 1) * 1000);
		}
	}

	@Test
	void goingOverTheLimitRevokesAllTokensAndReturns401() throws Exception {
		User user = newUser(users);
		String accessToken = accessTokens.issue(user.getId()).value();
		String refreshToken = refreshTokens.issue(user.getId()).value();

		for (int i = 0; i < 5; i++) {
			mvc.perform(getMe(accessToken)).andExpect(status().isOk());
		}
		MvcResult sixth = mvc.perform(getMe(accessToken)).andExpect(status().isUnauthorized()).andReturn();

		assertThat(sixth.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anyMatch(c -> c.startsWith("access_token=") && c.contains("Max-Age=0"))
			.anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0"));
		// Both tokens are now dead, not just rejected for this one request.
		mvc.perform(getMe(accessToken)).andExpect(status().isUnauthorized());
		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken))).andExpect(status().isUnauthorized());
	}

	@Test
	void limitIsPerUser() throws Exception {
		User heavy = newUser(users);
		User calm = newUser(users);
		String heavyToken = accessTokens.issue(heavy.getId()).value();
		String calmToken = accessTokens.issue(calm.getId()).value();

		for (int i = 0; i < 6; i++) {
			mvc.perform(getMe(heavyToken));
		}

		mvc.perform(getMe(calmToken)).andExpect(status().isOk());
	}

	@Test
	void unauthenticatedEndpointsAreNotLimited() throws Exception {
		for (int i = 0; i < 20; i++) {
			mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		}
	}
}
