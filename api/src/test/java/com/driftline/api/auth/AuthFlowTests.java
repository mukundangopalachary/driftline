package com.driftline.api.auth;

import static com.driftline.api.auth.TestSupport.access;
import static com.driftline.api.auth.TestSupport.getMe;
import static com.driftline.api.auth.TestSupport.newUser;
import static com.driftline.api.auth.TestSupport.postWithCsrf;
import static com.driftline.api.auth.TestSupport.refresh;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driftline.api.TestKeys;
import com.driftline.api.TestcontainersConfiguration;
import com.driftline.api.user.User;
import com.driftline.api.user.UserRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowTests {

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired AccessTokenService accessTokens;
	@Autowired RefreshTokenService refreshTokens;
	@Autowired RefreshTokenRepository refreshRepo;
	@Autowired StringRedisTemplate redis;
	@Autowired ECKey signingKey;

	// ---- access token checks ----

	@Test
	void validAccessCookieReturnsTheUser() throws Exception {
		User user = newUser(users);
		String token = accessTokens.issue(user.getId()).value();

		mvc.perform(getMe(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(user.getId().toString()))
			.andExpect(jsonPath("$.githubLogin").value(user.getGithubLogin()));
	}

	@Test
	void noCookieIs401() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void authorizationHeaderIsNotAccepted() throws Exception {
		User user = newUser(users);
		String token = accessTokens.issue(user.getId()).value();

		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokenIs401() throws Exception {
		User user = newUser(users);
		String token = accessTokens.issue(user.getId()).value();
		String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

		mvc.perform(getMe(tampered)).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherKeyIs401() throws Exception {
		User user = newUser(users);
		TestKeys.Pair other = TestKeys.generate();
		String forged = encode(other, user.getId(), Instant.now().plus(Duration.ofHours(1)));

		mvc.perform(getMe(forged)).andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIs401() throws Exception {
		User user = newUser(users);
		// Signed with the real key, so only the expiry is wrong.
		ECKey real = new ECKey.Builder(Curve.P_256, signingKey.toECPublicKey())
			.privateKey(signingKey.toECPrivateKey()).build();
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(real)));
		Instant issued = Instant.now().minus(Duration.ofDays(2));
		String expired = encoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(SignatureAlgorithm.ES256).build(),
			JwtClaimsSet.builder().issuer("driftline").subject(user.getId().toString())
				.id(UUID.randomUUID().toString()).issuedAt(issued).expiresAt(issued.plus(Duration.ofDays(1))).build()))
			.getTokenValue();

		mvc.perform(getMe(expired)).andExpect(status().isUnauthorized());
	}

	@Test
	void unsignedAlgNoneTokenIs401() throws Exception {
		User user = newUser(users);
		String header = b64url("{\"alg\":\"none\"}");
		String payload = b64url("{\"iss\":\"driftline\",\"sub\":\"" + user.getId() + "\",\"jti\":\"x\","
			+ "\"iat\":" + Instant.now().getEpochSecond() + ",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}");

		mvc.perform(getMe(header + "." + payload + ".")).andExpect(status().isUnauthorized());
	}

	// ---- refresh ----

	@Test
	void refreshIssuesANewAccessToken() throws Exception {
		User user = newUser(users);
		String refreshToken = refreshTokens.issue(user.getId()).value();

		MvcResult result = mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken)))
			.andExpect(status().isNoContent())
			.andReturn();

		String setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
			.filter(c -> c.startsWith("access_token=")).findFirst().orElseThrow();
		assertThat(setCookie).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
		String newToken = setCookie.substring("access_token=".length(), setCookie.indexOf(';'));
		mvc.perform(getMe(newToken)).andExpect(status().isOk());
	}

	@Test
	void refreshStillWorksWhenRedisHasNoCacheEntry() throws Exception {
		User user = newUser(users);
		String refreshToken = refreshTokens.issue(user.getId()).value();
		redis.delete("refresh:" + RefreshTokenService.hash(refreshToken)); // simulate a cache miss

		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken)))
			.andExpect(status().isNoContent());

		// The cache is refilled after the Postgres lookup.
		assertThat(redis.hasKey("refresh:" + RefreshTokenService.hash(refreshToken))).isTrue();
	}

	@Test
	void refreshWorksEvenWhenTheAccessCookieIsExpiredOrJunk() throws Exception {
		User user = newUser(users);
		String refreshToken = refreshTokens.issue(user.getId()).value();

		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken), access("expired.or.junk")))
			.andExpect(status().isNoContent());
	}

	@Test
	void unknownRefreshTokenIs401AndClearsCookies() throws Exception {
		MvcResult result = mvc.perform(postWithCsrf("/api/auth/refresh", refresh("not-a-real-token")))
			.andExpect(status().isUnauthorized())
			.andReturn();

		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anyMatch(c -> c.startsWith("access_token=") && c.contains("Max-Age=0"))
			.anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0"));
	}

	@Test
	void expiredRefreshTokenIs401() throws Exception {
		User user = newUser(users);
		String raw = "expired-refresh-" + UUID.randomUUID();
		refreshRepo.save(new RefreshToken(user.getId(), RefreshTokenService.hash(raw), Instant.now().minusSeconds(60)));

		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(raw))).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshTokenIsStoredAsAHashNotRaw() {
		User user = newUser(users);
		String raw = refreshTokens.issue(user.getId()).value();

		assertThat(refreshRepo.findByTokenHash(RefreshTokenService.hash(raw))).isPresent();
		assertThat(refreshRepo.findByTokenHash(raw)).isEmpty();
		assertThat(redis.hasKey("refresh:" + raw)).isFalse();
	}

	// ---- CSRF ----

	@Test
	void postWithoutCsrfTokenIs403() throws Exception {
		User user = newUser(users);
		String refreshToken = refreshTokens.issue(user.getId()).value();

		mvc.perform(post("/api/auth/refresh").cookie(refresh(refreshToken))).andExpect(status().isForbidden());
		mvc.perform(post("/api/auth/logout").cookie(refresh(refreshToken))).andExpect(status().isForbidden());
	}

	@Test
	void csrfCookieIsSetOnEveryResponseAndReadableByScript() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andExpect(cookie().httpOnly("XSRF-TOKEN", false));
	}

	@Test
	void ordinaryRequestsNeverCreateAServerSession() throws Exception {
		MvcResult denied = mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
			.andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
		assertThat(denied.getRequest().getSession(false)).isNull();

		User user = newUser(users);
		MvcResult allowed = mvc.perform(getMe(accessTokens.issue(user.getId()).value()))
			.andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
		assertThat(allowed.getRequest().getSession(false)).isNull();
	}

	// ---- logout ----

	@Test
	void logoutBlacklistsTheAccessTokenAndDeletesTheRefreshToken() throws Exception {
		User user = newUser(users);
		String accessToken = accessTokens.issue(user.getId()).value();
		String refreshToken = refreshTokens.issue(user.getId()).value();
		mvc.perform(getMe(accessToken)).andExpect(status().isOk());

		MvcResult result = mvc.perform(postWithCsrf("/api/auth/logout", access(accessToken), refresh(refreshToken)))
			.andExpect(status().isNoContent())
			.andReturn();

		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anyMatch(c -> c.startsWith("access_token=") && c.contains("Max-Age=0"))
			.anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0") && c.contains("Path=/api/auth"));
		// The old tokens are dead even if someone kept a copy.
		mvc.perform(getMe(accessToken)).andExpect(status().isUnauthorized());
		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken))).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutWorksWithoutAnyCookies() throws Exception {
		mvc.perform(postWithCsrf("/api/auth/logout")).andExpect(status().isNoContent());
	}

	@Test
	void logoutAllRevokesEveryTokenOfThatUserOnly() throws Exception {
		User user = newUser(users);
		User bystander = newUser(users);
		String accessToken = accessTokens.issue(user.getId()).value();
		String refreshToken = refreshTokens.issue(user.getId()).value();
		String otherAccess = accessTokens.issue(bystander.getId()).value();
		String otherRefresh = refreshTokens.issue(bystander.getId()).value();

		mvc.perform(postWithCsrf("/api/auth/logout-all", access(accessToken))).andExpect(status().isNoContent());

		mvc.perform(getMe(accessToken)).andExpect(status().isUnauthorized());
		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(refreshToken))).andExpect(status().isUnauthorized());
		mvc.perform(getMe(otherAccess)).andExpect(status().isOk());
		mvc.perform(postWithCsrf("/api/auth/refresh", refresh(otherRefresh))).andExpect(status().isNoContent());
	}

	@Test
	void signingInAgainAfterLogoutAllWorks() throws Exception {
		User user = newUser(users);
		String oldAccess = accessTokens.issue(user.getId()).value();
		mvc.perform(postWithCsrf("/api/auth/logout-all", access(oldAccess))).andExpect(status().isNoContent());

		// Tokens issued in the same second as the revoke count as revoked. A real login takes longer than that.
		Thread.sleep(1100);
		String newAccess = accessTokens.issue(user.getId()).value();

		mvc.perform(getMe(newAccess)).andExpect(status().isOk());
		mvc.perform(getMe(oldAccess)).andExpect(status().isUnauthorized());
	}

	@Test
	void everythingUnderApiNeedsAuth() throws Exception {
		mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
		mvc.perform(postWithCsrf("/api/auth/logout-all")).andExpect(status().isUnauthorized());
	}

	// ---- helpers ----

	private String encode(TestKeys.Pair keys, UUID userId, Instant expiresAt) {
		ECKey key = new ECKey.Builder(Curve.P_256, (ECPublicKey) keys.keyPair().getPublic())
			.privateKey((ECPrivateKey) keys.keyPair().getPrivate()).build();
		Instant now = Instant.now();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
			.encode(JwtEncoderParameters.from(
				JwsHeader.with(SignatureAlgorithm.ES256).build(),
				JwtClaimsSet.builder().issuer("driftline").subject(userId.toString())
					.id(UUID.randomUUID().toString()).issuedAt(now).expiresAt(expiresAt).build()))
			.getTokenValue();
	}

	private static String b64url(String s) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}
}
