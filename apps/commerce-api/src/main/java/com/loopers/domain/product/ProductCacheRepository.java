package com.loopers.domain.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandId;
import com.loopers.support.cache.CacheablePage;

public interface ProductCacheRepository {

	Optional<CacheablePage<ProductInfo>> findProductList(BrandId brandId, Pageable pageable);

	void saveProductList(BrandId brandId, Pageable pageable, CacheablePage<ProductInfo> productList);

	Optional<ProductInfo> findProductDetail(ProductId productId);

	void saveProductDetail(ProductId productId, ProductInfo productInfo);

	Map<ProductId, ProductInfo> findProductDetailsByIds(List<ProductId> productIds);

	void saveProductDetails(List<ProductInfo> productInfos);

	void evictProductDetail(ProductId productId);

	void evictProductList();
}
