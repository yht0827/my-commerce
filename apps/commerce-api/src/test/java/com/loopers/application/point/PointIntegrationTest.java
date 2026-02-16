package com.loopers.application.point;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.domain.point.Balance;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointHistoryType;
import com.loopers.domain.user.UserId;
import com.loopers.infrastructure.point.PointHistoryJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest
@DisplayName("포인트 통합 테스트")
public class PointIntegrationTest {

	@Autowired
	private PointApplicationService pointApplicationService;

	@Autowired
	private PointJpaRepository pointJpaRepository;

	@Autowired
	private PointHistoryJpaRepository pointHistoryJpaRepository;

	@Autowired
	private DatabaseCleanUp databaseCleanUp;

	private static final String TEST_USER_ID = "yht0827";
	private static final BigDecimal INITIAL_AMOUNT = BigDecimal.valueOf(1000L);
	private static final BigDecimal CHARGE_AMOUNT = BigDecimal.valueOf(500L);

	@AfterEach
	void tearDown() {
		databaseCleanUp.truncateAllTables();
	}

	private Point createTestPoint() {
		return Point.create()
			.userId(new UserId(TEST_USER_ID))
			.balance(new Balance(INITIAL_AMOUNT))
			.build();
	}

	@DisplayName("포인트를 조회할 때,")
	@Nested
	class GetPoint {

		@DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
		@Test
		void shouldReturnPoint_whenUserExists() {
			// arrange
			Point point = createTestPoint();
			pointJpaRepository.save(point);

			GetPointQuery query = GetPointQuery.of(TEST_USER_ID);

			// act
			PointResult result = pointApplicationService.getPoint(query);

			// assert
			assertThat(result).isNotNull();
			assertThat(result.userId()).isEqualTo(TEST_USER_ID);
			assertThat(result.balance()).isEqualByComparingTo(INITIAL_AMOUNT);
		}

		@DisplayName("해당 ID의 회원이 존재하지 않을 경우, 예외가 발생한다.")
		@Test
		void shouldThrowException_whenUserDoesNotExist() {
			// arrange
			String nonExistentUserId = "nonExistent";
			GetPointQuery query = GetPointQuery.of(nonExistentUserId);

			// act and assert
			assertThrows(CoreException.class, () -> pointApplicationService.getPoint(query));
		}
	}

	@DisplayName("포인트를 충전할 때,")
	@Nested
	class ChargePoint {

		@DisplayName("정상적인 충전 요청 시, 포인트가 충전된다.")
		@Test
		void shouldChargeBalancePoint_whenValidRequest() {
			// arrange
			Point point = createTestPoint();
			pointJpaRepository.save(point);

			ChargePointCommand command = new ChargePointCommand(TEST_USER_ID, CHARGE_AMOUNT);

			// act
			PointResult result = pointApplicationService.chargePoint(command);

			// assert
			assertThat(result).isNotNull();
			assertThat(result.userId()).isEqualTo(TEST_USER_ID);
			assertThat(result.balance()).isEqualByComparingTo(INITIAL_AMOUNT.add(CHARGE_AMOUNT));
			var histories = pointHistoryJpaRepository.findAllByUserIdOrderByCreatedAtDesc(UserId.of(TEST_USER_ID));
			assertThat(histories.size()).isEqualTo(1);
			assertThat(histories.getFirst().getType()).isEqualTo(PointHistoryType.CHARGE);
			assertThat(histories.getFirst().getAmount()).isEqualByComparingTo(CHARGE_AMOUNT);
		}

		@DisplayName("존재하지 않는 유저 ID로 충전을 시도한 경우, 예외가 발생한다.")
		@Test
		void shouldThrowException_whenUserDoesNotExist() {
			// arrange
			String nonExistentUserId = "nonExistent";
			ChargePointCommand command = new ChargePointCommand(nonExistentUserId, CHARGE_AMOUNT);

			// act and assert
			assertThrows(CoreException.class, () -> pointApplicationService.chargePoint(command));
		}

		@DisplayName("여러 번 충전할 경우, 포인트가 누적된다.")
		@Test
		void shouldAccumulatePoints_whenChargedMultipleTimes() {
			// arrange
			Point point = createTestPoint();
			pointJpaRepository.save(point);

			ChargePointCommand firstCommand = new ChargePointCommand(TEST_USER_ID, CHARGE_AMOUNT);
			ChargePointCommand secondCommand = new ChargePointCommand(TEST_USER_ID, CHARGE_AMOUNT);

			// act
			pointApplicationService.chargePoint(firstCommand);
			PointResult result = pointApplicationService.chargePoint(secondCommand);

			// assert
			BigDecimal expectedAmount = INITIAL_AMOUNT.add(CHARGE_AMOUNT).add(CHARGE_AMOUNT);
			assertThat(result.balance()).isEqualByComparingTo(expectedAmount);
			var histories = pointHistoryJpaRepository.findAllByUserIdOrderByCreatedAtDesc(UserId.of(TEST_USER_ID));
			assertThat(histories.size()).isEqualTo(2);
		}
	}
}
