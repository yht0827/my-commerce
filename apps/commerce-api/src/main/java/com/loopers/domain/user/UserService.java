package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

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

		validateCreatePolicy(user);

		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			if (isDuplicateKeyException(e)) {
				throw new CoreException(CONFLICT);
			}
			throw e;
		}
	}

	public User findByUserId(final UserId userId) {
		return userRepository.findByUserId(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, USER_NOT_FOUND.format(userId.getUserId())));
	}

	private void validateCreatePolicy(final User user) {
		validateUniqueUserId(user.getUserId());
		validateUniqueEmail(user.getEmail());
	}

	private void validateUniqueUserId(final UserId userId) {
		boolean isExisted = userRepository.existsByUserId(userId);
		if (isExisted) {
			throw new CoreException(CONFLICT, USER_ID_ALREADY_EXISTS.getMessage());
		}
	}

	private void validateUniqueEmail(final Email email) {
		boolean isExisted = userRepository.existsByEmail(email);
		if (isExisted) {
			throw new CoreException(CONFLICT, EMAIL_ALREADY_EXISTS.getMessage());
		}
	}

	private boolean isDuplicateKeyException(final DataIntegrityViolationException e) {
		Throwable mostSpecificCause = e.getMostSpecificCause();
		if (mostSpecificCause instanceof SQLIntegrityConstraintViolationException) {
			return true;
		}
		if (mostSpecificCause instanceof SQLException sqlException) {
			String sqlState = sqlException.getSQLState();
			return sqlState != null && sqlState.startsWith("23");
		}
		return false;
	}
}
