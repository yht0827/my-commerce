package com.loopers.domain.brand;

public record BrandInfo(Long brandId, BrandName brandName, String description, String logoUrl) {
	public static BrandInfo from(Brand brand) {
		return new BrandInfo(brand.getId(), brand.getBrandName(), brand.getDescription(), brand.getLogoUrl());
	}
}
