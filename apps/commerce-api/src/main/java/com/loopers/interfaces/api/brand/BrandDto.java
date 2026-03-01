package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record BrandDto() {

	public record V1() {
		public record BrandDetailResponse(
			@Schema(description = "브랜드 ID", example = "1")
			Long brandId,
			@Schema(description = "브랜드명", example = "나이키")
			String brandName,
			@Schema(description = "브랜드 설명", example = "글로벌 스포츠 브랜드")
			String description,
			@Schema(description = "브랜드 로고 URL", example = "https://cdn.example.com/logo/nike.png")
			String logoUrl
		) {
			public static BrandDetailResponse from(final BrandResult brandResult) {
				return new BrandDetailResponse(
					brandResult.brandId(),
					brandResult.brandName(),
					brandResult.description(),
					brandResult.logoUrl()
				);
			}
		}
	}
}
