package com.loopers.infrastructure.metrics;

import org.springframework.stereotype.Repository;

import com.loopers.domain.metrics.ProductRankDailyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRankDailyRepositoryImpl implements ProductRankDailyRepository {

	private final ProductRankDailyJpaRepository productRankDailyJpaRepository;
}
