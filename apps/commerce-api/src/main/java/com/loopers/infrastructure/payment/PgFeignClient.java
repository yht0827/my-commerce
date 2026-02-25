package com.loopers.infrastructure.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.loopers.interfaces.api.common.ApiResponse;

@FeignClient(
	name = "${clients.pg-simulator.name}",
	url = "${clients.pg-simulator.url}",
	path = "/api/v1/payments"
)
public interface PgFeignClient {

	@PostMapping
	ApiResponse<PgClientDto.PgPaymentTransaction> request(@RequestBody PgClientDto.PgPaymentRequest request);

	@GetMapping
	ApiResponse<PgClientDto.PgPaymentOrderResponse> findOrder(@RequestParam(name = "orderId") String orderId);

	@GetMapping("/{transactionKey}")
	ApiResponse<PgClientDto.PgPaymentTransaction> findTransaction(@PathVariable(name = "transactionKey") String transactionKey);
}
