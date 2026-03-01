package com.loopers.interfaces.api.ranking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;

import com.loopers.application.ranking.RankingQuery;
import com.loopers.application.ranking.RankingPageResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RankingDto() {

	public record V1() {

		public record RankingRequest(
			@Schema(description = "조회 기준 일자(yyyyMMdd). 비어있으면 오늘 기준", example = "20260219")
			@DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
			@Schema(description = "랭킹 타입(DAILY, WEEKLY, MONTHLY). 비어있으면 DAILY", example = "DAILY")
			String type,
			@Schema(description = "이전 호환용 파라미터(period). 미입력 권장", example = "daily")
			String period,
			@Schema(description = "페이지 번호(0부터 시작)", example = "0")
			@PositiveOrZero Integer page,
			@Schema(description = "페이지 크기", example = "10")
			@Positive Integer size
		) {
			public RankingQuery.GetRanking toQuery(final String userId) {
				String rankingType = type != null && !type.isBlank() ? type : period;
				return new RankingQuery.GetRanking(
					userId,
					this.date,
					rankingType,
					PageRequest.of(
						page != null ? page : 0,
						size != null ? size : 10
					));
			}
		}

		public record RankingPageResponse(
			@Schema(description = "현재 페이지 번호", example = "0")
			int page, int size, long totalElements,
			@Schema(description = "전체 페이지 수", example = "5")
			int totalPages, List<Item> items
		) {
			public static RankingDto.V1.RankingPageResponse from(final RankingPageResult result) {
				List<Item> items = result.items().stream()
					.map(r -> new Item(
						r.rank(), r.score(),
						r.productId(), r.productName(), r.price(), r.quantity(),
						r.brandName(), r.likeCount()
					))
					.toList();

				return new RankingDto.V1.RankingPageResponse(
					result.page(), result.size(), result.totalElements(), result.totalPages(), items
				);
			}

			public record Item(
				@Schema(description = "순위", example = "1")
				Long rank, Double score, Long productId,
				String productName, Long price, Long quantity,
				String brandName, Long likeCount
			) {
			}
		}
	}
}
