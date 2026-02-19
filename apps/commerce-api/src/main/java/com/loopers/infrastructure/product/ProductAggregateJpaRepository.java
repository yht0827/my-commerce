package com.loopers.infrastructure.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.product.ProductAggregate;

public interface ProductAggregateJpaRepository extends JpaRepository<ProductAggregate, Long> {

	@Modifying
	@Query("UPDATE ProductAggregate pa SET pa.likeCount.likeCount = pa.likeCount.likeCount + 1 WHERE pa.productId.productId = :productId")
	int incrementLikeCount(@Param("productId") Long productId);

	@Modifying
	@Query("UPDATE ProductAggregate pa SET pa.likeCount.likeCount = CASE WHEN pa.likeCount.likeCount > 0 THEN pa.likeCount.likeCount - 1 ELSE 0 END WHERE pa.productId.productId = :productId")
	int decrementLikeCount(@Param("productId") Long productId);

	@Modifying
	@Query(
		value = "INSERT INTO product_aggregate (product_id, like_count, created_at, updated_at, deleted_at) "
			+ "VALUES (:productId, 0, NOW(6), NOW(6), NULL) "
			+ "ON DUPLICATE KEY UPDATE product_id = product_id",
		nativeQuery = true
	)
	int createIfNotExists(@Param("productId") Long productId);
}
