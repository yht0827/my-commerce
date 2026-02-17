package com.loopers.domain.product;

public enum ProductStatus {
	ON_SALE,
	SOLD_OUT,
	DISCONTINUED;

	public boolean isOrderable() {
		return this == ON_SALE;
	}
}
