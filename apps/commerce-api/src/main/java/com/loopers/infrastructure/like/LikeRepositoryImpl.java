package com.loopers.infrastructure.like;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {
	private final LikeJpaRepository likeJpaRepository;

	@Override
	public Like save(final Like like) {
		return likeJpaRepository.save(like);
	}

	@Override
	public void delete(final Like like) {
		likeJpaRepository.delete(like);
	}

	@Override
	public Optional<Like> findByUserId(final UserId userId) {
		return likeJpaRepository.findByUserId(userId);
	}

	@Override
	public List<Like> findAllByUserId(final UserId userId) {
		return likeJpaRepository.findAllByUserId(userId);
	}

	@Override
	public Optional<Like> findByUserIdAndProductId(final UserId userId, final ProductId productId) {
		return likeJpaRepository.findByUserIdAndProductId(userId, productId);
	}

	@Override
	public Optional<Like> findByUserIdAndProductIdWithPessimisticLock(final UserId userId, final ProductId productId) {
		return likeJpaRepository.findByUserIdAndProductIdWithPessimisticLock(userId, productId);
	}

	@Override
	public Optional<Like> findByUserIdAndProductIdWithOptimisticLock(final UserId userId, final ProductId productId) {
		return likeJpaRepository.findByUserIdAndProductIdWithOptimisticLock(userId, productId);
	}

}
