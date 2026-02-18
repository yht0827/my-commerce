package com.loopers.domain.like;

import java.util.List;

import org.springframework.stereotype.Service;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {

	private final LikeRepository likeRepository;

	public LikeInfo likeProduct(final LikeData.LikeProduct data) {

		final UserId userId = UserId.of(data.userId());
		final ProductId productId = ProductId.of(data.productId());

		// 이미 좋아요를 눌렀는지 확인
		if (likeRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이미 좋아요를 누른 상품입니다.");
		}

		// 좋아요 정보 저장
		Like productLike = Like.create(userId, productId);
		Like savedLike = likeRepository.save(productLike);

		return LikeInfo.from(savedLike);
	}

	public void unlikeProduct(final LikeData.UnlikeProduct data) {
		final UserId userId = UserId.of(data.userId());
		final ProductId productId = ProductId.of(data.productId());

		Like productLike = likeRepository.findByUserIdAndProductId(userId, productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "좋아요 정보를 찾을 수 없습니다."));

		likeRepository.delete(productLike);
	}

	public List<LikeInfo> getLikedProductList(final LikeData.GetLikedProducts data) {
		final UserId userId = UserId.of(data.userId());

		return likeRepository.findAllByUserId(userId)
			.stream()
			.map(LikeInfo::from)
			.toList();
	}
}
