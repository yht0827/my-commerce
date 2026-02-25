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
public class ViewCount implements Serializable {

	@Column(name = "view_count")
	private Long viewCount;

	public ViewCount(final Long viewCount) {
		if (viewCount == null || viewCount < 0) {
			throw new CoreException(BAD_REQUEST, PRODUCT_VIEW_COUNT_INVALID.format());
		}
		this.viewCount = viewCount;
	}

	public static ViewCount zero() {
		return new ViewCount(0L);
	}
}
