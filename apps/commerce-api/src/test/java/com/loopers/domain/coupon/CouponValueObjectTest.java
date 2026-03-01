package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Coupon 값 객체 테스트")
class CouponValueObjectTest {

	@DisplayName("CouponIssuedAt")
	@Nested
	class CouponIssuedAtTest {

		@DisplayName("null이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenNull() {
			assertThatThrownBy(() -> new CouponIssuedAt(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("미래 시각이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenFuture() {
			ZonedDateTime future = ZonedDateTime.now().plusDays(1);

			assertThatThrownBy(() -> new CouponIssuedAt(future))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("과거 시각이면 정상 생성된다")
		@Test
		void createsSuccessfullyWithPastTime() {
			ZonedDateTime past = ZonedDateTime.now().minusDays(1);

			CouponIssuedAt issuedAt = new CouponIssuedAt(past);

			assertThat(issuedAt.getCouponIssuedAt()).isEqualTo(past);
		}
	}

	@DisplayName("CouponUsedAt")
	@Nested
	class CouponUsedAtTest {

		@DisplayName("null이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenNull() {
			assertThatThrownBy(() -> new CouponUsedAt(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("정상 생성된다")
		@Test
		void createsSuccessfully() {
			ZonedDateTime now = ZonedDateTime.now();

			CouponUsedAt usedAt = new CouponUsedAt(now);

			assertThat(usedAt.getCouponUsedAt()).isEqualTo(now);
		}

		@DisplayName("update 호출 후 값이 변경된다")
		@Test
		void updateChangesValue() {
			ZonedDateTime past = ZonedDateTime.now().minusDays(1);
			CouponUsedAt usedAt = new CouponUsedAt(past);

			ZonedDateTime before = usedAt.getCouponUsedAt();
			usedAt.update();

			assertThat(usedAt.getCouponUsedAt()).isAfter(before);
		}
	}

	@DisplayName("CouponExpiredAt")
	@Nested
	class CouponExpiredAtTest {

		@DisplayName("null이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenNull() {
			assertThatThrownBy(() -> new CouponExpiredAt(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("과거 시각이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenPast() {
			ZonedDateTime past = ZonedDateTime.now().minusDays(1);

			assertThatThrownBy(() -> new CouponExpiredAt(past))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("미래 시각이면 정상 생성된다")
		@Test
		void createsSuccessfullyWithFutureTime() {
			ZonedDateTime future = ZonedDateTime.now().plusDays(30);

			CouponExpiredAt expiredAt = new CouponExpiredAt(future);

			assertThat(expiredAt.getCouponExpiredAt()).isEqualTo(future);
		}
	}

	@DisplayName("CouponName")
	@Nested
	class CouponNameTest {

		@DisplayName("null이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenNull() {
			assertThatThrownBy(() -> new CouponName(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("빈 문자열이면 예외가 발생한다")
		@Test
		void throwsExceptionWhenBlank() {
			assertThatThrownBy(() -> new CouponName("  "))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("정상적인 이름이면 생성된다")
		@Test
		void createsSuccessfully() {
			CouponName couponName = new CouponName("신규가입 할인 쿠폰");

			assertThat(couponName.getCouponName()).isEqualTo("신규가입 할인 쿠폰");
		}
	}
}
