package com.loopers.infrastructure.payment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.interfaces.api.common.ApiResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgClientImpl implements PgClient {

	private final PgFeignClient pgFeignClient;

	@CircuitBreaker(name = "pgClient", fallbackMethod = "requestFallback")
	@Retry(name = "pgClient")
	@Override
	public PaymentInfo.transaction request(PgClientDto.PgPaymentRequest request) {
		ApiResponse<PgClientDto.PgPaymentTransaction> response = pgFeignClient.request(request);
		PgClientDto.PgPaymentTransaction data = response.data();
		return PaymentInfo.transaction.toData(data);
	}

	@CircuitBreaker(name = "pgClient", fallbackMethod = "findOrderFallback")
	@Retry(name = "pgClient")
	@Override
	public PaymentInfo.order findOrder(final String orderId) {
		ApiResponse<PgClientDto.PgPaymentOrderResponse> response = pgFeignClient.findOrder(orderId);
		PgClientDto.PgPaymentOrderResponse data = response.data();
		return PaymentInfo.order.toData(data);
	}

	@CircuitBreaker(name = "pgClient", fallbackMethod = "findTransactionFallback")
	@Retry(name = "pgClient")
	@Override
	public PaymentInfo.transaction findTransaction(final String transactionKey) {
		ApiResponse<PgClientDto.PgPaymentTransaction> response = pgFeignClient.findTransaction(transactionKey);
		PgClientDto.PgPaymentTransaction data = response.data();
		return PaymentInfo.transaction.toData(data);
	}

	public PaymentInfo.transaction requestFallback(PgClientDto.PgPaymentRequest request, Exception e) {
		log.warn("PG 결제 요청 실패, fallback 실행. 주문 ID: {}, error: {}", request.orderId(), e.getMessage());
		return new PaymentInfo.transaction(
			null,
			request.orderId(),
			request.amount(),
			request.cardNo(),
			request.cardType(),
			TransactionStatus.FAILED,
			"PG 통신 실패"
		);
	}

	public PaymentInfo.order findOrderFallback(String orderId, Exception e) {
		log.warn("PG 주문 조회 실패, fallback 실행. 주문 ID: {}, error: {}", orderId, e.getMessage());
		return new PaymentInfo.order(orderId, List.of());
	}

	public PaymentInfo.transaction findTransactionFallback(String transactionKey, Exception e) {
		log.warn("PG 트랜잭션 조회 실패, fallback 실행. transactionKey: {}, error: {}", transactionKey, e.getMessage());
		return new PaymentInfo.transaction(
			transactionKey,
			null,
			null,
			null,
			null,
			TransactionStatus.PENDING, // 실패시 PENDING 유지
			"PG 통신 실패"
		);
	}

}
