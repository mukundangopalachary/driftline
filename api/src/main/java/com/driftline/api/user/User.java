package com.driftline.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "github_id", nullable = false, unique = true)
	private long githubId;

	@Column(name = "github_login", nullable = false)
	private String githubLogin;

	private String name;

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(long githubId, String githubLogin, String name, String avatarUrl) {
		this.githubId = githubId;
		this.githubLogin = githubLogin;
		this.name = name;
		this.avatarUrl = avatarUrl;
	}

	// updated_at is maintained by the application, not triggers (decisions/0013).
	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void updateProfile(String githubLogin, String name, String avatarUrl) {
		this.githubLogin = githubLogin;
		this.name = name;
		this.avatarUrl = avatarUrl;
	}

	public UUID getId() {
		return id;
	}

	public long getGithubId() {
		return githubId;
	}

	public String getGithubLogin() {
		return githubLogin;
	}

	public String getName() {
		return name;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}
}
