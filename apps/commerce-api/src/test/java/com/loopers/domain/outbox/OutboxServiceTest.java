package com.loopers.domain.outbox;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OutboxService 테스트")
class OutboxServiceTest {

	private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
	private final OutboxService outboxService = new OutboxService(outboxEventRepository);

	@DisplayName("enqueue 메서드")
	@Nested
	class Enqueue {

		@Test
		@DisplayName("이벤트를 생성 위임하고 결과를 반환한다")
		void delegatesToRepository() {
			when(outboxEventRepository.createIfNotExists(
				eq(OutboxEventType.ORDER_STATUS_SYNC),
				eq("ORD-1001"),
				eq("dedupe-key"),
				eq("{}")
			)).thenReturn(true);

			boolean result = outboxService.enqueue(
				OutboxEventType.ORDER_STATUS_SYNC, "ORD-1001", "dedupe-key", "{}"
			);

			assertThat(result).isTrue();
			verify(outboxEventRepository).createIfNotExists(
				OutboxEventType.ORDER_STATUS_SYNC, "ORD-1001", "dedupe-key", "{}"
			);
		}

		@Test
		@DisplayName("중복 dedupeKey 면 false 를 반환한다")
		void returnsFalseWhenDuplicate() {
			when(outboxEventRepository.createIfNotExists(any(), anyString(), anyString(), anyString()))
				.thenReturn(false);

			boolean result = outboxService.enqueue(
				OutboxEventType.ORDER_STATUS_SYNC, "ORD-1001", "dup-key", "{}"
			);

			assertThat(result).isFalse();
		}
	}

	@DisplayName("findPendingEventIds 메서드")
	@Nested
	class FindPendingEventIds {

		@Test
		@DisplayName("대기 중인 이벤트 ID 목록을 반환한다")
		void returnsPendingEventIds() {
			when(outboxEventRepository.findPendingEventIds(any(LocalDateTime.class), eq(10)))
				.thenReturn(List.of(1L, 2L, 3L));

			List<Long> result = outboxService.findPendingEventIds(10);

			assertThat(result).containsExactly(1L, 2L, 3L);
		}

		@Test
		@DisplayName("대기 중인 이벤트가 없으면 빈 목록을 반환한다")
		void returnsEmptyWhenNoPending() {
			when(outboxEventRepository.findPendingEventIds(any(LocalDateTime.class), eq(10)))
				.thenReturn(List.of());

			List<Long> result = outboxService.findPendingEventIds(10);

			assertThat(result).isEmpty();
		}
	}

	@DisplayName("claim 메서드")
	@Nested
	class Claim {

		@Test
		@DisplayName("이벤트가 존재하지 않으면 빈 Optional 을 반환한다")
		void returnsEmptyWhenNotFound() {
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.empty());

			Optional<OutboxEvent> result = outboxService.claim(1L);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("이벤트 상태가 PENDING 이 아니면 빈 Optional 을 반환한다")
		void returnsEmptyWhenNotPending() {
			OutboxEvent event = outboxEvent(OutboxStatus.PROCESSING);
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			Optional<OutboxEvent> result = outboxService.claim(1L);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("nextRetryAt 이 미래이면 빈 Optional 을 반환한다")
		void returnsEmptyWhenNextRetryAtIsFuture() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.nextRetryAt(LocalDateTime.now().plusHours(1))
				.lastError(null)
				.build();
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			Optional<OutboxEvent> result = outboxService.claim(1L);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("PENDING 이고 nextRetryAt 이 과거이면 PROCESSING 으로 전이하고 반환한다")
		void claimsSuccessfully() {
			OutboxEvent event = outboxEvent(OutboxStatus.PENDING);
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			Optional<OutboxEvent> result = outboxService.claim(1L);

			assertThat(result).isPresent();
			assertThat(result.get().getStatus()).isEqualTo(OutboxStatus.PROCESSING);
		}
	}

	@DisplayName("complete 메서드")
	@Nested
	class Complete {

		@Test
		@DisplayName("이벤트를 COMPLETED 상태로 변경한다")
		void completesEvent() {
			OutboxEvent event = outboxEvent(OutboxStatus.PROCESSING);
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			outboxService.complete(1L);

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
		}

		@Test
		@DisplayName("이벤트가 존재하지 않으면 아무 동작도 하지 않는다")
		void doesNothingWhenNotFound() {
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.empty());

			outboxService.complete(1L);

			verify(outboxEventRepository).findById(1L);
		}
	}

	@DisplayName("retry 메서드")
	@Nested
	class Retry {

		@Test
		@DisplayName("재시도 횟수가 MAX_RETRY_COUNT 미만이면 PENDING 으로 되돌린다")
		void retriesToPendingWhenBelowMax() {
			OutboxEvent event = outboxEvent(OutboxStatus.PROCESSING);
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			outboxService.retry(1L, "일시적 오류");

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
			assertThat(event.getRetryCount()).isEqualTo(1);
			assertThat(event.getLastError()).isEqualTo("일시적 오류");
		}

		@Test
		@DisplayName("재시도 횟수가 MAX_RETRY_COUNT 에 도달하면 FAILED 로 변경한다")
		void failsWhenMaxRetryCountReached() {
			OutboxEvent event = OutboxEvent.create()
				.eventType(OutboxEventType.ORDER_STATUS_SYNC)
				.aggregateId("ORD-1001")
				.dedupeKey("dedupe-key")
				.payload("{}")
				.status(OutboxStatus.PROCESSING)
				.retryCount(9)
				.nextRetryAt(LocalDateTime.now().minusSeconds(1))
				.lastError(null)
				.build();
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

			outboxService.retry(1L, "최종 실패");

			assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
			assertThat(event.getRetryCount()).isEqualTo(10);
		}

		@Test
		@DisplayName("이벤트가 존재하지 않으면 아무 동작도 하지 않는다")
		void doesNothingWhenNotFound() {
			when(outboxEventRepository.findById(1L)).thenReturn(Optional.empty());

			outboxService.retry(1L, "오류");

			verify(outboxEventRepository).findById(1L);
		}
	}

	private OutboxEvent outboxEvent(OutboxStatus status) {
		return OutboxEvent.create()
			.eventType(OutboxEventType.ORDER_STATUS_SYNC)
			.aggregateId("ORD-1001")
			.dedupeKey("dedupe-key")
			.payload("{}")
			.status(status)
			.retryCount(0)
			.nextRetryAt(LocalDateTime.now().minusSeconds(1))
			.lastError(null)
			.build();
	}
}
