package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("PointService 테스트")
class PointServiceTest {

	private PointRepository pointRepository;
	private PointHistoryRepository pointHistoryRepository;
	private PointService pointService;

	@BeforeEach
	void setUp() {
		pointRepository = mock(PointRepository.class);
		pointHistoryRepository = mock(PointHistoryRepository.class);
		pointService = new PointService(pointRepository, pointHistoryRepository);
	}

	@DisplayName("charge 메서드")
	@Nested
	class Charge {

		@DisplayName("포인트가 없으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenPointDoesNotExist() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(1000));
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> pointService.charge(userId, amount))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("포인트가 있으면 충전 후 반환한다")
		@Test
		void chargesAndReturnsPoint() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(1000));
			Point point = new Point(userId, new Balance(BigDecimal.valueOf(500)));
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(point));

			// act
			Point result = pointService.charge(userId, amount);

			// assert
			assertThat(result.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
			verify(pointHistoryRepository).save(any(PointHistory.class));
		}
	}

	@DisplayName("use 메서드")
	@Nested
	class Use {

		@DisplayName("포인트가 없으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenPointDoesNotExist() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(100));
			when(pointRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> pointService.use(userId, amount))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("포인트가 있으면 차감 후 반환한다")
		@Test
		void usesAndReturnsPoint() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(300));
			Point point = new Point(userId, new Balance(BigDecimal.valueOf(1000)));
			when(pointRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.of(point));

			// act
			Point result = pointService.use(userId, amount);

			// assert
			assertThat(result.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
			verify(pointHistoryRepository).save(any(PointHistory.class));
		}
	}

	@DisplayName("refund 메서드")
	@Nested
	class Refund {

		@DisplayName("포인트가 없으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenPointDoesNotExist() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(500));
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> pointService.refund(userId, amount))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("포인트가 있으면 환불 후 반환한다")
		@Test
		void refundsAndReturnsPoint() {
			// arrange
			UserId userId = new UserId("user1234");
			Balance amount = new Balance(BigDecimal.valueOf(500));
			Point point = new Point(userId, new Balance(BigDecimal.valueOf(200)));
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(point));

			// act
			Point result = pointService.refund(userId, amount);

			// assert
			assertThat(result.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
			verify(pointHistoryRepository).save(any(PointHistory.class));
		}
	}

	@DisplayName("findByUserId 메서드")
	@Nested
	class FindByUserId {

		@DisplayName("존재하지 않으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenNotExists() {
			// arrange
			UserId userId = new UserId("user1234");
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> pointService.findByUserId(userId))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("존재하면 포인트를 반환한다")
		@Test
		void returnsPointWhenExists() {
			// arrange
			UserId userId = new UserId("user1234");
			Point point = new Point(userId, new Balance(BigDecimal.valueOf(1000)));
			when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(point));

			// act
			Point result = pointService.findByUserId(userId);

			// assert
			assertThat(result).isEqualTo(point);
			assertThat(result.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
		}
	}
}
