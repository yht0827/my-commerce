package com.loopers.domain.rank;

import org.hibernate.annotations.Immutable;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Immutable
@Entity
@Table(name = "product_metrics_monthly")
public class RankingMonthlySnapshot extends BaseEntity {

	private String periodKey;

	private Long productId;

	private Double score;

	private Long rank;

	private Long likeCount;

	private Long viewCount;

	private Long orderCount;
}
