package com.loopers.domain.brand;

public record BrandInfo(Long brandId, String brandName, String description, String logoUrl) {
	public static BrandInfo from(Brand brand) {
		return new BrandInfo(brand.getId(), brand.getBrandName().getBrandName(), brand.getDescription(), brand.getLogoUrl());
	}
}
