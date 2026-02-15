package com.loopers.interfaces.api.user;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;

@DisplayName("회원 E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private DatabaseCleanUp databaseCleanUp;

	private static final String TEST_USER_ID = "yht0827";
	private static final String TEST_EMAIL = "yht0827@naver.com";
	private static final String TEST_BIRTHDAY = "1999-01-01";
	private static final String TEST_GENDER = "M";

	@AfterEach
	void tearDown() {
		databaseCleanUp.truncateAllTables();
	}

	@DisplayName("POST /api/v1/users")
	@Nested
	class Post {
		@DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
		@Test
		void returnsExampleInfo_whenValidIdIsProvided() {
			// arrange
			UserDto.V1.UserRequest request = createUserRequest();
			HttpEntity<UserDto.V1.UserRequest> requestEntity = new HttpEntity<>(request);

			// act
			ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> response = createUser(requestEntity);

			// assert
			assertAll(
				() -> assertTrue(response.getStatusCode().is2xxSuccessful()),
				() -> {
					assertThat(response.getBody()).isNotNull();
					UserDto.V1.UserResponse responseData = response.getBody().data();
					assertThat(responseData).isNotNull();
					assertThat(responseData.id()).isNotNull();
					assertThat(responseData.userId()).isEqualTo(TEST_USER_ID);
					assertThat(responseData.email()).isEqualTo(TEST_EMAIL);
					assertThat(responseData.birthday()).isEqualTo(TEST_BIRTHDAY);
					assertThat(responseData.gender()).isEqualTo(TEST_GENDER);
				}
			);
		}

		@DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenGenderIsNull() {
			// arrange
			UserDto.V1.UserRequest request = new UserDto.V1.UserRequest(TEST_USER_ID, TEST_EMAIL, TEST_BIRTHDAY, null);
			HttpEntity<UserDto.V1.UserRequest> requestEntity = new HttpEntity<>(request);

			// act
			ResponseEntity<Object> response =
				testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, requestEntity, Object.class);

			// assert
			assertTrue(response.getStatusCode().is4xxClientError());
		}
	}

	@DisplayName("GET /api/v1/users")
	@Nested
	class Get {
		@DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
		@Test
		void returnsUserInfo_whenIdExists() {
			// arrange
			UserDto.V1.UserRequest postRequest = createUserRequest();
			HttpEntity<UserDto.V1.UserRequest> postRequestEntity = new HttpEntity<>(postRequest);
			ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> postResponse = createUser(postRequestEntity);
			assertThat(postResponse.getBody()).isNotNull();
			assertThat(postResponse.getBody().data()).isNotNull();
			String createdUserId = postResponse.getBody().data().userId();

			// act
			ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> getResponse = getUserInfo(createdUserId);

			// assert
			assertAll(
				() -> assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
				() -> {
					assertThat(getResponse.getBody()).isNotNull();
					UserDto.V1.UserResponse responseData = getResponse.getBody().data();
					assertThat(responseData).isNotNull();
					assertThat(responseData.userId()).isEqualTo(TEST_USER_ID);
					assertThat(responseData.email()).isEqualTo(TEST_EMAIL);
					assertThat(responseData.birthday()).isEqualTo(TEST_BIRTHDAY);
					assertThat(responseData.gender()).isEqualTo(TEST_GENDER);
				}
			);
		}

		@DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
		@Test
		void returnsNotFound_whenIdDoesNotExist() {
			// arrange
			long nonExistentId = 999L;
			String requestUrl = "/api/v1/users/" + nonExistentId;

			// act
			ResponseEntity<Object> response =
				testRestTemplate.exchange(requestUrl, HttpMethod.GET, null, Object.class);

			// assert
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	private UserDto.V1.UserRequest createUserRequest() {
		return new UserDto.V1.UserRequest(TEST_USER_ID, TEST_EMAIL, TEST_BIRTHDAY, TEST_GENDER);
	}

	private ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> createUser(HttpEntity<UserDto.V1.UserRequest> requestEntity) {
		ParameterizedTypeReference<ApiResponse<UserDto.V1.UserResponse>> responseType = new ParameterizedTypeReference<>() {
		};
		return testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, requestEntity, responseType);
	}

	private ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> getUserInfo(String userId) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ID", userId);
		HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

		ParameterizedTypeReference<ApiResponse<UserDto.V1.UserResponse>> responseType = new ParameterizedTypeReference<>() {
		};
		return testRestTemplate.exchange("/api/v1/users/me", HttpMethod.GET, requestEntity, responseType);
	}
}
