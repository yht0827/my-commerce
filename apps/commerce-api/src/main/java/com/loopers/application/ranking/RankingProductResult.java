package com.loopers.application.ranking;

import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.ranking.RankingInfo;

public record RankingProductResult(
	Long rank, Double score,
	Long productId, String productName, Long price, Long quantity,
	String brandName, Long likeCount
) {
	public static RankingProductResult from(final RankingInfo.Item item, final ProductInfo info) {
		return new RankingProductResult(
			item.rank(), item.score(), item.productId(),
			info.productName(), info.price(), info.quantity(),
			info.brandName(), info.likeCount()
		);
	}
}
