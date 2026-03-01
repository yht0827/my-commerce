package com.loopers.interfaces.api.ranking;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.application.ranking.RankingQuery;

@DisplayName("RankingDto 테스트")
class RankingDtoTest {

	@Test
	@DisplayName("type이 있으면 period보다 type을 우선 사용한다")
	void rankingRequest_toQuery_prefersTypeOverPeriod() {
		RankingDto.V1.RankingRequest request = new RankingDto.V1.RankingRequest(
			LocalDate.of(2026, 3, 1),
			"WEEKLY",
			"DAILY",
			1,
			20
		);

		RankingQuery.GetRanking query = request.toQuery("user1");

		assertThat(query.userId()).isEqualTo("user1");
		assertThat(query.period()).isEqualTo("WEEKLY");
		assertThat(query.pageable().getPageNumber()).isEqualTo(1);
		assertThat(query.pageable().getPageSize()).isEqualTo(20);
	}

	@Test
	@DisplayName("type이 blank면 period를 사용한다")
	void rankingRequest_toQuery_usesPeriodWhenTypeBlank() {
		RankingDto.V1.RankingRequest request = new RankingDto.V1.RankingRequest(
			LocalDate.of(2026, 3, 1),
			"   ",
			"MONTHLY",
			0,
			10
		);

		RankingQuery.GetRanking query = request.toQuery("user1");

		assertThat(query.period()).isEqualTo("MONTHLY");
	}

	@Test
	@DisplayName("type이 null이면 period를 사용하고 page/size 기본값을 적용한다")
	void rankingRequest_toQuery_usesPeriodAndDefaultsWhenTypeAndPagingNull() {
		RankingDto.V1.RankingRequest request = new RankingDto.V1.RankingRequest(
			LocalDate.of(2026, 3, 1),
			null,
			"DAILY",
			null,
			null
		);

		RankingQuery.GetRanking query = request.toQuery("user2");

		assertThat(query.period()).isEqualTo("DAILY");
		assertThat(query.pageable().getPageNumber()).isEqualTo(0);
		assertThat(query.pageable().getPageSize()).isEqualTo(10);
	}

	@Test
	@DisplayName("type/period 모두 null이면 period도 null로 전달한다")
	void rankingRequest_toQuery_keepsNullPeriodWhenBothTypeAndPeriodNull() {
		RankingDto.V1.RankingRequest request = new RankingDto.V1.RankingRequest(
			LocalDate.of(2026, 3, 1),
			null,
			null,
			2,
			5
		);

		RankingQuery.GetRanking query = request.toQuery("user3");

		assertThat(query.period()).isNull();
		assertThat(query.pageable().getPageNumber()).isEqualTo(2);
		assertThat(query.pageable().getPageSize()).isEqualTo(5);
	}
}
