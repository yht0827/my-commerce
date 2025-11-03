package com.loopers.domain.rank;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Immutable
@Entity
@Table(name = "product_rank_daily")
public class RankingDailySnapshot {

	@Id
	@Column(name = "id")
	private Long id;

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
}
