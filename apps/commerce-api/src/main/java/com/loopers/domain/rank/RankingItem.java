package com.loopers.domain.rank;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingItem {

	private Long rank;
	private Long productId;
	private Double score;
	private Long likeCount;
	private Long viewCount;
	private Long orderCount;

	@Builder(builderMethodName = "create")
	public RankingItem(
		Long rank,
		Long productId,
		Double score,
		Long likeCount,
		Long viewCount,
		Long orderCount
	) {
		this.rank = rank;
		this.productId = productId;
		this.score = score;
		this.likeCount = likeCount;
		this.viewCount = viewCount;
		this.orderCount = orderCount;
	}
}
