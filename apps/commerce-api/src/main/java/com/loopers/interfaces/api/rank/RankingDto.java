package com.loopers.interfaces.api.rank;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;

import com.loopers.application.ranking.GetRankingQuery;
import com.loopers.application.ranking.RankingPageResult;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RankingDto() {

	public record V1() {

		public record RankingRequest(
			@DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
			String period,
			@PositiveOrZero Integer page,
			@Positive Integer size
		) {
			public GetRankingQuery from(final String userId) {
				return new GetRankingQuery(
					userId,
					this.date,
					this.period,
					PageRequest.of(
						page != null ? page : 0,
						size != null ? size : 10
					));
			}
		}

		public record RankingPageResponse(
			int page, int size, long totalElements,
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
				Long rank, Double score, Long productId,
				String productName, Long price, Long quantity,
				String brandName, Long likeCount
			) {
			}
		}
	}
}
