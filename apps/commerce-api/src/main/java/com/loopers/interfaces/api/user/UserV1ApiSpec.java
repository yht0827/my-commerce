package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users V1 API", description = "Users API 입니다.")
public interface UserV1ApiSpec {

	@Operation(
		summary = "회원 가입",
		description = "회원 가입을 합니다."
	)
	ApiResponse<UserDto.V1.UserResponse> createUser(final UserDto.V1.CreateUserRequest request);

	@Operation(
		summary = "내 정보 조회",
		description = "내 정보를 조회 합니다."
	)
	ApiResponse<UserDto.V1.UserResponse> getUser(final String userId);
}
