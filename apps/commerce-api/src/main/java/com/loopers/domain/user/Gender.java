package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.util.Arrays;
import java.util.Locale;

import com.loopers.support.error.CoreException;

import lombok.Getter;

@Getter
public enum Gender {
	MALE, FEMALE, OTHER;

	public static Gender of(String value) {
		if (value == null || value.isBlank()) {
			throw new CoreException(BAD_REQUEST, GENDER_INVALID.getMessage());
		}

		String normalizedValue = value.trim().toUpperCase(Locale.ROOT);

		return Arrays.stream(Gender.values())
			.filter(gender -> gender.name().equals(normalizedValue))
			.findFirst()
			.orElseThrow(() -> new CoreException(BAD_REQUEST, GENDER_INVALID.getMessage()));
	}
}
