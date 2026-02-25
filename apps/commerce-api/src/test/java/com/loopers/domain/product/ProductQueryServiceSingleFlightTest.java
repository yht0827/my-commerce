package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.loopers.domain.brand.BrandId;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductQueryService singleflight 테스트")
class ProductQueryServiceSingleFlightTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductCacheRepository productCacheRepository;

	@Mock
	private ProductCacheMetrics productCacheMetrics;

	@InjectMocks
	private ProductQueryService productQueryService;

	@Test
	@DisplayName("동일 리스트 키 캐시 미스 동시 요청은 DB 로드를 1회만 수행한다")
	void getProductList_singleFlightOnConcurrentMiss() throws Exception {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());
		ProductData.GetProductList query = new ProductData.GetProductList(brandId, pageable);

		AtomicInteger cacheLookupCount = new AtomicInteger();
		CompletableFuture<Void> secondLookupObserved = new CompletableFuture<>();

		when(productCacheRepository.findProductList(eq(brandId), eq(pageable))).thenAnswer(invocation -> {
			if (cacheLookupCount.incrementAndGet() >= 2) {
				secondLookupObserved.complete(null);
			}
			return Optional.empty();
		});

		Page<ProductInfo> dbPage = new PageImpl<>(
			List.of(new ProductInfo(1L, "product-1", 1000L, 10L, "brand-1", 0L, 0L, 0L)),
			pageable,
			1
		);

		when(productRepository.getProductList(eq(brandId), eq(pageable))).thenAnswer(invocation -> {
			secondLookupObserved.get(2, TimeUnit.SECONDS);
			return dbPage;
		});

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CompletableFuture<Page<ProductInfo>> first = CompletableFuture.supplyAsync(
				() -> productQueryService.getProductList(query),
				executor
			);
			CompletableFuture<Page<ProductInfo>> second = CompletableFuture.supplyAsync(
				() -> productQueryService.getProductList(query),
				executor
			);

			Page<ProductInfo> firstResult = first.get(3, TimeUnit.SECONDS);
			Page<ProductInfo> secondResult = second.get(3, TimeUnit.SECONDS);

			assertThat(firstResult.getContent()).containsExactlyElementsOf(secondResult.getContent());
		} finally {
			executor.shutdownNow();
		}

		verify(productRepository, times(1)).getProductList(eq(brandId), eq(pageable));
		verify(productCacheRepository, times(1)).saveProductList(eq(brandId), eq(pageable), any());
		verify(productCacheMetrics, times(1)).recordDbLoad();
	}

	@Test
	@DisplayName("동일 상세 키 캐시 미스 동시 요청은 DB 로드를 1회만 수행한다")
	void getProductDetail_singleFlightOnConcurrentMiss() throws Exception {
		ProductId productId = ProductId.of(1L);

		AtomicInteger cacheLookupCount = new AtomicInteger();
		CompletableFuture<Void> secondLookupObserved = new CompletableFuture<>();

		when(productCacheRepository.findProductDetail(eq(productId))).thenAnswer(invocation -> {
			if (cacheLookupCount.incrementAndGet() >= 2) {
				secondLookupObserved.complete(null);
			}
			return Optional.empty();
		});

		ProductInfo productInfo = new ProductInfo(1L, "product-1", 1000L, 10L, "brand-1", 0L, 0L, 0L);

		when(productRepository.findById(eq(productId))).thenAnswer(invocation -> {
			secondLookupObserved.get(2, TimeUnit.SECONDS);
			return Optional.of(productInfo);
		});

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CompletableFuture<ProductInfo> first = CompletableFuture.supplyAsync(
				() -> productQueryService.getProductDetail(productId),
				executor
			);
			CompletableFuture<ProductInfo> second = CompletableFuture.supplyAsync(
				() -> productQueryService.getProductDetail(productId),
				executor
			);

			ProductInfo firstResult = first.get(3, TimeUnit.SECONDS);
			ProductInfo secondResult = second.get(3, TimeUnit.SECONDS);

			assertThat(firstResult).isEqualTo(secondResult);
		} finally {
			executor.shutdownNow();
		}

		verify(productRepository, times(1)).findById(eq(productId));
		verify(productCacheRepository, times(1)).saveProductDetail(eq(productId), eq(productInfo));
		verify(productCacheMetrics, times(1)).recordDbLoad();
	}
}
