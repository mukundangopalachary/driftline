package com.driftline.api.config;

import com.driftline.api.auth.AccessTokenService;
import com.driftline.api.auth.AuthCookies;
import com.driftline.api.auth.CookieBearerTokenResolver;
import com.driftline.api.auth.CsrfCookieFilter;
import com.driftline.api.auth.OAuthLoginSuccessHandler;
import com.driftline.api.auth.RateLimitFilter;
import com.driftline.api.auth.RefreshTokenService;
import com.driftline.api.auth.RevocationService;
import com.driftline.api.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Security rules. Decisions: docs/decisions/0006, 0009, 0011, 0015.
 *
 * - Login: GitHub OAuth. After login, tokens travel only in httpOnly cookies.
 * - Every other request: access token (ES256 JWT) read from the cookie, checked against Redis revocation.
 * - CSRF: Spring Security's protection with the cookie repository (the single-page-app variant).
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, UserService users, AccessTokenService accessTokens,
			RefreshTokenService refreshTokens, RevocationService revocation, AuthCookies cookies,
			StringRedisTemplate redis, AppProperties props) throws Exception {
		http
			// IF_REQUIRED, not STATELESS: the OAuth login handshake keeps its state value in a short-lived
			// session. The success handler invalidates it. Nothing else creates a session.
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			// Plain handler, not the default XOR one, so the frontend can send the cookie value as the header.
			.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health/**").permitAll()
				.requestMatchers("/api/auth/oauth2/**").permitAll()
				// Refresh and logout check their own cookies. The access token may already be expired.
				.requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
				.anyRequest().authenticated())
			.oauth2Login(oauth -> oauth
				.authorizationEndpoint(a -> a.baseUri("/api/auth/oauth2/authorization"))
				.redirectionEndpoint(r -> r.baseUri("/api/auth/oauth2/callback/*"))
				.successHandler(new OAuthLoginSuccessHandler(users, accessTokens, refreshTokens, cookies, props))
				.failureHandler((request, response, exception) ->
					response.sendRedirect(props.frontendUrl() + "/login?error=oauth")))
			.oauth2ResourceServer(rs -> rs
				.bearerTokenResolver(new CookieBearerTokenResolver())
				.jwt(Customizer.withDefaults()))
			// APIs answer 401, they do not redirect to a login page.
			.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.logout(logout -> logout.disable())
			// The request cache saves denied requests in a session so it can replay them after a form login.
			// We never replay (the success handler redirects to the frontend), so it would only create sessions.
			.requestCache(cache -> cache.disable())
			.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
			.addFilterAfter(
				new RateLimitFilter(redis, revocation, cookies, props.rateLimit().requestsPerMinute()),
				BearerTokenAuthenticationFilter.class);
		return http.build();
	}
}
