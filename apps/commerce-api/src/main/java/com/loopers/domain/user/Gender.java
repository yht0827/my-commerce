package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.util.Arrays;

import com.loopers.support.error.CoreException;

import lombok.Getter;

@Getter
public enum Gender {
	MALE, FEMALE, OTHER;

	public static Gender of(String value) {
		return Arrays.stream(Gender.values())
			.filter(gender -> gender.name().equals(value))
			.findFirst()
			.orElseThrow(() -> new CoreException(BAD_REQUEST, GENDER_INVALID.getMessage()));
	}
}
