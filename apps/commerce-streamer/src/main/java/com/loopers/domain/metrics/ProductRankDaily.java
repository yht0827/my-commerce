package com.loopers.domain.metrics;

import java.time.LocalDate;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_rank_daily")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRankDaily extends BaseEntity {

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;

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

	public static ProductRankDaily create(
		Long productId,
		LocalDate snapshotDate,
		Double score,
		Long ranking,
		Long likeCount,
		Long viewCount,
		Long orderCount
	) {
		ProductRankDaily metrics = new ProductRankDaily();
		metrics.productId = productId;
		metrics.snapshotDate = snapshotDate;
		metrics.score = score;
		metrics.ranking = ranking;
		metrics.likeCount = likeCount;
		metrics.viewCount = viewCount;
		metrics.orderCount = orderCount;
		return metrics;
	}

}
