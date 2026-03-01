package com.loopers.domain.outbox;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("OutboxEvent 엔티티 테스트")
class OutboxEventTest {

	@DisplayName("create 빌더")
	@Nested
	class Create {

		@Test
		@DisplayName("정상적인 파라미터로 OutboxEvent 를 생성한다")
		void createsOutboxEventSuccessfully() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("order-status-sync:ORD-1001:CONFIRMED")
				.payload("{\"orderId\":\"ORD-1001\"}")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now())
				.lastError(null)
				.build();

			assertThat(event.getEventType()).isEqualTo(OutboxEventType.ORDER_STATUS_SYNC);
			assertThat(event.getAggregateId()).isEqualTo("ORD-1001");
			assertThat(event.getDedupeKey()).isEqualTo("order-status-sync:ORD-1001:CONFIRMED");
			assertThat(event.getPayload()).isEqualTo("{\"orderId\":\"ORD-1001\"}");
			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
			assertThat(event.getRetryCount()).isZero();
		}

		@Test
		@DisplayName("aggregateId 앞뒤 공백을 trim 한다")
		void trimsAggregateId() {
			OutboxEvent event = outboxEvent(" ORD-1001 ", "dedupe-key");

			assertThat(event.getAggregateId()).isEqualTo("ORD-1001");
		}

		@Test
		@DisplayName("dedupeKey 앞뒤 공백을 trim 한다")
		void trimsDedupeKey() {
			OutboxEvent event = outboxEvent("ORD-1001", "  dedupe-key  ");

			assertThat(event.getDedupeKey()).isEqualTo("dedupe-key");
		}

		@Test
		@DisplayName("lastError 가 500자를 초과하면 잘라낸다")
		void trimsLastErrorToMaxLength() {
			String longError = "x".repeat(600);
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now())
				.lastError(longError)
				.build();

			assertThat(event.getLastError()).hasSize(500);
		}
	}

	@DisplayName("markProcessing 메서드")
	@Nested
	class MarkProcessing {

		@Test
		@DisplayName("PENDING 상태에서 PROCESSING 으로 전이한다")
		void transitionsFromPendingToProcessing() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");

			event.markProcessing();

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
			assertThat(event.getLastError()).isNull();
		}

		@Test
		@DisplayName("COMPLETED 상태에서는 상태를 변경하지 않는다")
		void doesNotChangeWhenCompleted() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			event.markCompleted();

			event.markProcessing();

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
		}

		@Test
		@DisplayName("FAILED 상태에서는 상태를 변경하지 않는다")
		void doesNotChangeWhenFailed() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			event.markRetry("에러", LocalDateTime.now(), true);

			event.markProcessing();

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
		}
	}

	@DisplayName("markCompleted 메서드")
	@Nested
	class MarkCompleted {

		@Test
		@DisplayName("상태를 COMPLETED 로 변경하고 lastError 를 null 로 초기화한다")
		void transitionsToCompleted() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			event.markProcessing();

			event.markCompleted();

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
			assertThat(event.getLastError()).isNull();
		}
	}

	@DisplayName("markRetry 메서드")
	@Nested
	class MarkRetry {

		@Test
		@DisplayName("terminalFailure 가 false 면 PENDING 상태로 되돌리고 재시도 횟수를 증가시킨다")
		void retriesToPendingWhenNotTerminal() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			LocalDateTime retryAt = LocalDateTime.now().plusSeconds(10);

			event.markRetry("일시적 오류", retryAt, false);

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
			assertThat(event.getRetryCount()).isEqualTo(1);
			assertThat(event.getLastError()).isEqualTo("일시적 오류");
			assertThat(event.getNextRetryAt()).isEqualTo(retryAt);
		}

		@Test
		@DisplayName("terminalFailure 가 true 면 FAILED 상태로 변경한다")
		void failsWhenTerminal() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			LocalDateTime retryAt = LocalDateTime.now().plusSeconds(10);

			event.markRetry("최종 실패", retryAt, true);

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
			assertThat(event.getRetryCount()).isEqualTo(1);
			assertThat(event.getLastError()).isEqualTo("최종 실패");
		}

		@Test
		@DisplayName("재시도 횟수가 누적된다")
		void accumulatesRetryCount() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			LocalDateTime retryAt = LocalDateTime.now().plusSeconds(10);

			event.markRetry("오류 1", retryAt, false);
			event.markRetry("오류 2", retryAt, false);
			event.markRetry("오류 3", retryAt, false);

			assertThat(event.getRetryCount()).isEqualTo(3);
			assertThat(event.getLastError()).isEqualTo("오류 3");
		}

		@Test
		@DisplayName("lastError 가 500자를 초과하면 잘라낸다")
		void trimsLastErrorOnRetry() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");
			String longError = "e".repeat(600);

			event.markRetry(longError, LocalDateTime.now().plusSeconds(10), false);

			assertThat(event.getLastError()).hasSize(500);
		}
	}

	@DisplayName("guard 메서드")
	@Nested
	class Guard {

		@Test
		@DisplayName("유효한 값이면 예외가 발생하지 않는다")
		void doesNotThrowWhenValid() {
			OutboxEvent event = outboxEvent("ORD-1001", "dedupe-key");

			assertThatCode(event::guard).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("eventType이 null이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenEventTypeNull() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(null)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now())
				.build();

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("aggregateId가 비어있으면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenAggregateIdBlank() {
			OutboxEvent event = outboxEvent(" ", "dedupe-key");

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("dedupeKey가 비어있으면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenDedupeKeyBlank() {
			OutboxEvent event = outboxEvent("ORD-1001", " ");

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("payload가 비어있으면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenPayloadBlank() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload(" ")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now())
				.build();

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("status가 null이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenStatusNull() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(null)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now())
				.build();

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("retryCount가 음수이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenRetryCountNegative() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PENDING)
				.retryCount(-1)
				.nextRetryAt(LocalDateTime.now())
				.build();

			assertCoreException(event::guard);
		}

		@Test
		@DisplayName("nextRetryAt이 null이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenNextRetryAtNull() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(null)
				.build();

			assertCoreException(event::guard);
		}
	}

	private OutboxEvent outboxEvent(String aggregateId, String dedupeKey) {
		return OutboxEvent.create()
			.eventType(OutboxEventType.ORDER_STATUS_SYNC)
			.aggregateId(aggregateId)
			.dedupeKey(dedupeKey)
			.payload("{\"orderId\":\"ORD-1001\"}")
			.status(OutboxStatus.PENDING)
			.retryCount(0)
			.nextRetryAt(LocalDateTime.now())
			.lastError(null)
			.build();
	}

	private void assertCoreException(final ThrowingRunnable runnable) {
		assertThatThrownBy(runnable::run)
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				Object errorType = ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(errorType).isEqualTo(ErrorType.BAD_REQUEST);
			});
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run();
	}
}
