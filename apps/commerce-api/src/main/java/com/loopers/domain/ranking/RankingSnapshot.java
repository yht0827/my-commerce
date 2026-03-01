package com.loopers.domain.ranking;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Immutable
@Entity
@Table(name = "product_ranking")
public class RankingSnapshot {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "rank_type", nullable = false)
	private RankingPeriod rankType;

	@Column(name = "rank_position", nullable = false)
	private Long rankPosition;

	@Column(name = "score", nullable = false)
	private Double score;

	@Column(name = "rank_date", nullable = false)
	private LocalDate rankDate;
}
