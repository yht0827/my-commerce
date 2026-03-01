package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;
import java.util.regex.Pattern;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserId implements Serializable {

	private static final Pattern VALID_USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,20}$");
	private static final int MIN_LENGTH = 4;
	private static final int MAX_LENGTH = 20;

	@Column(name = "user_id")
	private String userId;

	public UserId(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new CoreException(BAD_REQUEST, USER_ID_REQUIRED.getMessage());
		}

		String normalizedUserId = userId.trim();
		if (!VALID_USER_ID_PATTERN.matcher(normalizedUserId).matches()) {
			throw new CoreException(BAD_REQUEST, USER_ID_INVALID_FORMAT.format(MIN_LENGTH, MAX_LENGTH));
		}

		this.userId = normalizedUserId;
	}

	public static UserId of(String userId) {
		return new UserId(userId);
	}
}
