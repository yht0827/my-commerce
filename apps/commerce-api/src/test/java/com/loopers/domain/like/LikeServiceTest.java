package com.loopers.domain.like;

import static com.loopers.support.error.ErrorMessage.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("좋아요 서비스 단위 테스트")
class LikeServiceTest {

	@Test
	@DisplayName("이미 좋아요한 상품이면 BAD_REQUEST 예외를 반환한다")
	void throwsBadRequestWhenLikeAlreadyExists() {
		LikeRepository likeRepository = mock(LikeRepository.class);
		LikeService likeService = new LikeService(likeRepository);

		Like existingLike = Like.create(UserId.of("yht0827"), ProductId.of(100L));
		when(likeRepository.findByUserIdAndProductId(any(), any())).thenReturn(Optional.of(existingLike));

		CoreException result = assertThrows(
			CoreException.class,
			() -> likeService.likeProduct(new LikeData.LikeProduct("yht0827", 100L))
		);

		assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		assertThat(result.getCustomMessage()).isEqualTo(LIKE_ALREADY_EXISTS.getMessage());
	}

	@Test
	@DisplayName("저장 시 uk_likes_user_product 제약조건 위반이면 BAD_REQUEST로 변환한다")
	void mapsUniqueConstraintViolationToBadRequest() {
		LikeRepository likeRepository = mock(LikeRepository.class);
		LikeService likeService = new LikeService(likeRepository);

		when(likeRepository.findByUserIdAndProductId(any(), any())).thenReturn(Optional.empty());

		ConstraintViolationException duplicateConstraintViolation = new ConstraintViolationException(
			"duplicate key",
			new SQLException("duplicate"),
			"uk_likes_user_product"
		);
		when(likeRepository.save(any(Like.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate key", duplicateConstraintViolation));

		CoreException result = assertThrows(
			CoreException.class,
			() -> likeService.likeProduct(new LikeData.LikeProduct("yht0827", 100L))
		);

		assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		assertThat(result.getCustomMessage()).isEqualTo(LIKE_ALREADY_EXISTS.getMessage());
	}

	@Test
	@DisplayName("좋아요 내역이 없으면 NOT_FOUND 예외를 반환한다")
	void throwsNotFoundWhenUnlikeTargetMissing() {
		LikeRepository likeRepository = mock(LikeRepository.class);
		LikeService likeService = new LikeService(likeRepository);

		when(likeRepository.findByUserIdAndProductId(any(), any())).thenReturn(Optional.empty());

		CoreException result = assertThrows(
			CoreException.class,
			() -> likeService.unlikeProduct(new LikeData.UnlikeProduct("yht0827", 100L))
		);

		assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
		assertThat(result.getCustomMessage()).isEqualTo(LIKE_NOT_FOUND.getMessage());
	}
}
