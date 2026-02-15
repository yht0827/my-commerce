package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	public User register(final String userId, final String email, final String birthday, final String gender) {

		User user = User.create()
			.userId(UserId.of(userId))
			.email(Email.of(email))
			.birthday(Birthday.of(birthday))
			.gender(Gender.of(gender))
			.build();

		ensureCreatePolicy(user);

		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			throw new CoreException(BAD_REQUEST, USER_ID_ALREADY_EXISTS.getMessage());
		}
	}

	public User findByUserId(final UserId userId) {
		return userRepository.findByUserId(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, USER_NOT_FOUND.format(userId)));
	}

	private void validateUniqueUserId(final UserId userId) {
		boolean isExisted = userRepository.existsByUserId(userId);
		if (isExisted) {
			throw new CoreException(BAD_REQUEST, USER_ID_ALREADY_EXISTS.getMessage());
		}
	}

	private void validateUniqueEmail(final Email email) {
		boolean isExisted = userRepository.existsByEmail(email);
		if (isExisted) {
			throw new CoreException(BAD_REQUEST, EMAIL_ALREADY_EXISTS.getMessage());
		}
	}

	private void ensureCreatePolicy(final User user) {
		validateUniqueUserId(user.getUserId());
		validateUniqueEmail(user.getEmail());
	}
}
