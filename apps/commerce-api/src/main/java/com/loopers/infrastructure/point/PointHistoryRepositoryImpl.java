package com.loopers.infrastructure.point;

import org.springframework.stereotype.Repository;

import com.loopers.domain.point.PointHistory;
import com.loopers.domain.point.PointHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

	private final PointHistoryJpaRepository pointHistoryJpaRepository;

	@Override
	public PointHistory save(final PointHistory pointHistory) {
		return pointHistoryJpaRepository.save(pointHistory);
	}
}
