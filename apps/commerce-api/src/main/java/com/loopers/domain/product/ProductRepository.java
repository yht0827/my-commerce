package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandId;

public interface ProductRepository {
	Optional<ProductInfo> findById(final ProductId id);

	Optional<Product> findEntityById(final ProductId id);

	Optional<Product> findByIdWithPessimisticLock(ProductId id);

	Optional<Product> findByIdWithOptimisticLock(ProductId id);

	Page<ProductInfo> getProductList(final BrandId brandId, final Pageable pageable);

	Product save(final Product product);

	List<ProductInfo> findInfosByIds(List<ProductId> ids);
}
