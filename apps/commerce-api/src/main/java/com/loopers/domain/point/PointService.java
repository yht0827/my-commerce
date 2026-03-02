package com.loopers.domain.point;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.loopers.config.lock.LockProperties;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
	private final PointRepository pointRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final RedissonClient redissonClient;
	private final LockProperties lockProperties;

	public Point charge(final UserId userId, final Balance amount) {
		Point point = findByUserId(userId);
		point.chargeBalance(amount);
		pointHistoryRepository.save(PointHistory.charge(userId, amount));
		return point;
	}

	@Transactional
	public Point use(final UserId userId, final Balance amount) {
		return switch (lockProperties.mode()) {
			case PESSIMISTIC -> useWithPessimistic(userId, amount);
			case OPTIMISTIC -> useWithOptimistic(userId, amount);
			case DISTRIBUTED -> useWithDistributed(userId, amount);
			case DOUBLE_DEFENSE -> useWithDoubleDefense(userId, amount);
		};
	}

	@Transactional
	public Point refund(final UserId userId, final Balance amount) {
		Point point = findByUserId(userId);
		point.chargeBalance(amount);
		pointHistoryRepository.save(PointHistory.charge(userId, amount));
		return point;
	}

	public Point findByUserId(final UserId userId) {
		return pointRepository.findByUserId(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, POINT_NOT_FOUND.format(userId.getUserId())));
	}

	private Point useWithPessimistic(final UserId userId, final Balance amount) {
		Point point = findByUserIdWithPessimisticLock(userId);
		point.useBalance(amount);
		pointHistoryRepository.save(PointHistory.use(userId, amount));
		return point;
	}

	private Point useWithOptimistic(final UserId userId, final Balance amount) {
		int maxRetry = lockProperties.optimisticRetryCount();
		for (int attempt = 0; attempt < maxRetry; attempt++) {
			try {
				Point point = findByUserIdWithOptimisticLock(userId);
				point.useBalance(amount);
				pointHistoryRepository.save(PointHistory.use(userId, amount));
				return point;
			} catch (ObjectOptimisticLockingFailureException e) {
				log.debug("Optimistic lock conflict on point use, attempt={}/{}", attempt + 1, maxRetry);
				if (attempt == maxRetry - 1) {
					throw new CoreException(CONFLICT, POINT_CONFLICT.format(userId.getUserId()));
				}
			}
		}
		throw new CoreException(CONFLICT, POINT_CONFLICT.format(userId.getUserId()));
	}

	private Point useWithDistributed(final UserId userId, final Balance amount) {
		String lockKey = "lock:point:" + userId.getUserId();
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(10, TimeUnit.SECONDS);
		registerUnlockAfterCommit(lock);
		Point point = findByUserId(userId);
		point.useBalance(amount);
		pointHistoryRepository.save(PointHistory.use(userId, amount));
		return point;
	}

	private Point useWithDoubleDefense(final UserId userId, final Balance amount) {
		String lockKey = "lock:point:" + userId.getUserId();
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(10, TimeUnit.SECONDS);
		registerUnlockAfterCommit(lock);
		Point point = findByUserIdWithOptimisticLock(userId);
		point.useBalance(amount);
		pointHistoryRepository.save(PointHistory.use(userId, amount));
		return point;
	}

	private void registerUnlockAfterCommit(final RLock lock) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		});
	}

	private Point findByUserIdWithPessimisticLock(final UserId userId) {
		return pointRepository.findByUserIdWithPessimisticLock(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, POINT_NOT_FOUND.format(userId.getUserId())));
	}

	private Point findByUserIdWithOptimisticLock(final UserId userId) {
		return pointRepository.findByUserIdWithOptimisticLock(userId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, POINT_NOT_FOUND.format(userId.getUserId())));
	}
}
