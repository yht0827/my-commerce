package com.loopers.infrastructure.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.order.Order;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

	@Query("SELECT o FROM Order o WHERE o.userId.userId = :userId ORDER BY o.createdAt DESC")
	List<Order> findAllOrdersByUserId(final String userId);

	@Query("SELECT o FROM Order o WHERE o.userId.userId = :userId AND o.orderNumber.orderNumber = :orderNumber")
	Optional<Order> findByOrderNumberAndUserId(String orderNumber, String userId);

	@Query("SELECT o FROM Order o WHERE o.orderNumber.orderNumber = :orderNumber")
	Optional<Order> findByOrderNumber(String orderNumber);
}
