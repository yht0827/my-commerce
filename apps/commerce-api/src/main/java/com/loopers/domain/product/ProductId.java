package com.loopers.domain.product;

import java.io.Serializable;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode(of = "productId")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductId implements Serializable {

	@Column(name = "product_id")
	private Long productId;

	public ProductId(Long productId) {
		if (productId == null || productId <= 0) {
			throw new CoreException(ErrorType.BAD_REQUEST, "제품 ID는 비어있을 수 없습니다.");
		}
		this.productId = productId;
	}

	public static ProductId of(Long productId) {
		return new ProductId(productId);
	}
}
