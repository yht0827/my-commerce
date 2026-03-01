package com.loopers.domain.common;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Price 값 객체 테스트")
class PriceTest {

	@Test
	@DisplayName("null이면 BAD_REQUEST 예외가 발생한다")
	void constructor_throwsWhenNull() {
		assertThatThrownBy(() -> new Price(null))
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType actual = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(actual).isEqualTo(ErrorType.BAD_REQUEST);
			});
	}

	@Test
	@DisplayName("음수면 BAD_REQUEST 예외가 발생한다")
	void constructor_throwsWhenNegative() {
		assertThatThrownBy(() -> new Price(-1L))
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType actual = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(actual).isEqualTo(ErrorType.BAD_REQUEST);
			});
	}

	@Test
	@DisplayName("0 이상이면 정상 생성된다")
	void constructor_createsWhenZeroOrPositive() {
		Price zero = new Price(0L);
		Price positive = new Price(100L);

		assertThat(ReflectionTestUtils.getField(zero, "price")).isEqualTo(0L);
		assertThat(ReflectionTestUtils.getField(positive, "price")).isEqualTo(100L);
	}
}
