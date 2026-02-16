package com.loopers.application.point;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.point.Balance;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PointApplicationService {
	private final PointService pointService;

	public PointResult chargePoint(final ChargePointCommand command) {

		Point point = pointService.charge(
			UserId.of(command.userId()),
			Balance.of(command.balance())
		);

		return PointResult.from(point);
	}

	@Transactional(readOnly = true)
	public PointResult getPoint(final GetPointQuery query) {
		final UserId userId = UserId.of(query.userId());

		Point point = pointService.findByUserId(userId);
		return PointResult.from(point);
	}
}
