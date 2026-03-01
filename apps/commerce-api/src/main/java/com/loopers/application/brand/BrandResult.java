package com.loopers.application.brand;

import com.loopers.domain.brand.BrandInfo;

public record BrandResult(Long brandId, String brandName, String description, String logoUrl) {

	public static BrandResult from(BrandInfo brand) {
		return new BrandResult(brand.brandId(), brand.brandName(), brand.description(), brand.logoUrl());
	}
}
