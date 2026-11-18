package com.driftline.api.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("delete from RefreshToken t where t.tokenHash = :tokenHash")
	int deleteByTokenHash(String tokenHash);

	@Query("select t.tokenHash from RefreshToken t where t.userId = :userId")
	List<String> findTokenHashesByUserId(UUID userId);

	@Modifying
	@Query("delete from RefreshToken t where t.userId = :userId")
	int deleteByUserId(UUID userId);
}
