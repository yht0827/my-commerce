package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_aggregate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductAggregate extends BaseEntity {

	@Embedded
	private ProductId productId;

	@Embedded
	private LikeCount likeCount;

	@Embedded
	private OrderCount orderCount;

	@Embedded
	private ViewCount viewCount;

	@Builder
	public ProductAggregate(ProductId productId, LikeCount likeCount, OrderCount orderCount, ViewCount viewCount) {
		this.productId = productId;
		this.likeCount = likeCount != null ? likeCount : LikeCount.Zero();
		this.orderCount = orderCount != null ? orderCount : OrderCount.zero();
		this.viewCount = viewCount != null ? viewCount : ViewCount.zero();
	}

}
