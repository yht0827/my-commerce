package com.loopers.application.product;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductData;

public record GetProductListQuery(
	Long brandId,
	Pageable pageable
) {
	public ProductData.GetProductList toData() {
		BrandId resolvedBrandId = brandId == null ? null : BrandId.of(brandId);
		return new ProductData.GetProductList(resolvedBrandId, pageable);
	}
}
