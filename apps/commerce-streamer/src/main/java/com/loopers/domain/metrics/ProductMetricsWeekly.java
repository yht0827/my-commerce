package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_metrics_weekly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsWeekly extends BaseEntity {

	@Column(name = "period_key", nullable = false, length = 8)
	private String periodKey;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "score", nullable = false)
	private Double score;

	@Column(name = "ranking", nullable = false)
	private Long ranking;

	@Column(name = "like_count", nullable = false)
	private Long likeCount;

	@Column(name = "view_count", nullable = false)
	private Long viewCount;

	@Column(name = "order_count", nullable = false)
	private Long orderCount;

	@Builder
	public ProductMetricsWeekly(
		String periodKey,
		Long productId,
		Double score,
		Long ranking,
		Long likeCount,
		Long viewCount,
		Long orderCount
	) {
		this.periodKey = periodKey;
		this.productId = productId;
		this.score = score;
		this.ranking = ranking;
		this.likeCount = likeCount;
		this.viewCount = viewCount;
		this.orderCount = orderCount;
	}
}
