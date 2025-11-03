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
@Table(name = "product_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetric extends BaseEntity {

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "view_count", nullable = false)
	private Long viewCount;

	@Column(name = "like_count", nullable = false)
	private Long likeCount;

	@Column(name = "order_count", nullable = false)
	private Long orderCount;

	@Column(name = "score", nullable = false)
	private Double score;

	@Builder
	public ProductMetric(Long productId, Long viewCount, Long likeCount, Long orderCount, Double score) {
		this.productId = productId;
		this.viewCount = viewCount;
		this.likeCount = likeCount;
		this.orderCount = orderCount;
		this.score = score;
	}

	public static ProductMetric of(Long productId) {
		ProductMetric metrics = new ProductMetric();
		metrics.productId = productId;
		return metrics;
	}

	public void updateLikeCount(Long count) {
		this.likeCount = count;
	}
}
