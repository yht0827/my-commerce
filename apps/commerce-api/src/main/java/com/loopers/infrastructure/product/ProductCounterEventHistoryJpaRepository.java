package com.loopers.infrastructure.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.product.ProductCounterEventHistory;
import com.loopers.domain.product.ProductCounterProcessStatus;
import com.loopers.domain.product.ProductCounterType;

public interface ProductCounterEventHistoryJpaRepository extends JpaRepository<ProductCounterEventHistory, Long> {

	@Modifying
	@Query(
		value = "INSERT IGNORE INTO product_counter_event_history "
			+ "(dedupe_key, product_id, counter_type, process_status, created_at, updated_at, deleted_at) "
			+ "VALUES (:dedupeKey, :productId, :counterType, 'RECEIVED', NOW(6), NOW(6), NULL)",
		nativeQuery = true
	)
	int createIfNotExists(
		@Param("dedupeKey") String dedupeKey,
		@Param("productId") Long productId,
		@Param("counterType") String counterType
	);

	Optional<ProductCounterEventHistory> findByDedupeKey(String dedupeKey);

	@Query(
		"SELECT COUNT(h) FROM ProductCounterEventHistory h "
			+ "WHERE h.productId.productId = :productId "
			+ "AND h.counterType = :counterType "
			+ "AND h.processStatus = :processStatus"
	)
	long countByProductIdAndCounterTypeAndProcessStatus(
		@Param("productId") Long productId,
		@Param("counterType") ProductCounterType counterType,
		@Param("processStatus") ProductCounterProcessStatus processStatus
	);

	@Query("SELECT h FROM ProductCounterEventHistory h WHERE h.processStatus = :processStatus ORDER BY h.updatedAt ASC")
	List<ProductCounterEventHistory> findByProcessStatusOrderByUpdatedAtAsc(
		@Param("processStatus") ProductCounterProcessStatus processStatus,
		Pageable pageable
	);
}
