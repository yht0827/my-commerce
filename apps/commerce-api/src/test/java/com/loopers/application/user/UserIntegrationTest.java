package com.loopers.application.user;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest
@DisplayName("회원 통합 테스트")
public class UserIntegrationTest {

    @Autowired
    private UserApplicationService userApplicationService;

    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String TEST_USER_ID = "yht0827";
    private static final String TEST_EMAIL = "yht0827@naver.com";
    private static final String TEST_BIRTHDAY = "1999-01-01";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private CreateUserCommand createTestCommand() {
        return new CreateUserCommand(TEST_USER_ID, TEST_EMAIL, TEST_BIRTHDAY, "MALE");
    }

    private void assertUserInfoEquals(UserResult info) {
        assertAll(
            () -> assertThat(info).isNotNull(),
            () -> assertThat(info.userId()).isEqualTo(TEST_USER_ID),
            () -> assertThat(info.email()).isEqualTo(TEST_EMAIL),
            () -> assertThat(info.birthday()).isEqualTo(TEST_BIRTHDAY),
            () -> assertThat(info.gender()).isEqualTo(Gender.MALE.name())
        );
    }

    private void assertUserEquals(User user) {
        assertAll(
            () -> assertThat(user).isNotNull(),
            () -> assertThat(user.getUserId().getUserId()).isEqualTo(TEST_USER_ID),
            () -> assertThat(user.getEmail().getEmail()).isEqualTo(TEST_EMAIL),
            () -> assertThat(user.getBirthday().getBirthday()).isEqualTo(TEST_BIRTHDAY),
            () -> assertThat(user.getGender()).isEqualTo(Gender.MALE)
        );
    }

    @Nested
    class Read {
        @DisplayName("유저를 조회할 때,")
        @Nested
        class GetUserTest {

            @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
            @Test
            void shouldReturnUserInfo_whenUserExists() {
                // arrange
                UserResult created = userApplicationService.register(createTestCommand());

                // act
                UserResult found = userApplicationService.getUser(GetUserQuery.of(created.userId()));

                // assert
                assertUserInfoEquals(found);
            }

            @DisplayName("해당 ID의 회원이 존재하지 않을 경우, 예외가 발생한다.")
            @Test
            void shouldThrowException_whenUserDoesNotExist() {
                // arrange
                String nonExistentUserId = "nonExistent";

                // act and assert
                CoreException result = assertThrows(
                    CoreException.class,
                    () -> userApplicationService.getUser(GetUserQuery.of(nonExistentUserId))
                );
	                assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            }
        }
    }

    @Nested
    class Create {
        @DisplayName("유저를 생성할 때,")
        @Nested
        class CreateUserTest {

            @DisplayName("올바른 정보로 회원 가입시, User 저장이 수행된다.")
            @Test
            void shouldSaveUser_whenValidUserCommandProvided() {
                // arrange
                CreateUserCommand command = createTestCommand();

                // act
                UserResult result = userApplicationService.register(command);

                // assert
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userJpaRepository, times(1)).save(captor.capture());
                User savedUser = captor.getValue();

                assertUserInfoEquals(result);
                assertUserEquals(savedUser);
            }

            @DisplayName("이미 가입된 ID로 회원가입 시도 시, 예외가 발생한다.")
            @Test
            void shouldThrowException_whenUserIdAlreadyExists() {
                // arrange
                CreateUserCommand command = createTestCommand();
                userApplicationService.register(command);

                CreateUserCommand duplicate = createTestCommand();

                // act and assert
                CoreException result = assertThrows(CoreException.class, () -> userApplicationService.register(duplicate));
                assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            }
        }
    }
}
