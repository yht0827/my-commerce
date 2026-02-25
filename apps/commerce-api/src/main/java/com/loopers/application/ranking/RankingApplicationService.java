package com.loopers.application.ranking;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductQueryService;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingQueryService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RankingApplicationService {

	private final RankingQueryService rankingQueryService;
	private final ProductQueryService productQueryService;

	@Transactional(readOnly = true)
	public RankingPageResult getRanking(final RankingQuery.GetRanking query) {
		RankingInfo.RankingPage pageData = rankingQueryService.getRanking(query.toData());

		if (pageData.items().isEmpty()) {
			return RankingPageResult.from(pageData, List.of());
		}

		List<ProductId> ids = pageData.items().stream()
			.map(RankingInfo.Item::productId)
			.filter(Objects::nonNull)
			.map(ProductId::of)
			.toList();

		Map<ProductId, ProductInfo> productMap = productQueryService.getProductByIds(ids);

		List<RankingProductResult> items = pageData.items().stream()
			.map(item -> {
				ProductInfo info = productMap.get(ProductId.of(item.productId()));
				return info != null ? RankingProductResult.from(item, info) : null;
			})
			.filter(Objects::nonNull)
			.toList();

		return RankingPageResult.from(pageData, items);
	}

}
