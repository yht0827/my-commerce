package com.loopers.interfaces.api.user;

import com.loopers.application.user.CreateUserCommand;
import com.loopers.application.user.UserResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserDto() {

	public record V1() {
		public record CreateUserRequest(
			@Schema(description = "회원 아이디", example = "loopers01")
			@NotBlank(message = "userId는 필수입니다.")
			@Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "userId는 영문/숫자 4~20자여야 합니다.")
			String userId,
			@Schema(description = "이메일", example = "loopers01@example.com")
			@NotBlank(message = "email은 필수입니다.")
			@Email(message = "올바른 이메일 형식이어야 합니다.")
			String email,
			@Schema(description = "생년월일(yyyy-MM-dd)", example = "2000-01-01")
			@NotBlank(message = "birthday는 필수입니다.")
			@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birthday는 yyyy-MM-dd 형식이어야 합니다.")
			String birthday,
			@Schema(description = "성별 (MALE, FEMALE, OTHER)", example = "MALE")
			@NotBlank(message = "gender는 필수입니다.")
			@Pattern(regexp = "(?i)MALE|FEMALE|OTHER", message = "gender는 MALE, FEMALE, OTHER 중 하나여야 합니다.")
			String gender
		) {
			public CreateUserCommand toCommand() {
				return new CreateUserCommand(userId, email, birthday, gender);
			}
		}

		public record UserResponse(
			@Schema(description = "회원 PK", example = "1")
			Long id,
			@Schema(description = "회원 아이디", example = "loopers01")
			String userId,
			@Schema(description = "이메일", example = "loopers01@example.com")
			String email,
			@Schema(description = "생년월일(yyyy-MM-dd)", example = "2000-01-01")
			String birthday,
			@Schema(description = "성별", example = "MALE")
			String gender,
			@Schema(description = "가입 시각", example = "2026-02-17T10:15:30+09:00")
			String createdAt
		) {
			public static UserResponse from(final UserResult info) {
				return new UserResponse(
					info.id(),
					info.userId(),
					info.email(),
					info.birthday(),
					info.gender(),
					info.createdAt()
				);
			}
		}
	}
}
