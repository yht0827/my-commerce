package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private static final String USER_ID_UNIQUE_CONSTRAINT = "uk_users_user_id";
	private static final String USER_EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

	private final UserRepository userRepository;

	public User register(final UserId userId, final Email email, final Birthday birthday, final Gender gender) {

		User user = User.create()
			.userId(userId)
			.email(email)
			.birthday(birthday)
			.gender(gender)
			.build();

		validateCreatePolicy(user);

		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			throw mapUserConflictException(e);
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

	private RuntimeException mapUserConflictException(final DataIntegrityViolationException e) {
		if (!(e.getCause() instanceof ConstraintViolationException constraintViolation)) {
			return e;
		}

		String constraintName = constraintViolation.getConstraintName();
		if (USER_ID_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
			return new CoreException(CONFLICT, USER_ID_ALREADY_EXISTS.getMessage());
		}
		if (USER_EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
			return new CoreException(CONFLICT, EMAIL_ALREADY_EXISTS.getMessage());
		}

		return new CoreException(CONFLICT);
	}
}
