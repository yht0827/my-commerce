package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.rank.RankingQueryService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProductApplicationService {

	private final ProductService productService;
	private final RankingQueryService rankingQueryService;

	public ProductListResult getProductList(final GetProductListQuery query) {
		ProductData.GetProductList data = query.toData();
		Page<ProductInfo> products = productService.getProductList(data);
		return ProductListResult.from(products);
	}

	public ProductDetailResult getProductDetail(final GetProductDetailQuery query) {
		ProductId productId = ProductId.of(query.productId());
		ProductInfo productInfo = productService.getProductDetail(productId);

		Long rank = rankingQueryService.getProductRanking(productId.getProductId());

		return ProductDetailResult.from(productInfo, rank);
	}

}
