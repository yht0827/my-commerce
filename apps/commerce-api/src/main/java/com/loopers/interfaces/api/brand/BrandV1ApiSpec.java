package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Brands V1 API", description = "Brands API 입니다.")
public interface BrandV1ApiSpec {

	@Operation(
		summary = "브랜드 상세 조회",
		description = "브랜드 상세 정보를 조회합니다."
	)
	ApiResponse<BrandDto.V1.BrandResponse> getBrandById(final Long brandId);
}
