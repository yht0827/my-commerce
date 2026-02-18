package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;

public interface LikeRepository {

	Like save(final Like like);

	void delete(final Like like);

	Optional<Like> findByUserId(final UserId userId);

	List<Like> findAllByUserId(final UserId userId);

	Optional<Like> findByUserIdAndProductId(final UserId userId, final ProductId productId);

	Optional<Like> findByUserIdAndProductIdWithPessimisticLock(final UserId userId, final ProductId productId);

	Optional<Like> findByUserIdAndProductIdWithOptimisticLock(final UserId userId, final ProductId productId);

}
