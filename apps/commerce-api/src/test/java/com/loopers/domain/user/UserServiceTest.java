package com.loopers.domain.user;

import static com.loopers.support.error.ErrorMessage.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("회원 서비스 단위 테스트")
class UserServiceTest {

	private static final String USER_ID = "user1234";
	private static final String EMAIL = "user1234@test.com";
	private static final String BIRTHDAY = "1999-01-01";
	private static final String GENDER = "MALE";

	@Test
	@DisplayName("저장 시 userId 유니크 제약조건 위반이면, userId 중복 메시지와 함께 CONFLICT 예외로 변환한다.")
	void throwsConflictWithUserIdMessage_whenUserIdConstraintViolated() {
		// arrange
		UserRepository userRepository = mock(UserRepository.class);
		UserService userService = new UserService(userRepository);
		when(userRepository.existsByUserId(any())).thenReturn(false);
		when(userRepository.existsByEmail(any())).thenReturn(false);

		ConstraintViolationException duplicateConstraintViolation = new ConstraintViolationException(
			"duplicate key",
			new SQLException("duplicate"),
			"uk_users_user_id"
		);
		when(userRepository.save(any(User.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate key", duplicateConstraintViolation));

		// act
		CoreException result = assertThrows(
			CoreException.class,
			() -> userService.register(USER_ID, EMAIL, BIRTHDAY, GENDER)
		);

		// assert
		assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
		assertThat(result.getCustomMessage()).isEqualTo(USER_ID_ALREADY_EXISTS.getMessage());
	}

	@Test
	@DisplayName("저장 시 email 유니크 제약조건 위반이면, email 중복 메시지와 함께 CONFLICT 예외로 변환한다.")
	void throwsConflictWithEmailMessage_whenEmailConstraintViolated() {
		// arrange
		UserRepository userRepository = mock(UserRepository.class);
		UserService userService = new UserService(userRepository);
		when(userRepository.existsByUserId(any())).thenReturn(false);
		when(userRepository.existsByEmail(any())).thenReturn(false);

		ConstraintViolationException duplicateConstraintViolation = new ConstraintViolationException(
			"duplicate key",
			new SQLException("duplicate"),
			"uk_users_email"
		);
		when(userRepository.save(any(User.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate key", duplicateConstraintViolation));

		// act
		CoreException result = assertThrows(
			CoreException.class,
			() -> userService.register(USER_ID, EMAIL, BIRTHDAY, GENDER)
		);

		// assert
		assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
		assertThat(result.getCustomMessage()).isEqualTo(EMAIL_ALREADY_EXISTS.getMessage());
	}

	@Test
	@DisplayName("저장 시 알 수 없는 제약조건 위반이면, 기본 CONFLICT 예외로 변환한다.")
	void throwsDefaultConflict_whenUnknownConstraintViolated() {
		// arrange
		UserRepository userRepository = mock(UserRepository.class);
		UserService userService = new UserService(userRepository);
		when(userRepository.existsByUserId(any())).thenReturn(false);
		when(userRepository.existsByEmail(any())).thenReturn(false);

		ConstraintViolationException notDuplicateConstraintViolation = new ConstraintViolationException(
			"foreign key violation",
			new SQLException("fk violation"),
			"fk_orders_user"
		);
		DataIntegrityViolationException exception = new DataIntegrityViolationException(
			"integrity constraint violation",
			notDuplicateConstraintViolation
		);
		when(userRepository.save(any(User.class))).thenThrow(exception);

		// act
		CoreException result = assertThrows(
			CoreException.class,
			() -> userService.register(USER_ID, EMAIL, BIRTHDAY, GENDER)
		);

		// assert
		assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
		assertThat(result.getCustomMessage()).isNull();
	}

	@Test
	@DisplayName("저장 시 제약조건 위반이 아닌 DataIntegrityViolationException이면, 원본 예외를 그대로 던진다.")
	void rethrowsDataIntegrityViolation_whenCauseIsNotConstraintViolation() {
		// arrange
		UserRepository userRepository = mock(UserRepository.class);
		UserService userService = new UserService(userRepository);
		when(userRepository.existsByUserId(any())).thenReturn(false);
		when(userRepository.existsByEmail(any())).thenReturn(false);

		DataIntegrityViolationException exception = new DataIntegrityViolationException(
			"other integrity violation",
			new SQLException("other integrity violation")
		);
		when(userRepository.save(any(User.class))).thenThrow(exception);

		// act
		DataIntegrityViolationException result = assertThrows(
			DataIntegrityViolationException.class,
			() -> userService.register(USER_ID, EMAIL, BIRTHDAY, GENDER)
		);

		// assert
		assertThat(result).isSameAs(exception);
	}

}
