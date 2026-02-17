package com.loopers.application.product;

public record GetProductDetailQuery(Long productId) {
	public static GetProductDetailQuery of(final Long productId) {
		return new GetProductDetailQuery(productId);
	}
}
