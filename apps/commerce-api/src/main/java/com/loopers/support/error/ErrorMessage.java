package com.loopers.support.error;

import lombok.Getter;

@Getter
public enum ErrorMessage {

	USER_ID_REQUIRED("사용자 ID는 필수입니다."),
	USER_NAME_REQUIRED("사용자 이름은 필수입니다."),
	USER_EMAIL_REQUIRED("사용자 이메일은 필수입니다."),
	USER_ID_ALREADY_EXISTS("이미 사용중인 ID입니다."),
	EMAIL_ALREADY_EXISTS("이미 사용중인 이메일입니다"),
	USER_NOT_FOUND("해당 사용자 ID '%s'의 회원을 찾을 수 없습니다."),
	BIRTHDAY_INVALID_FORMAT("생년월일은 yyyy-MM-dd 형식이어야 합니다."),
	BIRTHDAY_CANNOT_BE_FUTURE("생년월일은 미래일 수 없습니다."),
	USER_ID_INVALID_FORMAT("사용자 ID는 영문/숫자 조합이며 %d자 이상 %d자 이하여야 합니다."),
	USER_NAME_INVALID_FORMAT("사용자 이름은 한글, 영문만 가능하며 %d자 이내여야 합니다."),
	EMAIL_INVALID_FORMAT("유효하지 않은 이메일 형식입니다."),
	GENDER_INVALID("유효하지 않은 성별입니다."),

	POINT_NOT_FOUND("해당 사용자 ID '%s'의 포인트가 존재하지 않습니다."),
	POINT_BALANCE_REQUIRED("포인트 금액은 필수입니다."),
	POINT_BALANCE_INVALID("포인트 금액은 %d보다 커야 합니다.");

	private final String message;

	ErrorMessage(String message) {
		this.message = message;
	}

	public String format(Object... args) {
		return String.format(message, args);
	}
}
