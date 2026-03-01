package com.loopers.interfaces.api.order;

import java.util.List;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Orders V1 API", description = "Orders API 입니다.")
public interface OrderV1ApiSpec {

	@Operation(
		summary = "주문 생성",
		description = "주문을 생성합니다."
	)
	ApiResponse<OrderDto.V1.OrderResponse> createOrder(final String userId, final String idempotencyKey,
		final OrderDto.V1.OrderRequest orderRequest);

	@Operation(
		summary = "주문 목록 조회",
		description = "사용자의 주문 목록을 조회합니다."
	)
	ApiResponse<List<OrderDto.V1.OrderResponse>> getOrders(final String userId);

	@Operation(
		summary = "주문 상세 조회",
		description = "주문 상세 정보를 조회합니다."
	)
	ApiResponse<OrderDto.V1.OrderResponse> getOrder(final String userId, final String orderId);
}
