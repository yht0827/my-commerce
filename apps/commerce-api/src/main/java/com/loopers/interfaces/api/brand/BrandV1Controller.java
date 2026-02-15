package com.loopers.interfaces.api.brand;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandResult;
import com.loopers.domain.brand.BrandId;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller {
	private final BrandFacade brandFacade;

	@GetMapping("/{brandId}")
	public ApiResponse<BrandResponse> getBrandById(@PathVariable final BrandId brandId) {
		BrandResult brandResult = brandFacade.getBrandById(brandId);
		BrandResponse brandResponse = BrandResponse.from(brandResult);
		return ApiResponse.success(brandResponse);
	}
}
