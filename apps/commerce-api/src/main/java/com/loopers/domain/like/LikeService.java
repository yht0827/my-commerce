package com.loopers.domain.like;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {
	private static final String LIKE_UNIQUE_CONSTRAINT = "uk_likes_user_product";

	private final LikeRepository likeRepository;

	public LikeInfo likeProduct(final LikeData.LikeProduct data) {

		final UserId userId = UserId.of(data.userId());
		final ProductId productId = ProductId.of(data.productId());

		// 이미 좋아요를 눌렀는지 확인
		if (likeRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
			throw new CoreException(BAD_REQUEST, LIKE_ALREADY_EXISTS.getMessage());
		}

		// 좋아요 정보 저장
		Like productLike = Like.create(userId, productId);
		Like savedLike;
		try {
			savedLike = likeRepository.save(productLike);
		} catch (DataIntegrityViolationException e) {
			throw mapLikeConflictException(e);
		}

		return LikeInfo.from(savedLike);
	}

	public void unlikeProduct(final LikeData.UnlikeProduct data) {
		final UserId userId = UserId.of(data.userId());
		final ProductId productId = ProductId.of(data.productId());

		Like productLike = likeRepository.findByUserIdAndProductId(userId, productId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, LIKE_NOT_FOUND.getMessage()));

		likeRepository.delete(productLike);
	}

	public List<LikeInfo> getLikedProductList(final LikeData.GetLikedProducts data) {
		final UserId userId = UserId.of(data.userId());

		return likeRepository.findAllByUserId(userId)
			.stream()
			.map(LikeInfo::from)
			.toList();
	}

	private RuntimeException mapLikeConflictException(final DataIntegrityViolationException e) {
		if (!(e.getCause() instanceof ConstraintViolationException constraintViolation)) {
			return e;
		}

		String constraintName = constraintViolation.getConstraintName();
		if (LIKE_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
			return new CoreException(BAD_REQUEST, LIKE_ALREADY_EXISTS.getMessage());
		}

		return new CoreException(BAD_REQUEST);
	}
}
