package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class PointTest {

	@DisplayName("포인트를 생성할 때, ")
	@Nested
	class Create {

		@DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
		@Test
		void failWhenChargingWithZeroOrNegativeAmount() {
			// arrange
			String userId = "yht0827";
			BigDecimal balance = BigDecimal.valueOf(-1L);

			// act
			CoreException result = assertThrows(CoreException.class, () -> new Point(new UserId(userId), new Balance(balance)));

			// assert
			assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}

		@DisplayName("양수 잔액으로 포인트를 정상 생성한다.")
		@Test
		void createSuccessfullyWithPositiveBalance() {
			// arrange
			String userId = "yht0827";
			BigDecimal balance = BigDecimal.valueOf(1000L);

			// act
			Point point = new Point(new UserId(userId), new Balance(balance));

			// assert
			assertThat(point.getUserId().getUserId()).isEqualTo(userId);
			assertThat(point.getBalance().getBalance()).isEqualByComparingTo(balance);
		}
	}

	@DisplayName("chargeBalance 메서드")
	@Nested
	class ChargeBalance {

		@DisplayName("정상 충전 후 잔액이 증가한다.")
		@Test
		void chargeIncreasesBalance() {
			// arrange
			Point point = new Point(new UserId("yht0827"), new Balance(BigDecimal.valueOf(500)));
			Balance chargeAmount = new Balance(BigDecimal.valueOf(300));

			// act
			point.chargeBalance(chargeAmount);

			// assert
			assertThat(point.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
		}
	}

	@DisplayName("useBalance 메서드")
	@Nested
	class UseBalance {

		@DisplayName("정상 차감 후 잔액이 감소한다.")
		@Test
		void useDecreasesBalance() {
			// arrange
			Point point = new Point(new UserId("yht0827"), new Balance(BigDecimal.valueOf(1000)));
			Balance useAmount = new Balance(BigDecimal.valueOf(400));

			// act
			point.useBalance(useAmount);

			// assert
			assertThat(point.getBalance().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
		}
	}
}
