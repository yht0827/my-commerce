package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("ProductCounterEventHistory 테스트")
class ProductCounterEventHistoryTest {

	@Test
	@DisplayName("dedupeKey는 trim 되어 저장된다")
	void constructor_normalizesDedupeKey() {
		ProductCounterEventHistory history = history("  dedupe-key  ", ProductCounterProcessStatus.RECEIVED, ProductCounterType.LIKE);

		assertThat(field(history, "dedupeKey")).isEqualTo("dedupe-key");
	}

	@Test
	@DisplayName("guard는 dedupeKey가 비어있으면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenDedupeKeyBlank() {
		ProductCounterEventHistory history = history(" ", ProductCounterProcessStatus.RECEIVED, ProductCounterType.ORDER);

		assertCoreException(history::guard, ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 dedupeKey 길이가 128자를 초과하면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenDedupeKeyTooLong() {
		ProductCounterEventHistory history = history("x".repeat(129), ProductCounterProcessStatus.RECEIVED, ProductCounterType.ORDER);

		assertCoreException(history::guard, ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 productId가 null이면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenProductIdNull() {
		ProductCounterEventHistory history = new ProductCounterEventHistory(
			"dedupe-key",
			null,
			ProductCounterType.VIEW,
			ProductCounterProcessStatus.RECEIVED,
			null,
			null
		);

		assertCoreException(history::guard, ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 counterType이 null이면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenCounterTypeNull() {
		ProductCounterEventHistory history = new ProductCounterEventHistory(
			"dedupe-key",
			ProductId.of(1L),
			null,
			ProductCounterProcessStatus.RECEIVED,
			null,
			null
		);

		assertCoreException(history::guard, ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("guard는 processStatus가 null이면 BAD_REQUEST 예외를 던진다")
	void guard_throwsWhenProcessStatusNull() {
		ProductCounterEventHistory history = new ProductCounterEventHistory(
			"dedupe-key",
			ProductId.of(1L),
			ProductCounterType.LIKE,
			null,
			null,
			null
		);

		assertCoreException(history::guard, ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("COMPLETED 상태에서 markProcessing을 호출해도 상태가 유지된다")
	void markProcessing_keepsCompletedStatus() {
		ProductCounterEventHistory history = history("dedupe-key", ProductCounterProcessStatus.COMPLETED, ProductCounterType.VIEW);

		history.markProcessing();

		assertThat(field(history, "processStatus")).isEqualTo(ProductCounterProcessStatus.COMPLETED);
	}

	@Test
	@DisplayName("RECEIVED 상태에서 markProcessing을 호출하면 PROCESSING으로 변경된다")
	void markProcessing_changesToProcessing() {
		ProductCounterEventHistory history = history("dedupe-key", ProductCounterProcessStatus.RECEIVED, ProductCounterType.VIEW);

		history.markProcessing();

		assertThat(field(history, "processStatus")).isEqualTo(ProductCounterProcessStatus.PROCESSING);
	}

	@Test
	@DisplayName("complete를 호출하면 COMPLETED가 되고 실패 사유가 제거된다")
	void complete_setsCompletedAndClearsReason() {
		ProductCounterEventHistory history = history("dedupe-key", ProductCounterProcessStatus.PROCESSING, ProductCounterType.ORDER);
		history.fail("old-reason");

		history.complete();

		assertThat(field(history, "processStatus")).isEqualTo(ProductCounterProcessStatus.COMPLETED);
		assertThat(field(history, "failedReason")).isNull();
		assertThat(field(history, "processedAt")).isNotNull();
	}

	@Test
	@DisplayName("fail을 호출하면 FAILED가 되고 실패 사유/처리 시각이 기록된다")
	void fail_setsFailedAndReason() {
		ProductCounterEventHistory history = history("dedupe-key", ProductCounterProcessStatus.PROCESSING, ProductCounterType.LIKE);

		history.fail("invalid-request");

		assertThat(field(history, "processStatus")).isEqualTo(ProductCounterProcessStatus.FAILED);
		assertThat(field(history, "failedReason")).isEqualTo("invalid-request");
		assertThat(field(history, "processedAt")).isNotNull();
	}

	private ProductCounterEventHistory history(
		final String dedupeKey,
		final ProductCounterProcessStatus processStatus,
		final ProductCounterType counterType
	) {
		return new ProductCounterEventHistory(
			dedupeKey,
			ProductId.of(1L),
			counterType,
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
