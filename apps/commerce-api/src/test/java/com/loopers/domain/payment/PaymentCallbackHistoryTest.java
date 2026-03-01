package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("PaymentCallbackHistory 테스트")
class PaymentCallbackHistoryTest {

	@Test
	@DisplayName("dedupeKey는 trim 되어 저장된다")
	void constructor_normalizesDedupeKey() {
		PaymentCallbackHistory history = history("  dedupe-key  ", PaymentCallbackProcessStatus.RECEIVED);

		assertThat(field(history, "dedupeKey")).isEqualTo("dedupe-key");
	}

	@Test
	@DisplayName("guard는 dedupeKey가 비어있으면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenDedupeKeyBlank() {
		PaymentCallbackHistory history = history(" ", PaymentCallbackProcessStatus.RECEIVED);

		assertCoreException(() -> history.guard(), ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 dedupeKey 길이가 128자를 초과하면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenDedupeKeyTooLong() {
		PaymentCallbackHistory history = history("x".repeat(129), PaymentCallbackProcessStatus.RECEIVED);

		assertCoreException(() -> history.guard(), ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 processStatus가 null이면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenProcessStatusNull() {
		PaymentCallbackHistory history = history("dedupe-key", null);

		assertCoreException(() -> history.guard(), ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("COMPLETED 상태에서 markProcessing을 호출해도 상태가 유지된다")
	void markProcessing_keepsCompletedStatus() {
		PaymentCallbackHistory history = history("dedupe-key", PaymentCallbackProcessStatus.COMPLETED);

		history.markProcessing();

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.COMPLETED);
	}

	@Test
	@DisplayName("RECEIVED 상태에서 markProcessing을 호출하면 PROCESSING으로 변경된다")
	void markProcessing_changesStatusToProcessing() {
		PaymentCallbackHistory history = history("dedupe-key", PaymentCallbackProcessStatus.RECEIVED);

		history.markProcessing();

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.PROCESSING);
	}

	@Test
	@DisplayName("complete를 호출하면 COMPLETED로 바뀌고 실패 사유가 제거된다")
	void complete_setsCompletedAndClearsReason() {
		PaymentCallbackHistory history = history("dedupe-key", PaymentCallbackProcessStatus.PROCESSING);
		history.fail("old-reason");

		history.complete();

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.COMPLETED);
		assertThat(field(history, "failedReason")).isNull();
		assertThat(field(history, "processedAt")).isNotNull();
	}

	@Test
	@DisplayName("fail을 호출하면 FAILED로 바뀌고 실패 사유/처리 시각이 기록된다")
	void fail_setsFailedStatusAndReason() {
		PaymentCallbackHistory history = history("dedupe-key", PaymentCallbackProcessStatus.PROCESSING);

		history.fail("signature-invalid");

		assertThat(field(history, "processStatus")).isEqualTo(PaymentCallbackProcessStatus.FAILED);
		assertThat(field(history, "failedReason")).isEqualTo("signature-invalid");
		assertThat(field(history, "processedAt")).isNotNull();
	}

	private PaymentCallbackHistory history(
		final String dedupeKey,
		final PaymentCallbackProcessStatus processStatus
	) {
		return new PaymentCallbackHistory(
			dedupeKey,
			"tx-1",
			"ORD-1",
			TransactionStatus.SUCCESS,
			processStatus,
			null,
			null
		);
	}

	private Object field(final Object target, final String fieldName) {
		return ReflectionTestUtils.getField(target, fieldName);
	}

	private void assertCoreException(final ThrowingRunnable runnable, final ErrorType errorType) {
		assertThatThrownBy(runnable::run)
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType actual = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(actual).isEqualTo(errorType);
			});
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run();
	}
}
