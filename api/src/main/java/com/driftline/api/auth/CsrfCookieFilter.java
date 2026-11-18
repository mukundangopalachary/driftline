package com.driftline.api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring creates the CSRF token lazily, so the XSRF-TOKEN cookie would only appear once something reads
 * the token. Reading it on every request makes sure the browser always gets the cookie. The frontend
 * copies it into the X-XSRF-TOKEN header on state-changing requests (decisions/0009, 0014).
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (token != null) {
			token.getToken();
		}
		chain.doFilter(request, response);
	}
}
