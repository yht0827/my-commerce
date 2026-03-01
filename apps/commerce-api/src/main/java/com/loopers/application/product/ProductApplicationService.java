package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductQueryService;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.domain.ranking.RankingQueryService;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductApplicationService {

	private final ProductQueryService productQueryService;
	private final RankingQueryService rankingQueryService;
	private final EventPublisher eventPublisher;

	public ProductListResult getProductList(final GetProductListQuery query) {
		ProductData.GetProductList data = query.toData();
		Page<ProductInfo> products = productQueryService.getProductList(data);
		return ProductListResult.from(products);
	}

	public ProductDetailResult getProductDetail(final GetProductDetailQuery query) {
		ProductId productId = ProductId.of(query.productId());
		ProductInfo productInfo = productQueryService.getProductDetail(productId);

		Long rank = rankingQueryService.getProductRanking(productId.getProductId());

		try {
			eventPublisher.publish(ProductViewedEvent.create(productId.getProductId()));
		} catch (Exception e) {
			log.warn("상품 조회 이벤트 발행 실패. productId={}", productId.getProductId(), e);
		}

		return ProductDetailResult.from(productInfo, rank);
	}

}
