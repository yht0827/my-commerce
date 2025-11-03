package com.loopers.infrastructure.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.loopers.interfaces.api.ApiResponse;

@FeignClient(
	name = "${clients.pg-simulator.name}",
	url = "${clients.pg-simulator.url}",
	path = "/pg/v1/payments"
)
public interface PgFeignClient {

	@PostMapping
	ApiResponse<PgClientDto.PgPaymentTransaction> request(PgClientDto.PgPaymentRequest request);

	@GetMapping
	ApiResponse<PgClientDto.PgPaymentOrderResponse> findOrder(final String orderId);

	@GetMapping("/{transactionKey}")
	ApiResponse<PgClientDto.PgPaymentTransaction> findTransaction(@PathVariable(name = "transactionKey") String transactionKey);
}
