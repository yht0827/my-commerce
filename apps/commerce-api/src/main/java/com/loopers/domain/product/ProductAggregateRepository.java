package com.loopers.domain.product;

import java.util.List;

public interface ProductAggregateRepository {

	boolean incrementLikeCount(final ProductId productId);

	boolean decrementLikeCount(final ProductId productId);

	boolean incrementOrderCount(final ProductId productId);

	boolean incrementViewCount(final ProductId productId);

	List<ProductId> findAllProductIds();

	void replaceCounts(ProductId productId, long likeCount, long orderCount, long viewCount);

	void createIfNotExists(final ProductId productId);
}
