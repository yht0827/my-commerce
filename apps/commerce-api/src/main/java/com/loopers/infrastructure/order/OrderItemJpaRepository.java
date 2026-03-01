package com.loopers.infrastructure.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.order.OrderItem;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

	@Query("SELECT oi FROM OrderItem oi WHERE oi.orderId.orderId = :orderId")
	List<OrderItem> findAllByOrderId(final String orderId);

	@Query("SELECT oi FROM OrderItem oi WHERE oi.orderId.orderId IN :orderIds")
	List<OrderItem> findAllByOrderIdIn(final List<String> orderIds);

	@Query(
		value = "SELECT COUNT(DISTINCT oi.order_id) "
			+ "FROM order_items oi "
			+ "INNER JOIN orders o ON o.order_number = oi.order_id "
			+ "WHERE oi.product_id = :productId AND o.status = 'CONFIRMED'",
		nativeQuery = true
	)
	long countConfirmedOrdersByProductId(@Param("productId") Long productId);
}
