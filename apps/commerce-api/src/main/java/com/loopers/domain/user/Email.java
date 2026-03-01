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
public class Email implements Serializable {

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
		"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

	@Column(name = "email")
	private String email;

	public Email(String email) {
		if (email == null || email.isBlank()) {
			throw new CoreException(BAD_REQUEST, USER_EMAIL_REQUIRED.getMessage());
		}
		String normalizedEmail = email.trim();
		if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
			throw new CoreException(BAD_REQUEST, EMAIL_INVALID_FORMAT.getMessage());
		}
		this.email = normalizedEmail;
	}

	public static Email of(String email) {
		return new Email(email);
	}
}
