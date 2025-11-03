package com.loopers.application.ranking;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.rank.RankingItem;
import com.loopers.domain.rank.RankingPageData;
import com.loopers.domain.rank.RankingPeriod;
import com.loopers.domain.rank.RankingQueryService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RankingService implements RankingUseCase {

	private final RankingQueryService rankingQueryService;
	private final ProductService productService;

	@Transactional(readOnly = true)
	@Override
	public RankingPageResult getRanking(final GetRankingQuery query) {
		RankingPeriod period = RankingPeriod.from(query.period());
		return switch (period) {
			case DAILY -> getDaily(query);
			case WEEKLY -> getWeekly(query);
			case MONTHLY -> getMonthly(query);
		};
	}

	public RankingPageResult getDaily(final GetRankingQuery query) {
		RankingPageData pageData = rankingQueryService.getDaily(query.date(), query.pageable());
		Page<RankingItem> page = pageData == null || pageData.page() == null
			? Page.empty(query.pageable())
			: pageData.page();

		return buildRankingPageResult(page);
	}

	public RankingPageResult getWeekly(GetRankingQuery query) {
		RankingPageData pageData = rankingQueryService.getWeekly(query.date(), query.pageable());
		Page<RankingItem> page = pageData == null || pageData.page() == null
			? Page.empty(query.pageable())
			: pageData.page();

		return buildRankingPageResult(page);
	}

	public RankingPageResult getMonthly(GetRankingQuery query) {
		RankingPageData pageData = rankingQueryService.getMonthly(query.date(), query.pageable());
		Page<RankingItem> page = pageData == null || pageData.page() == null
			? Page.empty(query.pageable())
			: pageData.page();

		return buildRankingPageResult(page);
	}

	public RankingPageResult buildRankingPageResult(Page<RankingItem> page) {
		if (page == null || page.isEmpty()) {
			Page<RankingItem> emptyPage = page == null ? Page.empty() : page;
			return RankingPageResult.from(emptyPage, List.of());
		}

		List<Long> ids = page.getContent().stream()
			.map(RankingItem::getProductId)
			.toList();

		Map<Long, ProductInfo> productMap = productService.getProductByIds(ids);

		List<RankingProductResult> items = page.getContent().stream()
			.map(item -> {
				ProductInfo info = productMap.get(item.getProductId());
				return info != null ? RankingProductResult.from(item, info) : null;
			})
			.filter(Objects::nonNull)
			.toList();

		return RankingPageResult.from(page, items);
	}

}
