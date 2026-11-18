package com.driftline.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Postgres and Redis for tests (needs Docker running), plus a fresh JWT key pair per context so
 * tests never depend on a developer's .env.
 *
 * One Postgres and one Redis are shared by every Spring context in the test run. Starting a pair per
 * context ran the machine out of memory and made later contexts fail to connect. Tests stay independent
 * because each creates its own users, and all Redis keys include the user id. Testcontainers' Ryuk
 * sidecar removes the containers when the run ends.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Keep the Postgres image in step with docker-compose.yml.
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	private static final GenericContainer<?> REDIS =
		new GenericContainer<>(DockerImageName.parse("redis:8")).withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@Bean
	DynamicPropertyRegistrar testInfrastructure() {
		TestKeys.Pair keys = TestKeys.generate();
		return registry -> {
			registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
			registry.add("spring.datasource.username", POSTGRES::getUsername);
			registry.add("spring.datasource.password", POSTGRES::getPassword);
			registry.add("spring.data.redis.host", REDIS::getHost);
			registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
			registry.add("driftline.jwt.private-key-b64", keys::privateKeyB64);
			registry.add("driftline.jwt.public-key-b64", keys::publicKeyB64);
		};
	}
}
