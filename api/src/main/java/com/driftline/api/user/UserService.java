package com.driftline.api.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;

	public UserService(UserRepository users) {
		this.users = users;
	}

	/** Create the user on first GitHub login, refresh profile fields on later logins. */
	@Transactional
	public User upsertFromGithub(long githubId, String login, String name, String avatarUrl) {
		return users.findByGithubId(githubId)
			.map(existing -> {
				existing.updateProfile(login, name, avatarUrl);
				return existing;
			})
			.orElseGet(() -> users.save(new User(githubId, login, name, avatarUrl)));
	}
}
