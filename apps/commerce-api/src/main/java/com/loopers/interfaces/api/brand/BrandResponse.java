package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandResult;
import com.loopers.domain.brand.BrandName;

public record BrandResponse(
	Long brandId,
	BrandName brandName,
	String description,
	String logoUrl
) {
	public static BrandResponse from(BrandResult brandResult) {
		return new BrandResponse(
			brandResult.brandId(),
			brandResult.brandName(),
			brandResult.description(),
			brandResult.logoUrl()
		);
	}
}
