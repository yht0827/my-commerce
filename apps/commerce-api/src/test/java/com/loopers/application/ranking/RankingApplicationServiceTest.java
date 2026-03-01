package com.loopers.application.ranking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductQueryService;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingQueryService;
import com.loopers.domain.ranking.RankingSource;

@DisplayName("RankingApplicationService 테스트")
@ExtendWith(MockitoExtension.class)
class RankingApplicationServiceTest {

	@Mock
	private RankingQueryService rankingQueryService;

	@Mock
	private ProductQueryService productQueryService;

	@InjectMocks
	private RankingApplicationService rankingApplicationService;

	@Test
	@DisplayName("랭킹 아이템이 없으면 상품 조회 없이 빈 결과를 반환한다")
	void getRanking_returnsEmptyWhenRankingItemsEmpty() {
		RankingQuery.GetRanking query = query();
		RankingInfo.RankingPage rankingPage = new RankingInfo.RankingPage(0, 10, 0L, 0, RankingSource.SNAPSHOT, List.of());
		when(rankingQueryService.getRanking(query.toData())).thenReturn(rankingPage);

		RankingPageResult result = rankingApplicationService.getRanking(query);

		assertThat(result.items()).isEmpty();
		assertThat(result.totalElements()).isZero();
		verifyNoInteractions(productQueryService);
	}

	@Test
	@DisplayName("상품 정보가 없는 랭킹 아이템은 필터링하고 매핑 가능한 아이템만 반환한다")
	void getRanking_mapsItemsAndSkipsUnmatchedProducts() {
		RankingQuery.GetRanking query = query();
		RankingInfo.RankingPage rankingPage = new RankingInfo.RankingPage(
			0,
			10,
			2L,
			1,
			RankingSource.REALTIME,
			List.of(
				new RankingInfo.Item(1L, 1L, 10.5, 3L, 5L, 1L),
				new RankingInfo.Item(2L, 2L, 8.0, 1L, 2L, 1L)
			)
		);

		ProductInfo productInfo = new ProductInfo(1L, "product-1", 1000L, 10L, "brand-1", 7L, 0L, 0L);
		when(rankingQueryService.getRanking(query.toData())).thenReturn(rankingPage);
		when(productQueryService.getProductByIds(anyList())).thenReturn(Map.of(ProductId.of(1L), productInfo));

		RankingPageResult result = rankingApplicationService.getRanking(query);

		verify(productQueryService).getProductByIds(argThat(ids -> ids.equals(List.of(ProductId.of(1L), ProductId.of(2L)))));
		assertThat(result.items()).hasSize(1);

		RankingProductResult first = result.items().getFirst();
		assertThat(first.rank()).isEqualTo(1L);
		assertThat(first.productId()).isEqualTo(1L);
		assertThat(first.productName()).isEqualTo("product-1");
		assertThat(first.brandName()).isEqualTo("brand-1");
		assertThat(first.likeCount()).isEqualTo(7L);
	}

	private RankingQuery.GetRanking query() {
		Pageable pageable = PageRequest.of(0, 10);
		return new RankingQuery.GetRanking("u1", LocalDate.of(2026, 3, 1), "DAILY", pageable);
	}
}
