package com.loopers.interfaces.api.brand;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandResult;
import com.loopers.application.brand.GetBrandQuery;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {
	private final BrandApplicationService brandApplicationService;

	@GetMapping("/{brandId}")
	@Override
	public ApiResponse<BrandDto.V1.BrandResponse> getBrandById(@PathVariable final Long brandId) {
		GetBrandQuery query = GetBrandQuery.of(brandId);
		BrandResult brandResult = brandApplicationService.getBrandById(query);
		BrandDto.V1.BrandResponse brandResponse = BrandDto.V1.BrandResponse.from(brandResult);
		return ApiResponse.success(brandResponse);
	}
}
