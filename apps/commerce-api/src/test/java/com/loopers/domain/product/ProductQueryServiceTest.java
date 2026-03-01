package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.domain.brand.BrandId;
import com.loopers.support.cache.CacheablePage;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("ProductQueryService 테스트")
@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductCacheRepository productCacheRepository;

	@Mock
	private ProductCacheMetrics productCacheMetrics;

	@InjectMocks
	private ProductQueryService productQueryService;

	@Test
	@DisplayName("페이지 번호가 캐시 범위를 벗어나면 DB만 조회한다")
	void getProductList_usesDbOnlyWhenPageNotCacheable() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(3, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(brandId, pageable);
		Page<ProductInfo> dbPage = new PageImpl<>(List.of(productInfo(1L)), pageable, 1);

		when(productRepository.getProductList(brandId, pageable)).thenReturn(dbPage);

		Page<ProductInfo> result = productQueryService.getProductList(query);

		assertThat(result).isEqualTo(dbPage);
		verifyNoInteractions(productCacheRepository, productCacheMetrics);
	}

	@Test
	@DisplayName("리스트 캐시 히트면 캐시 데이터를 그대로 반환한다")
	void getProductList_returnsCachedPageWhenHit() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(brandId, pageable);
		Page<ProductInfo> cachedPage = new PageImpl<>(List.of(productInfo(1L)), pageable, 1);
		CacheablePage<ProductInfo> cacheablePage = CacheablePage.from(cachedPage);

		when(productCacheRepository.findProductList(brandId, pageable)).thenReturn(Optional.of(cacheablePage));

		Page<ProductInfo> result = productQueryService.getProductList(query);

		assertThat(result.getContent()).containsExactlyElementsOf(cachedPage.getContent());
		verify(productCacheMetrics).recordCacheHit();
		verify(productRepository, never()).getProductList(any(), any());
	}

	@Test
	@DisplayName("리스트 캐시 미스면 DB 조회 후 캐시에 저장한다")
	void getProductList_loadsFromDbAndCachesWhenMiss() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(brandId, pageable);
		Page<ProductInfo> dbPage = new PageImpl<>(List.of(productInfo(1L), productInfo(2L)), pageable, 2);

		when(productCacheRepository.findProductList(brandId, pageable)).thenReturn(Optional.empty());
		when(productRepository.getProductList(brandId, pageable)).thenReturn(dbPage);

		Page<ProductInfo> result = productQueryService.getProductList(query);

		assertThat(result.getContent()).containsExactlyElementsOf(dbPage.getContent());
		verify(productCacheMetrics).recordCacheMiss();
		verify(productCacheMetrics).recordDbLoad();
		verify(productCacheRepository).saveProductList(eq(brandId), eq(pageable), any(CacheablePage.class));
	}

	@Test
	@DisplayName("리스트 로딩 중 예외가 발생해도 다음 요청은 정상적으로 재시도된다")
	void getProductList_retriesNormallyAfterPreviousLoadFailure() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(brandId, pageable);
		Page<ProductInfo> dbPage = new PageImpl<>(List.of(productInfo(10L)), pageable, 1);
		AtomicInteger callCount = new AtomicInteger();

		when(productCacheRepository.findProductList(brandId, pageable)).thenReturn(Optional.empty());
		when(productRepository.getProductList(brandId, pageable)).thenAnswer(invocation -> {
			if (callCount.incrementAndGet() == 1) {
				throw new RuntimeException("db-failure");
			}
			return dbPage;
		});

		assertThatThrownBy(() -> productQueryService.getProductList(query))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("db-failure");

		Page<ProductInfo> second = productQueryService.getProductList(query);

		assertThat(second.getContent()).containsExactlyElementsOf(dbPage.getContent());
		verify(productRepository, times(2)).getProductList(brandId, pageable);
		verify(productCacheRepository, times(1)).saveProductList(eq(brandId), eq(pageable), any(CacheablePage.class));
	}

	@Test
	@DisplayName("브랜드 ID가 null이어도 리스트 키를 생성해 캐시 로딩을 수행한다")
	void getProductList_buildsListLoadKeyWithNullBrandId() {
		Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(null, pageable);
		Page<ProductInfo> dbPage = new PageImpl<>(List.of(productInfo(99L)), pageable, 1);

		when(productCacheRepository.findProductList(null, pageable)).thenReturn(Optional.empty());
		when(productRepository.getProductList(null, pageable)).thenReturn(dbPage);

		Page<ProductInfo> result = productQueryService.getProductList(query);

		assertThat(result.getContent()).containsExactlyElementsOf(dbPage.getContent());
		verify(productCacheRepository).saveProductList(eq(null), eq(pageable), any(CacheablePage.class));
	}

	@Test
	@DisplayName("상세 캐시 히트면 캐시 데이터를 반환한다")
	void getProductDetail_returnsCachedWhenHit() {
		ProductId productId = ProductId.of(1L);
		ProductInfo cached = productInfo(1L);

		when(productCacheRepository.findProductDetail(productId)).thenReturn(Optional.of(cached));

		ProductInfo result = productQueryService.getProductDetail(productId);

		assertThat(result).isEqualTo(cached);
		verify(productCacheMetrics).recordCacheHit();
		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("상세 캐시 미스면 DB 조회 후 캐시에 저장한다")
	void getProductDetail_loadsFromDbAndCachesWhenMiss() {
		ProductId productId = ProductId.of(2L);
		ProductInfo dbInfo = productInfo(2L);

		when(productCacheRepository.findProductDetail(productId)).thenReturn(Optional.empty());
		when(productRepository.findById(productId)).thenReturn(Optional.of(dbInfo));

		ProductInfo result = productQueryService.getProductDetail(productId);

		assertThat(result).isEqualTo(dbInfo);
		verify(productCacheMetrics).recordCacheMiss();
		verify(productCacheMetrics).recordDbLoad();
		verify(productCacheRepository).saveProductDetail(productId, dbInfo);
	}

	@Test
	@DisplayName("상세 DB 조회 결과가 없으면 NOT_FOUND 예외가 발생한다")
	void getProductDetail_throwsWhenNotFoundInDb() {
		ProductId productId = ProductId.of(3L);

		when(productCacheRepository.findProductDetail(productId)).thenReturn(Optional.empty());
		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productQueryService.getProductDetail(productId))
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType actual = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(actual).isEqualTo(ErrorType.NOT_FOUND);
			});
	}

	@Test
	@DisplayName("getProductByIds는 입력이 null 또는 empty면 빈 맵을 반환한다")
	void getProductByIds_returnsEmptyWhenInputNullOrEmpty() {
		assertThat(productQueryService.getProductByIds(null)).isEmpty();
		assertThat(productQueryService.getProductByIds(List.of())).isEmpty();
		verifyNoInteractions(productCacheRepository, productRepository, productCacheMetrics);
	}

	@Test
	@DisplayName("getProductByIds는 null 요소만 있으면 빈 맵을 반환한다")
	void getProductByIds_returnsEmptyWhenAllIdsAreNull() {
		Map<ProductId, ProductInfo> result = productQueryService.getProductByIds(Arrays.asList(null, null));

		assertThat(result).isEmpty();
		verifyNoInteractions(productCacheRepository, productRepository, productCacheMetrics);
	}

	@Test
	@DisplayName("getProductByIds는 중복/순서를 정리하고 캐시+DB 결과를 요청 순서로 반환한다")
	void getProductByIds_mergesCacheAndDbAndKeepsOrder() {
		ProductId id2 = ProductId.of(2L);
		ProductId id1 = ProductId.of(1L);
		ProductId id3 = ProductId.of(3L);
		List<ProductId> requested = Arrays.asList(id2, id1, id2, null, id3);

		ProductInfo cachedInfo = productInfo(2L);
		ProductInfo dbInfo = productInfo(1L);

		when(productCacheRepository.findProductDetailsByIds(List.of(id2, id1, id3)))
			.thenReturn(Map.of(id2, cachedInfo));
		when(productRepository.findInfosByIds(List.of(id1, id3))).thenReturn(List.of(dbInfo));

		Map<ProductId, ProductInfo> result = productQueryService.getProductByIds(requested);

		assertThat(result.keySet()).containsExactly(id2, id1);
		assertThat(result).containsEntry(id2, cachedInfo);
		assertThat(result).containsEntry(id1, dbInfo);
		assertThat(result).doesNotContainKey(id3);

		verify(productCacheMetrics).recordBatchCacheHitCount(1);
		verify(productCacheMetrics).recordBatchCacheMissCount(2);
		verify(productCacheMetrics).recordDbLoad();
		verify(productCacheRepository).saveProductDetails(List.of(dbInfo));
	}

	@Test
	@DisplayName("getProductByIds는 모두 캐시에 있으면 DB를 조회하지 않는다")
	void getProductByIds_doesNotLoadFromDbWhenAllCached() {
		ProductId id1 = ProductId.of(1L);
		ProductId id2 = ProductId.of(2L);
		List<ProductId> requested = List.of(id2, id1, id2);
		ProductInfo info1 = productInfo(1L);
		ProductInfo info2 = productInfo(2L);

		when(productCacheRepository.findProductDetailsByIds(List.of(id2, id1)))
			.thenReturn(Map.of(id1, info1, id2, info2));

		Map<ProductId, ProductInfo> result = productQueryService.getProductByIds(requested);

		assertThat(result.keySet()).containsExactly(id2, id1);
		assertThat(result).containsEntry(id1, info1).containsEntry(id2, info2);

		verify(productCacheMetrics).recordBatchCacheHitCount(2);
		verify(productCacheMetrics).recordBatchCacheMissCount(0);
		verify(productRepository, never()).findInfosByIds(anyList());
		verify(productCacheRepository, never()).saveProductDetails(anyList());
	}

	@Test
	@DisplayName("isListCacheable: 음수 페이지 번호는 캐시 대상이 아니다")
	void isListCacheable_returnsFalseForNegativePageNumber() {
		Pageable pageable = mock(Pageable.class);
		when(pageable.getPageNumber()).thenReturn(-1);

		Boolean cacheable = ReflectionTestUtils.invokeMethod(productQueryService, "isListCacheable", pageable);

		assertThat(cacheable).isFalse();
	}

	@Test
	@DisplayName("executeSingleFlight: 기존 in-flight future가 있으면 join 결과를 반환한다")
	void executeSingleFlight_returnsExistingFutureResult() {
		ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
		inFlight.put("key", CompletableFuture.completedFuture("from-existing"));

		String result = ReflectionTestUtils.invokeMethod(
			productQueryService,
			"executeSingleFlight",
			inFlight,
			"key",
			(Supplier<String>) () -> "from-loader"
		);

		assertThat(result).isEqualTo("from-existing");
	}

	@Test
	@DisplayName("executeSingleFlight: 기존 future가 checked 예외로 실패하면 IllegalStateException으로 변환한다")
	void executeSingleFlight_convertsCheckedExceptionFromExistingFuture() {
		ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new Exception("checked-failure"));
		inFlight.put("key", failedFuture);

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
			productQueryService,
			"executeSingleFlight",
			inFlight,
			"key",
			(Supplier<String>) () -> "from-loader"
		))
			.isInstanceOf(IllegalStateException.class)
			.hasCauseInstanceOf(Exception.class)
			.hasMessageContaining("checked-failure");
	}

	private ProductInfo productInfo(final Long productId) {
		return new ProductInfo(productId, "product-" + productId, 1000L + productId, 10L, "brand", 0L, 0L, 0L);
	}
}
