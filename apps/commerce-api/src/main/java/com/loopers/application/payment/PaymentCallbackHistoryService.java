package com.loopers.application.payment;

import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.payment.PaymentCallbackHistory;
import com.loopers.domain.payment.PaymentCallbackHistoryRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.util.HashingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCallbackHistoryService {

	private static final int MAX_DEDUPE_KEY_LENGTH = 128;

	private final PaymentCallbackHistoryRepository paymentCallbackHistoryRepository;

	public String generateDedupeKey(final PaymentCommand.ProcessCallback command) {
		if (command.callbackId() != null && !command.callbackId().isBlank()) {
			String callbackId = command.callbackId().trim();
			if (callbackId.length() <= MAX_DEDUPE_KEY_LENGTH) {
				return callbackId;
			}
			return HashingUtils.sha256Hex(callbackId, "콜백 멱등 키 해시 생성에 실패했습니다.");
		}

		String keySource = String.join("|",
			normalize(command.transactionKey()),
			normalize(command.orderId()),
			normalize(command.status() != null ? command.status().name() : null),
			normalize(command.rawBody())
		);

		return HashingUtils.sha256Hex(keySource, "콜백 멱등 키 해시 생성에 실패했습니다.");
	}

	@Transactional
	public boolean claim(final String dedupeKey, final PaymentCommand.ProcessCallback command) {
		return paymentCallbackHistoryRepository.createIfNotExists(
			dedupeKey,
			command.transactionKey(),
			command.orderId(),
			command.status()
		);
	}

	@Transactional
	public void markProcessing(final String dedupeKey) {
		PaymentCallbackHistory history = findByDedupeKey(dedupeKey);
		history.markProcessing();
	}

	@Transactional
	public void complete(final String dedupeKey) {
		PaymentCallbackHistory history = findByDedupeKey(dedupeKey);
		history.complete();
	}

	@Transactional
	public void fail(final String dedupeKey, final String reason) {
		PaymentCallbackHistory history = findByDedupeKey(dedupeKey);
		history.fail(reason);
	}

	private PaymentCallbackHistory findByDedupeKey(final String dedupeKey) {
		return paymentCallbackHistoryRepository.findByDedupeKey(dedupeKey)
			.orElseThrow(() -> new CoreException(NOT_FOUND, "콜백 이력을 찾을 수 없습니다."));
	}

	private String normalize(final String value) {
		return value == null ? "" : value.trim();
	}
}
