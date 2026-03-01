package com.loopers.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.user.Birthday;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserApplicationService {
	private final UserService userService;

	public UserResult register(final CreateUserCommand command) {
		User savedUser = userService.register(
			UserId.of(command.userId()),
			Email.of(command.email()),
			Birthday.of(command.birthday()),
			Gender.of(command.gender()));

		return UserResult.from(savedUser);
	}

	@Transactional(readOnly = true)
	public UserResult getUser(final GetUserQuery query) {
		final UserId userId = UserId.of(query.userId());

		User user = userService.findByUserId(userId);

		return UserResult.from(user);
	}

}
