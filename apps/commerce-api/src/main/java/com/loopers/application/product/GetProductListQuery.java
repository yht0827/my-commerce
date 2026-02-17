package com.loopers.application.product;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductCommand;

public record GetProductListQuery(
	Long brandId,
	Pageable pageable
) {
	public ProductCommand.GetProductList toCommand() {
		BrandId resolvedBrandId = brandId == null ? null : BrandId.of(brandId);
		return new ProductCommand.GetProductList(resolvedBrandId, pageable);
	}
}
