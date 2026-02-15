package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
public class Birthday implements Serializable {

	private static final Pattern BIRTHDAY_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	@Column(name = "birthday")
	private String birthday;

	public Birthday(String birthday) {
		if (birthday == null) {
			throw new CoreException(BAD_REQUEST, BIRTHDAY_INVALID_FORMAT.getMessage());
		}

		String normalizedBirthday = birthday.trim();
		if (!BIRTHDAY_PATTERN.matcher(normalizedBirthday).matches()) {
			throw new CoreException(BAD_REQUEST, BIRTHDAY_INVALID_FORMAT.getMessage());
		}

		LocalDate parsedBirthday;
		try {
			parsedBirthday = LocalDate.parse(normalizedBirthday);
		} catch (DateTimeParseException e) {
			throw new CoreException(BAD_REQUEST, BIRTHDAY_INVALID_FORMAT.getMessage());
		}

		if (parsedBirthday.isAfter(LocalDate.now())) {
			throw new CoreException(BAD_REQUEST, BIRTHDAY_CANNOT_BE_FUTURE.getMessage());
		}

		this.birthday = normalizedBirthday;
	}

	public static Birthday of(String birthday) {
		return new Birthday(birthday);
	}
}
