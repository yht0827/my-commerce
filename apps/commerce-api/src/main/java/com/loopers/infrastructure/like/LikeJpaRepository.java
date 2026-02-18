package com.loopers.infrastructure.like;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.like.Like;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;

import jakarta.persistence.LockModeType;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

	Optional<Like> findByUserId(final UserId userId);

	List<Like> findAllByUserId(final UserId userId);

	@Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
	Optional<Like> findByUserIdAndProductId(final UserId userId, final ProductId productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
	Optional<Like> findByUserIdAndProductIdWithPessimisticLock(final UserId userId, final ProductId productId);

	@Lock(LockModeType.OPTIMISTIC)
	@Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
	Optional<Like> findByUserIdAndProductIdWithOptimisticLock(final UserId userId, final ProductId productId);
}
