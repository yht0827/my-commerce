package com.loopers.interfaces.api.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.product.ProductCriteria;
import com.loopers.application.product.ProductDetailResult;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductListResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller {

	private final ProductFacade productFacade;

	@GetMapping
	public ApiResponse<ProductDto.V1.ProductListResponse> getProductList(ProductDto.V1.ProductRequest productRequest) {
		ProductCriteria.GetProductList criteria = productRequest.toCriteria();
		ProductListResult products = productFacade.getProductList(criteria);
		ProductDto.V1.ProductListResponse response = ProductDto.V1.ProductListResponse.from(products);
		return ApiResponse.success(response);
	}

	@GetMapping("/{productId}")
	public ApiResponse<ProductDto.V1.ProductDetailResponse> getProductDetail(@PathVariable final Long productId) {
		ProductDetailResult product = productFacade.getProductDetail(productId);
		ProductDto.V1.ProductDetailResponse response = ProductDto.V1.ProductDetailResponse.from(product);
		return ApiResponse.success(response);
	}
}
