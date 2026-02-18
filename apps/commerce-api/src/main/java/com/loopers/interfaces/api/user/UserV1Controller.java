package com.loopers.interfaces.api.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.user.CreateUserCommand;
import com.loopers.application.user.GetUserQuery;
import com.loopers.application.user.UserApplicationService;
import com.loopers.application.user.UserResult;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.interfaces.api.common.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

	private final UserApplicationService userApplicationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Override
	public ApiResponse<UserDto.V1.UserResponse> createUser(@Valid @RequestBody final UserDto.V1.CreateUserRequest request) {
		CreateUserCommand command = request.toCommand();
		UserResult userInfo = userApplicationService.register(command);
		UserDto.V1.UserResponse response = UserDto.V1.UserResponse.from(userInfo);
		return ApiResponse.success(response);
	}

	@GetMapping("/me")
	@Override
	public ApiResponse<UserDto.V1.UserResponse> getUser(@CurrentUserId final String userId) {
		GetUserQuery query = GetUserQuery.of(userId);
		UserResult userInfo = userApplicationService.getUser(query);
		UserDto.V1.UserResponse response = UserDto.V1.UserResponse.from(userInfo);
		return ApiResponse.success(response);
	}

}
