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
		productAggregateRepository.createIfNotExists(productId);
	}

}
