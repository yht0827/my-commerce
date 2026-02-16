package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserResult(Long id, String userId, String email, String birthday, String gender, String createdAt) {
	public static UserResult from(final User user) {
		return new UserResult(
			user.getId(),
			user.getUserId().getUserId(),
			user.getEmail().getEmail(),
			user.getBirthday().getBirthday(),
			user.getGender().name(),
			user.getCreatedAt().toString()
		);
	}
}
