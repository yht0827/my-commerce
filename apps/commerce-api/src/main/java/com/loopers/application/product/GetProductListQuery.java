package com.loopers.application.product;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.product.ProductCommand;

public record GetProductListQuery(
	Long brandId,
	Pageable pageable
) {
	public ProductCommand.GetProductList toCommand() {
		return new ProductCommand.GetProductList(brandId, pageable);
	}
}
