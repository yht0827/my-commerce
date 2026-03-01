package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.brand.BrandName;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductAggregateJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("상품 목록 통합 테스트")
class ProductListIntegrationTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductQueryService productQueryService;

	@Autowired
	private ProductJpaRepository productJpaRepository;

	@Autowired
	private ProductAggregateJpaRepository productAggregateJpaRepository;

	@Autowired
	private BrandJpaRepository brandJpaRepository;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@MockitoSpyBean
	private ProductRepository spyProductRepository;

	private BrandId primaryBrandId;
	private Product firstProduct;
	private Product secondProduct;
	private Product thirdProduct;

	@BeforeEach
	void setUp() {
		productAggregateJpaRepository.deleteAllInBatch();
		productJpaRepository.deleteAllInBatch();
		brandJpaRepository.deleteAllInBatch();
		evictAllProductListCaches();

		Brand primaryBrand = brandJpaRepository.saveAndFlush(new Brand(new BrandName("brand-a")));
		Brand secondaryBrand = brandJpaRepository.saveAndFlush(new Brand(new BrandName("brand-b")));

		primaryBrandId = BrandId.of(primaryBrand.getId());
		BrandId secondaryBrandId = BrandId.of(secondaryBrand.getId());

		firstProduct = saveProduct(primaryBrandId, "product-300", 300L, 10L);
		waitForDifferentCreatedAt();
		secondProduct = saveProduct(primaryBrandId, "product-100", 100L, 10L);
		waitForDifferentCreatedAt();
		thirdProduct = saveProduct(primaryBrandId, "product-200", 200L, 10L);
		saveProduct(secondaryBrandId, "other-brand-product", 50L, 10L);

		saveAggregate(firstProduct, 5L, 2L, 100L);
		saveAggregate(secondProduct, 1L, 5L, 20L);
		saveAggregate(thirdProduct, 3L, 3L, 300L);

		clearInvocations(spyProductRepository);
	}

	@Test
	@DisplayName("브랜드 필터 + 가격 오름차순 + 페이지네이션이 동작한다")
	void getProductList_priceAscWithBrandFilterAndPagination() {
		Page<ProductInfo> firstPage = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(0, 2, Sort.by("price").ascending())
		);

		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(secondProduct.getId(), thirdProduct.getId());

		Page<ProductInfo> secondPage = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(1, 2, Sort.by("price").ascending())
		);

		assertThat(secondPage.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(firstProduct.getId());
	}

	@Test
	@DisplayName("좋아요 내림차순 정렬이 동작한다")
	void getProductList_likesDescOrdering() {
		Page<ProductInfo> page = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(0, 3, Sort.by("likesCount").descending())
		);

		assertThat(page.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(firstProduct.getId(), thirdProduct.getId(), secondProduct.getId());
	}

	@Test
	@DisplayName("최신순 정렬이 동작한다")
	void getProductList_latestOrdering() {
		Page<ProductInfo> page = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(0, 3, Sort.by("createdAt").descending())
		);

		assertThat(page.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(thirdProduct.getId(), secondProduct.getId(), firstProduct.getId());
	}

	@Test
	@DisplayName("주문수 내림차순 정렬이 동작한다")
	void getProductList_ordersDescOrdering() {
		Page<ProductInfo> page = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(0, 3, Sort.by("orderCount").descending())
		);

		assertThat(page.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(secondProduct.getId(), thirdProduct.getId(), firstProduct.getId());
	}

	@Test
	@DisplayName("조회수 내림차순 정렬이 동작한다")
	void getProductList_viewsDescOrdering() {
		Page<ProductInfo> page = productRepository.getProductList(
			primaryBrandId,
			PageRequest.of(0, 3, Sort.by("viewCount").descending())
		);

		assertThat(page.getContent())
			.extracting(ProductInfo::productId)
			.containsExactly(thirdProduct.getId(), firstProduct.getId(), secondProduct.getId());
	}

	@Test
	@DisplayName("정렬이 다르면 별도 리스트 캐시를 사용한다")
	void getProductList_usesDifferentCachePerSort() {
		Pageable pricePageable = PageRequest.of(0, 3, Sort.by("price").ascending());
		Pageable likesPageable = PageRequest.of(0, 3, Sort.by("likesCount").descending());

		Page<ProductInfo> firstPriceResult = productQueryService.getProductList(new ProductData.GetProductList(primaryBrandId, pricePageable));
		Page<ProductInfo> firstLikesResult = productQueryService.getProductList(new ProductData.GetProductList(primaryBrandId, likesPageable));

		Page<ProductInfo> secondPriceResult = productQueryService.getProductList(new ProductData.GetProductList(primaryBrandId, pricePageable));
		Page<ProductInfo> secondLikesResult = productQueryService.getProductList(new ProductData.GetProductList(primaryBrandId, likesPageable));

		verify(spyProductRepository, times(2)).getProductList(any(BrandId.class), any(Pageable.class));

		assertThat(secondPriceResult.getContent())
			.extracting(ProductInfo::productId)
			.containsExactlyElementsOf(extractIds(firstPriceResult));

		assertThat(secondLikesResult.getContent())
			.extracting(ProductInfo::productId)
			.containsExactlyElementsOf(extractIds(firstLikesResult));

		assertThat(extractIds(firstPriceResult)).isNotEqualTo(extractIds(firstLikesResult));

		Set<String> cacheKeys = redisTemplate.keys("productList:" + primaryBrandId.getBrandId() + ":0:3:*");
		assertThat(cacheKeys).isNotNull();
		assertThat(cacheKeys).hasSize(2);
	}

	private Product saveProduct(final BrandId brandId, final String name, final Long price, final Long quantity) {
		return productJpaRepository.saveAndFlush(
			Product.builder()
				.brandId(brandId)
				.name(new ProductName(name))
				.price(new Price(price))
				.quantity(new Quantity(quantity))
				.build()
		);
	}

	private void saveAggregate(final Product product, final Long likeCount, final Long orderCount, final Long viewCount) {
		productAggregateJpaRepository.saveAndFlush(
			ProductAggregate.builder()
				.productId(ProductId.of(product.getId()))
				.likeCount(new LikeCount(likeCount))
				.orderCount(new OrderCount(orderCount))
				.viewCount(new ViewCount(viewCount))
				.build()
		);
	}

	private List<Long> extractIds(final Page<ProductInfo> page) {
		return page.getContent().stream().map(ProductInfo::productId).toList();
	}

	private void evictAllProductListCaches() {
		Set<String> keys = redisTemplate.keys("productList:*");
		if (keys == null || keys.isEmpty()) {
			return;
		}
		redisTemplate.delete(keys);
	}

	private void waitForDifferentCreatedAt() {
		try {
			Thread.sleep(5L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while preparing test data", e);
		}
	}
}
