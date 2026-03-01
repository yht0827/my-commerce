package com.loopers.application.product;

import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.product.ProductCounterEventHistory;
import com.loopers.domain.product.ProductCounterEventHistoryRepository;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;
import com.loopers.support.error.CoreException;
import com.loopers.support.util.HashingUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCounterEventHistoryService {

	private final ProductCounterEventHistoryRepository productCounterEventHistoryRepository;

	public String generateDedupeKey(final String keySource) {
		String normalized = keySource == null ? "" : keySource.trim();
		return HashingUtils.sha256Hex(normalized, "카운터 멱등 키 해시 생성에 실패했습니다.");
	}

	@Transactional
	public boolean claim(final String dedupeKey, final ProductId productId, final ProductCounterType counterType) {
		return productCounterEventHistoryRepository.createIfNotExists(dedupeKey, productId, counterType);
	}

	@Transactional
	public void markProcessing(final String dedupeKey) {
		ProductCounterEventHistory history = findByDedupeKey(dedupeKey);
		history.markProcessing();
	}

	@Transactional
	public void complete(final String dedupeKey) {
		ProductCounterEventHistory history = findByDedupeKey(dedupeKey);
		history.complete();
	}

	@Transactional
	public void fail(final String dedupeKey, final String reason) {
		ProductCounterEventHistory history = findByDedupeKey(dedupeKey);
		history.fail(reason);
	}

	private ProductCounterEventHistory findByDedupeKey(final String dedupeKey) {
		return productCounterEventHistoryRepository.findByDedupeKey(dedupeKey)
			.orElseThrow(() -> new CoreException(NOT_FOUND, "카운터 이벤트 이력을 찾을 수 없습니다."));
	}

}
