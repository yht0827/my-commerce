package com.loopers.domain.ranking;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("RankingValidator 단위 테스트")
class RankingValidatorTest {

	private final RankingValidator rankingValidator = new RankingValidator();

	@DisplayName("validateDate 시")
	@Nested
	class ValidateDate {

		@DisplayName("null이면 오늘 날짜를 반환한다")
		@Test
		void validateDate_returnsToday_whenNull() {
			// act
			LocalDate result = rankingValidator.validateDate(null);

			// assert
			assertThat(result).isEqualTo(LocalDate.now());
		}

		@DisplayName("과거 날짜이면 그대로 반환한다")
		@Test
		void validateDate_returnsPastDate_whenPastDateGiven() {
			// arrange
			LocalDate pastDate = LocalDate.now().minusDays(1);

			// act
			LocalDate result = rankingValidator.validateDate(pastDate);

			// assert
			assertThat(result).isEqualTo(pastDate);
		}

		@DisplayName("미래 날짜이면 CoreException(BAD_REQUEST)이 발생한다")
		@Test
		void validateDate_throwsCoreException_whenFutureDateGiven() {
			// arrange
			LocalDate futureDate = LocalDate.now().plusDays(1);

			// act & assert
			CoreException exception = assertThrows(
				CoreException.class,
				() -> rankingValidator.validateDate(futureDate)
			);

			assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}
	}

	@DisplayName("validatePeriod 시")
	@Nested
	class ValidatePeriod {

		@DisplayName("null이면 DAILY를 반환한다")
		@Test
		void validatePeriod_returnsDaily_whenNull() {
			// act
			RankingPeriod result = rankingValidator.validatePeriod(null);

			// assert
			assertThat(result).isEqualTo(RankingPeriod.DAILY);
		}

		@DisplayName("\"DAILY\"이면 DAILY를 반환한다")
		@Test
		void validatePeriod_returnsDaily_whenDailyGiven() {
			// act
			RankingPeriod result = rankingValidator.validatePeriod("DAILY");

			// assert
			assertThat(result).isEqualTo(RankingPeriod.DAILY);
		}

		@DisplayName("\"weekly\" (소문자)이면 WEEKLY를 반환한다")
		@Test
		void validatePeriod_returnsWeekly_whenLowercaseWeeklyGiven() {
			// act
			RankingPeriod result = rankingValidator.validatePeriod("weekly");

			// assert
			assertThat(result).isEqualTo(RankingPeriod.WEEKLY);
		}

		@DisplayName("\"invalid\"이면 CoreException(BAD_REQUEST)이 발생한다")
		@Test
		void validatePeriod_throwsCoreException_whenInvalidPeriodGiven() {
			// act & assert
			CoreException exception = assertThrows(
				CoreException.class,
				() -> rankingValidator.validatePeriod("invalid")
			);

			assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}
	}
}
