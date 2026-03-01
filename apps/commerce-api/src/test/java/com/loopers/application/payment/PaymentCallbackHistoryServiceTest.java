package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.domain.payment.PaymentCallbackHistory;
import com.loopers.domain.payment.PaymentCallbackHistoryRepository;
import com.loopers.domain.payment.PaymentCallbackProcessStatus;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.util.HashingUtils;

@DisplayName("PaymentCallbackHistoryService 테스트")
@ExtendWith(MockitoExtension.class)
class PaymentCallbackHistoryServiceTest {

	@Mock
	private PaymentCallbackHistoryRepository repository;

	@InjectMocks
	private PaymentCallbackHistoryService service;

	@Test
	@DisplayName("callbackId가 유효 길이면 trim 값 자체를 dedupe key로 사용한다")
	void generateDedupeKey_returnsTrimmedCallbackId() {
		PaymentCommand.ProcessCallback command = callbackCommand(" tx-1 ", TransactionStatus.SUCCESS, "ORD-1", "  cb-1  ", " { } ");

		String result = service.generateDedupeKey(command);

		assertThat(result).isEqualTo("cb-1");
	}

	@Test
	@DisplayName("callbackId가 최대 길이를 초과하면 해시 키를 생성한다")
	void generateDedupeKey_hashesLongCallbackId() {
		String callbackId = "x".repeat(129);
		PaymentCommand.ProcessCallback command = callbackCommand("tx-1", TransactionStatus.SUCCESS, "ORD-1", callbackId, "{}");

		String result = service.generateDedupeKey(command);

		assertThat(result).isEqualTo(HashingUtils.sha256Hex(callbackId, "콜백 멱등 키 해시 생성에 실패했습니다."));
	}

	@Test
	@DisplayName("callbackId가 없으면 transaction/order/status/rawBody 조합으로 해시 키를 생성한다")
	void generateDedupeKey_hashesFallbackKeySource() {
		PaymentCommand.ProcessCallback command = callbackCommand(" tx-1 ", TransactionStatus.FAILED, null, null, "  {\"orderId\":\"ORD-1\"}  ");

		String result = service.generateDedupeKey(command);

		String expectedKeySource = "tx-1||FAILED|{\"orderId\":\"ORD-1\"}";
		assertThat(result).isEqualTo(HashingUtils.sha256Hex(expectedKeySource, "콜백 멱등 키 해시 생성에 실패했습니다."));
	}

	@Test
	@DisplayName("claim은 저장소 createIfNotExists 결과를 그대로 반환한다")
	void claim_delegatesToRepository() {
		PaymentCommand.ProcessCallback command = callbackCommand("tx-1", TransactionStatus.SUCCESS, "ORD-1", "cb-1", "{}");
		when(repository.createIfNotExists("dedupe-key", "tx-1", "ORD-1", TransactionStatus.SUCCESS)).thenReturn(true);

		boolean result = service.claim("dedupe-key", command);

		assertThat(result).isTrue();
		verify(repository).createIfNotExists("dedupe-key", "tx-1", "ORD-1", TransactionStatus.SUCCESS);
	}

	@Test
	@DisplayName("markProcessing은 이력을 조회한 뒤 PROCESSING으로 변경한다")
	void markProcessing_updatesHistoryStatus() {
		PaymentCallbackHistory history = history(PaymentCallbackProcessStatus.RECEIVED);
		when(repository.findByDedupeKey("dedupe-key")).thenReturn(Optional.of(history));

		service.markProcessing("dedupe-key");

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.PROCESSING);
	}

	@Test
	@DisplayName("complete는 이력을 조회한 뒤 COMPLETED로 변경한다")
	void complete_updatesHistoryStatus() {
		PaymentCallbackHistory history = history(PaymentCallbackProcessStatus.PROCESSING);
		history.fail("old-reason");
		when(repository.findByDedupeKey("dedupe-key")).thenReturn(Optional.of(history));

		service.complete("dedupe-key");

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.COMPLETED);
		assertThat(field(history, "failedReason")).isNull();
		assertThat(field(history, "processedAt")).isNotNull();
	}

	@Test
	@DisplayName("fail은 이력을 조회한 뒤 FAILED와 실패 사유를 기록한다")
	void fail_updatesHistoryStatusAndReason() {
		PaymentCallbackHistory history = history(PaymentCallbackProcessStatus.PROCESSING);
		when(repository.findByDedupeKey("dedupe-key")).thenReturn(Optional.of(history));

		service.fail("dedupe-key", "invalid-signature");

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.FAILED);
		assertThat(field(history, "failedReason")).isEqualTo("invalid-signature");
		assertThat(field(history, "processedAt")).isNotNull();
	}

	@Test
	@DisplayName("이력이 없으면 NOT_FOUND 예외가 발생한다")
	void complete_throwsWhenHistoryMissing() {
		when(repository.findByDedupeKey("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.complete("missing"))
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType errorType = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(errorType).isEqualTo(ErrorType.NOT_FOUND);
			});
	}

	private PaymentCommand.ProcessCallback callbackCommand(
		final String transactionKey,
		final TransactionStatus status,
		final String orderId,
		final String callbackId,
		final String rawBody
	) {
		return new PaymentCommand.ProcessCallback(
			transactionKey,
			status,
			orderId,
			callbackId,
			"1739950043",
			"signature",
			rawBody
		);
	}

	private PaymentCallbackHistory history(final PaymentCallbackProcessStatus status) {
		return new PaymentCallbackHistory(
			"dedupe-key",
			"tx-1",
			"ORD-1",
			TransactionStatus.SUCCESS,
			status,
			null,
			null
		);
	}

	private Object field(final Object target, final String fieldName) {
		return ReflectionTestUtils.getField(target, fieldName);
	}
}
