package com.loopers.domain.product;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LikeCount implements Serializable {

	@Column(name = "like_count")
	private Long likeCount;

	public LikeCount(Long likeCount) {
		if (likeCount == null || likeCount < 0) {
			throw new CoreException(BAD_REQUEST, PRODUCT_LIKE_COUNT_INVALID.format());
		}
		this.likeCount = likeCount;
	}

	public static LikeCount Zero() {
		return new LikeCount(0L);
	}
}
