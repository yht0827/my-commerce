package com.loopers.infrastructure.payment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loopers.domain.payment.PaymentData;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgClientException;
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
	public PaymentInfo.Transaction request(PaymentData.PaymentRequest request) {
		ApiResponse<PgClientDto.PgPaymentTransaction> response = pgFeignClient.request(PgClientDto.PgPaymentRequest.from(request));
		PgClientDto.PgPaymentTransaction data = response.data();
		return new PaymentInfo.Transaction(
			data.transactionKey(),
			data.orderId(),
			data.amount(),
			data.cardNo(),
			data.cardType(),
			data.status(),
			data.reason()
		);
	}

	@CircuitBreaker(name = "pgClient", fallbackMethod = "findOrderFallback")
	@Retry(name = "pgClient")
	@Override
	public PaymentInfo.Order findOrder(final String orderId) {
		ApiResponse<PgClientDto.PgPaymentOrderResponse> response = pgFeignClient.findOrder(orderId);
		PgClientDto.PgPaymentOrderResponse data = response.data();
		List<PaymentInfo.TransactionResponse> list = data.transactions().stream()
			.map(transactionResponse -> new PaymentInfo.TransactionResponse(
				transactionResponse.transactionKey(),
				transactionResponse.status(),
				transactionResponse.reason()
			)).toList();
		return new PaymentInfo.Order(data.orderId(), list);
	}

	@CircuitBreaker(name = "pgClient", fallbackMethod = "findTransactionFallback")
	@Retry(name = "pgClient")
	@Override
	public PaymentInfo.Transaction findTransaction(final String transactionKey) {
		ApiResponse<PgClientDto.PgPaymentTransaction> response = pgFeignClient.findTransaction(transactionKey);
		PgClientDto.PgPaymentTransaction data = response.data();
		return new PaymentInfo.Transaction(
			data.transactionKey(),
			data.orderId(),
			data.amount(),
			data.cardNo(),
			data.cardType(),
			data.status(),
			data.reason()
		);
	}

	public PaymentInfo.Transaction requestFallback(PaymentData.PaymentRequest request, Exception e) {
		log.warn("PG 결제 요청 실패, fallback 실행. 주문 ID: {}, error: {}", request.orderId(), e.getMessage());
		throw new PgClientException("PG 결제 요청에 실패했습니다.", e);
	}

	public PaymentInfo.Order findOrderFallback(String orderId, Exception e) {
		log.warn("PG 주문 조회 실패, fallback 실행. 주문 ID: {}, error: {}", orderId, e.getMessage());
		throw new PgClientException("PG 주문 조회에 실패했습니다.", e);
	}

	public PaymentInfo.Transaction findTransactionFallback(String transactionKey, Exception e) {
		log.warn("PG 트랜잭션 조회 실패, fallback 실행. transactionKey: {}, error: {}", transactionKey, e.getMessage());
		throw new PgClientException("PG 트랜잭션 조회에 실패했습니다.", e);
	}

}
