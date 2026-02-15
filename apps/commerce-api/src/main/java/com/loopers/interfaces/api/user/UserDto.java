package com.loopers.interfaces.api.user;

import com.loopers.application.user.CreateUserCommand;
import com.loopers.application.user.UserResult;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserDto() {

	public record V1() {
		public record UserRequest(
			@NotBlank(message = "userId는 필수입니다.")
			@Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "userId는 영문/숫자 4~20자여야 합니다.")
			String userId,
			@NotBlank(message = "email은 필수입니다.")
			@Email(message = "올바른 이메일 형식이어야 합니다.")
			String email,
			@NotBlank(message = "birthday는 필수입니다.")
			@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birthday는 yyyy-MM-dd 형식이어야 합니다.")
			String birthday,
			@NotBlank(message = "gender는 필수입니다.")
			@Pattern(regexp = "(?i)MALE|FEMALE|OTHER", message = "gender는 MALE, FEMALE, OTHER 중 하나여야 합니다.")
			String gender
		) {
			public CreateUserCommand toCommand() {
				return new CreateUserCommand(userId, email, birthday, gender);
			}
		}

		public record UserResponse(Long id, String userId, String email, String birthday, String gender) {
			public static UserResponse from(final UserResult info) {
				return new UserResponse(
					info.id(),
					info.userId(),
					info.email(),
					info.birthday(),
					info.gender()
				);
			}
		}
	}
}
