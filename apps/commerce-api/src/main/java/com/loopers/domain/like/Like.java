package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

	@Embedded
	private UserId userId;

	@Embedded
	private ProductId productId;

	@Version
	private Long version;

	@Builder
	public Like(UserId userId, ProductId productId) {
		this.userId = userId;
		this.productId = productId;
	}

	public static Like create(final UserId userId, final ProductId productId) {
		return Like.builder()
			.userId(userId)
			.productId(productId)
			.build();
	}

}
