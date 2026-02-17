package com.loopers.domain.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAggregateService {

	private final ProductAggregateRepository productAggregateRepository;

	public boolean incrementLikeCount(final ProductId productId) {
		return productAggregateRepository.incrementLikeCount(productId);
	}

	public boolean decrementLikeCount(final ProductId productId) {
		return productAggregateRepository.decrementLikeCount(productId);
	}

	public void createIfNotExists(final ProductId productId) {
		// UPSERT 패턴 또는 존재 여부 체크 후 생성
		if (!productAggregateRepository.existsByProductId(productId)) {
			ProductAggregate productAggregate = ProductAggregate.builder()
				.productId(productId)
				.likeCount(LikeCount.Zero())
				.build();

			productAggregateRepository.save(productAggregate);
		}

	}

}
