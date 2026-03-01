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
    private static final String TEST_GENDER = "MALE";

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
            UserDto.V1.CreateUserRequest request = createUserRequest();
            HttpEntity<UserDto.V1.CreateUserRequest> requestEntity = new HttpEntity<>(request);

            // act
            ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> response = createUser(requestEntity);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> {
                    assertThat(response.getBody()).isNotNull();
                    UserDto.V1.UserResponse responseData = response.getBody().data();
                    assertThat(responseData).isNotNull();
                    assertThat(responseData.id()).isNotNull();
                    assertThat(responseData.userId()).isEqualTo(TEST_USER_ID);
                    assertThat(responseData.email()).isEqualTo(TEST_EMAIL);
                    assertThat(responseData.birthday()).isEqualTo(TEST_BIRTHDAY);
                    assertThat(responseData.gender()).isEqualTo(TEST_GENDER);
                    assertThat(responseData.createdAt()).isNotBlank();
                }
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenGenderIsNull() {
            // arrange
            UserDto.V1.CreateUserRequest request = new UserDto.V1.CreateUserRequest(TEST_USER_ID, TEST_EMAIL, TEST_BIRTHDAY, null);
            HttpEntity<UserDto.V1.CreateUserRequest> requestEntity = new HttpEntity<>(request);

            // act
            ResponseEntity<Object> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, requestEntity, Object.class);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
        }

        @DisplayName("이미 존재하는 userId로 회원가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returnsConflict_whenUserIdAlreadyExists() {
            // arrange
            HttpEntity<UserDto.V1.CreateUserRequest> first = new HttpEntity<>(createUserRequest());
            createUser(first);

            UserDto.V1.CreateUserRequest duplicateRequest =
                new UserDto.V1.CreateUserRequest(TEST_USER_ID, "another@naver.com", TEST_BIRTHDAY, TEST_GENDER);
            HttpEntity<UserDto.V1.CreateUserRequest> duplicate = new HttpEntity<>(duplicateRequest);

            // act
            ResponseEntity<Object> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, duplicate, Object.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @DisplayName("이미 존재하는 email로 회원가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returnsConflict_whenEmailAlreadyExists() {
            // arrange
            HttpEntity<UserDto.V1.CreateUserRequest> first = new HttpEntity<>(createUserRequest());
            createUser(first);

            UserDto.V1.CreateUserRequest duplicateRequest =
                new UserDto.V1.CreateUserRequest("newuser01", TEST_EMAIL, TEST_BIRTHDAY, TEST_GENDER);
            HttpEntity<UserDto.V1.CreateUserRequest> duplicate = new HttpEntity<>(duplicateRequest);

            // act
            ResponseEntity<Object> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, duplicate, Object.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    class Get {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenIdExists() {
            // arrange
            UserDto.V1.CreateUserRequest postRequest = createUserRequest();
            HttpEntity<UserDto.V1.CreateUserRequest> postRequestEntity = new HttpEntity<>(postRequest);
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
                    assertThat(responseData.createdAt()).isNotBlank();
                }
            );
        }

	        @DisplayName("X-USER-ID 헤더가 없을 경우, 401 Unauthorized 응답을 반환한다.")
	        @Test
	        void returnsUnauthorized_whenHeaderIsMissing() {
            // arrange
            ParameterizedTypeReference<ApiResponse<UserDto.V1.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };

            // act
            ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> response =
                testRestTemplate.exchange("/api/v1/users/me", HttpMethod.GET, null, responseType);

            // assert
	            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	        }

	        @DisplayName("X-USER-ID 헤더가 빈 값일 경우, 401 Unauthorized 응답을 반환한다.")
	        @Test
	        void returnsUnauthorized_whenHeaderIsBlank() {
	            // act
	            ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> response = getUserInfo("");

	            // assert
	            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	        }

	        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
	        @Test
	        void returnsNotFound_whenIdDoesNotExist() {
	            // act
	            ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> response = getUserInfo("notExistsUser");

	            // assert
	            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	        }
    }

    private UserDto.V1.CreateUserRequest createUserRequest() {
        return new UserDto.V1.CreateUserRequest(TEST_USER_ID, TEST_EMAIL, TEST_BIRTHDAY, TEST_GENDER);
    }

    private ResponseEntity<ApiResponse<UserDto.V1.UserResponse>> createUser(HttpEntity<UserDto.V1.CreateUserRequest> requestEntity) {
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
