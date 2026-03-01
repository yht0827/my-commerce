package com.loopers.infrastructure.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.support.cache.CacheablePage;

@DisplayName("ProductCacheRepositoryImpl 테스트")
@ExtendWith(MockitoExtension.class)
class ProductCacheRepositoryImplTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ProductCacheKeys productCacheKeys;

	@Mock
	private ValueOperations<String, Object> valueOperations;

	@InjectMocks
	@Spy
	private ProductCacheRepositoryImpl repository;

	@Test
	@DisplayName("findProductList는 캐시가 없으면 empty를 반환한다")
	void findProductList_returnsEmptyWhenCacheMiss() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 10);
		when(productCacheKeys.buildProductListKey(brandId, pageable)).thenReturn("product-list-key");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("product-list-key")).thenReturn(null);

		Optional<CacheablePage<ProductInfo>> result = repository.findProductList(brandId, pageable);

		assertThat(result).isEmpty();
		verifyNoInteractions(objectMapper);
	}

	@Test
	@DisplayName("findProductList는 캐시가 있으면 CacheablePage로 변환해 반환한다")
	void findProductList_returnsConvertedPageWhenCacheHit() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 10);
		Object cachedValue = Map.of("content", List.of());
		CacheablePage<ProductInfo> expectedPage = CacheablePage.from(new PageImpl<>(List.of(), pageable, 0));

		when(productCacheKeys.buildProductListKey(brandId, pageable)).thenReturn("product-list-key");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("product-list-key")).thenReturn(cachedValue);
		when(objectMapper.convertValue(eq(cachedValue), ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<CacheablePage<ProductInfo>>>any()))
			.thenReturn(expectedPage);

		Optional<CacheablePage<ProductInfo>> result = repository.findProductList(brandId, pageable);

		assertThat(result).contains(expectedPage);
	}

	@Test
	@DisplayName("findProductDetail은 캐시 값이 ProductInfo면 반환한다")
	void findProductDetail_returnsCachedProductInfo() {
		ProductId productId = ProductId.of(7L);
		ProductInfo productInfo = new ProductInfo(7L, "p-7", 1000L, 10L, "brand", 0L, 0L, 0L);

		when(productCacheKeys.buildProductDetailKey(productId)).thenReturn("detail:7");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("detail:7")).thenReturn(productInfo);

		Optional<ProductInfo> result = repository.findProductDetail(productId);

		assertThat(result).contains(productInfo);
	}

	@Test
	@DisplayName("findProductDetail은 캐시 값 타입이 다르면 empty를 반환한다")
	void findProductDetail_returnsEmptyWhenCachedTypeMismatch() {
		ProductId productId = ProductId.of(7L);

		when(productCacheKeys.buildProductDetailKey(productId)).thenReturn("detail:7");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("detail:7")).thenReturn("not-product-info");

		Optional<ProductInfo> result = repository.findProductDetail(productId);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findProductDetailsByIds는 입력이 null이면 빈 맵을 반환한다")
	void findProductDetailsByIds_returnsEmptyWhenInputNull() {
		assertThat(repository.findProductDetailsByIds(null)).isEmpty();
		verifyNoInteractions(redisTemplate);
	}

	@Test
	@DisplayName("findProductDetailsByIds는 입력이 비어있으면 빈 맵을 반환한다")
	void findProductDetailsByIds_returnsEmptyWhenInputEmpty() {
		assertThat(repository.findProductDetailsByIds(List.of())).isEmpty();
		verifyNoInteractions(redisTemplate);
	}

	@Test
	@DisplayName("findProductDetailsByIds는 multiGet 결과가 null이면 빈 맵을 반환한다")
	void findProductDetailsByIds_returnsEmptyWhenMultiGetIsNull() {
		List<ProductId> ids = List.of(ProductId.of(1L), ProductId.of(2L));
		when(productCacheKeys.buildProductDetailKey(ProductId.of(1L))).thenReturn("detail:1");
		when(productCacheKeys.buildProductDetailKey(ProductId.of(2L))).thenReturn("detail:2");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.multiGet(List.of("detail:1", "detail:2"))).thenReturn(null);

		Map<ProductId, ProductInfo> result = repository.findProductDetailsByIds(ids);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findProductDetailsByIds는 ProductInfo 값만 결과 맵에 포함한다")
	void findProductDetailsByIds_includesOnlyProductInfoValues() {
		ProductId id1 = ProductId.of(1L);
		ProductId id2 = ProductId.of(2L);
		ProductId id3 = ProductId.of(3L);
		List<ProductId> ids = List.of(id1, id2, id3);
		ProductInfo productInfo = new ProductInfo(1L, "p-1", 1000L, 10L, "brand", 0L, 0L, 0L);

		when(productCacheKeys.buildProductDetailKey(id1)).thenReturn("detail:1");
		when(productCacheKeys.buildProductDetailKey(id2)).thenReturn("detail:2");
		when(productCacheKeys.buildProductDetailKey(id3)).thenReturn("detail:3");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.multiGet(List.of("detail:1", "detail:2", "detail:3")))
			.thenReturn(Arrays.asList(productInfo, "wrong-type", null));

		Map<ProductId, ProductInfo> result = repository.findProductDetailsByIds(ids);

		assertThat(result).containsEntry(id1, productInfo);
		assertThat(result).doesNotContainKeys(id2, id3);
	}

	@Test
	@DisplayName("saveProductDetails는 입력이 null 또는 empty이면 저장을 생략한다")
	void saveProductDetails_skipsWhenInputNullOrEmpty() {
		repository.saveProductDetails(null);
		repository.saveProductDetails(List.of());

		verify(repository, never()).saveProductDetail(any(), any());
	}

	@Test
	@DisplayName("saveProductDetails는 각 상품을 상세 캐시에 저장한다")
	void saveProductDetails_savesEachProduct() {
		ProductInfo first = new ProductInfo(1L, "p-1", 1000L, 10L, "brand", 0L, 0L, 0L);
		ProductInfo second = new ProductInfo(2L, "p-2", 2000L, 20L, "brand", 0L, 0L, 0L);
		doNothing().when(repository).saveProductDetail(any(), any());

		repository.saveProductDetails(List.of(first, second));

		verify(repository, times(1)).saveProductDetail(ProductId.of(1L), first);
		verify(repository, times(1)).saveProductDetail(ProductId.of(2L), second);
	}

	@Test
	@DisplayName("saveProductList는 계산된 키로 Redis에 값을 저장한다")
	void saveProductList_storesInRedis() {
		BrandId brandId = BrandId.of(1L);
		Pageable pageable = PageRequest.of(0, 10);
		CacheablePage<ProductInfo> cacheablePage = CacheablePage.from(new PageImpl<>(List.of(), pageable, 0));

		when(productCacheKeys.buildProductListKey(brandId, pageable)).thenReturn("list:key");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		repository.saveProductList(brandId, pageable, cacheablePage);

		verify(valueOperations).set(eq("list:key"), eq(cacheablePage), any());
	}

	@Test
	@DisplayName("saveProductDetail은 계산된 키로 Redis에 값을 저장한다")
	void saveProductDetail_storesInRedis() {
		ProductId productId = ProductId.of(7L);
		ProductInfo productInfo = new ProductInfo(7L, "p-7", 1000L, 10L, "brand", 0L, 0L, 0L);

		when(productCacheKeys.buildProductDetailKey(productId)).thenReturn("detail:key");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		repository.saveProductDetail(productId, productInfo);

		verify(valueOperations).set(eq("detail:key"), eq(productInfo), any());
	}

	@Test
	@DisplayName("evictProductDetail은 계산된 키를 삭제한다")
	void evictProductDetail_deletesComputedKey() {
		ProductId productId = ProductId.of(9L);
		when(productCacheKeys.buildProductDetailKey(productId)).thenReturn("detail:9");

		repository.evictProductDetail(productId);

		verify(redisTemplate).delete("detail:9");
	}
}
