package com.driftline.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Settings under the {@code driftline.*} prefix. See application.yml. */
@Validated
@ConfigurationProperties("driftline")
public record AppProperties(
		@NotBlank String frontendUrl,
		@Valid @NotNull Jwt jwt,
		@NotNull Duration refreshTtl,
		@Valid @NotNull Cookie cookie,
		@Valid @NotNull RateLimit rateLimit) {

	public record Jwt(
			@NotBlank(message = "JWT_PRIVATE_KEY_B64 is not set. Run scripts/gen-dev-keys.sh") String privateKeyB64,
			@NotBlank(message = "JWT_PUBLIC_KEY_B64 is not set. Run scripts/gen-dev-keys.sh") String publicKeyB64,
			@NotBlank String issuer,
			@NotNull Duration accessTtl) {
	}

	public record Cookie(boolean secure) {
	}

	public record RateLimit(@Positive int requestsPerMinute) {
	}
}
