package com.loopers.domain.point;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Service;

import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {
	private final PointRepository pointRepository;
	private final PointHistoryRepository pointHistoryRepository;

	public Point charge(final UserId userId, final Balance amount) {
		Point point = findByUserId(userId);

		point.chargeBalance(amount);
		pointHistoryRepository.save(PointHistory.charge(userId, amount));

		return point;
	}

	public Point findByUserId(final UserId userId) {
		return pointRepository.findByUserId(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, POINT_NOT_FOUND.format(userId.getUserId())));
	}
}
