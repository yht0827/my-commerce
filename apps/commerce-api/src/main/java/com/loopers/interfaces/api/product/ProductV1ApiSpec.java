package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Products V1 API", description = "Products API 입니다.")
public interface ProductV1ApiSpec {

	@Operation(
		summary = "상품 목록 조회",
		description = "조건에 맞는 상품 목록을 조회합니다."
	)
	ApiResponse<ProductDto.V1.ProductListResponse> getProductList(final ProductDto.V1.ProductRequest productRequest);

	@Operation(
		summary = "상품 상세 조회",
		description = "상품 상세 정보를 조회합니다."
	)
	ApiResponse<ProductDto.V1.ProductDetailResponse> getProductDetail(final Long productId);
}
