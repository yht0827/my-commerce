package com.loopers.domain.order;

import java.util.Optional;

public interface OrderHistoryRepository {

	boolean createIfNotExists(String userId, String idempotencyKey);

	Optional<OrderHistory> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
