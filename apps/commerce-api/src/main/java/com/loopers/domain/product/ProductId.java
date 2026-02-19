package com.loopers.domain.product;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;

import com.loopers.support.error.CoreException;

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
			throw new CoreException(BAD_REQUEST, PRODUCT_ID_INVALID.format());
		}
		this.productId = productId;
	}

	public static ProductId of(Long productId) {
		return new ProductId(productId);
	}
}
