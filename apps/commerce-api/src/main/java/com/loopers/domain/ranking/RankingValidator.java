package com.loopers.domain.ranking;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankingValidator {

	public LocalDate validateDate(final LocalDate date) {
		if (date == null) {
			return LocalDate.now();
		}

		if (date.isAfter(LocalDate.now())) {
			throw new CoreException(BAD_REQUEST, RANKING_DATE_CANNOT_BE_FUTURE.format());
		}

		return date;
	}

	public RankingPeriod validatePeriod(final String period) {
		try {
			return RankingPeriod.from(period);
		} catch (IllegalArgumentException e) {
			throw new CoreException(BAD_REQUEST, RANKING_PERIOD_INVALID.format());
		}
	}
}
