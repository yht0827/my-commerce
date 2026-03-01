package com.loopers.infrastructure.order;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.order.OrderHistory;
import com.loopers.domain.order.OrderHistoryRepository;
import com.loopers.domain.user.UserId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderHistoryRepositoryImpl implements OrderHistoryRepository {

	private final OrderHistoryJpaRepository orderHistoryJpaRepository;

	@Override
	public boolean createIfNotExists(final String userId, final String idempotencyKey) {
		return orderHistoryJpaRepository.createIfNotExists(userId, idempotencyKey) > 0;
	}

	@Override
	public Optional<OrderHistory> findByUserIdAndIdempotencyKey(final String userId, final String idempotencyKey) {
		return orderHistoryJpaRepository.findByUserIdAndIdempotencyKey(UserId.of(userId), idempotencyKey);
	}
}
