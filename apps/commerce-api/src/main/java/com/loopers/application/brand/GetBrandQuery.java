package com.loopers.application.brand;

public record GetBrandQuery(Long brandId) {
	public static GetBrandQuery of(final Long brandId) {
		return new GetBrandQuery(brandId);
	}
}
