package com.loopers.interfaces.api.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.product.GetProductDetailQuery;
import com.loopers.application.product.GetProductListQuery;
import com.loopers.application.product.ProductDetailResult;
import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductListResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

	private final ProductApplicationService productApplicationService;

	@GetMapping
	@Override
	public ApiResponse<ProductDto.V1.ProductListResponse> getProductList(ProductDto.V1.ProductRequest productRequest) {
		GetProductListQuery query = productRequest.toQuery();
		ProductListResult products = productApplicationService.getProductList(query);
		ProductDto.V1.ProductListResponse response = ProductDto.V1.ProductListResponse.from(products);
		return ApiResponse.success(response);
	}

	@GetMapping("/{productId}")
	@Override
	public ApiResponse<ProductDto.V1.ProductDetailResponse> getProductDetail(@PathVariable final Long productId) {
		GetProductDetailQuery query = GetProductDetailQuery.of(productId);
		ProductDetailResult product = productApplicationService.getProductDetail(query);
		ProductDto.V1.ProductDetailResponse response = ProductDto.V1.ProductDetailResponse.from(product);
		return ApiResponse.success(response);
	}
}
