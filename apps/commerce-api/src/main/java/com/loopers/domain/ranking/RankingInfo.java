package com.loopers.domain.ranking;

import java.util.List;

import org.springframework.data.domain.Page;

public record RankingInfo() {

	public record Item(
		Long rank,
		Long productId,
		Double score,
		Long likeCount,
		Long viewCount,
		Long orderCount
	) {
		public static Item from(final RankingItem item) {
			return new Item(
				item.getRank(),
				item.getProductId(),
				item.getScore(),
				item.getLikeCount(),
				item.getViewCount(),
				item.getOrderCount()
			);
		}
	}

	public record RankingPage(
		int page,
		int size,
		long totalElements,
		int totalPages,
		RankingSource source,
		List<Item> items
	) {
		public static RankingPage from(final Page<RankingItem> pageData, final RankingSource source) {
			List<Item> items = pageData.getContent().stream()
				.map(Item::from)
				.toList();

			return new RankingPage(
				pageData.getNumber(),
				pageData.getSize(),
				pageData.getTotalElements(),
				pageData.getTotalPages(),
				source,
				items
			);
		}
	}
}
