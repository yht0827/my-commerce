package com.loopers.domain.rank;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Immutable
@Entity
@Table(name = "product_metrics_weekly")
public class RankingWeeklySnapshot {

	@Id
	@Column(name = "id")
	private Long id;

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
}
