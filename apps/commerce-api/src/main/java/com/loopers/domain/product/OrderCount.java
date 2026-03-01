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
public class OrderCount implements Serializable {

	@Column(name = "order_count")
	private Long orderCount;

	public OrderCount(final Long orderCount) {
		if (orderCount == null || orderCount < 0) {
			throw new CoreException(BAD_REQUEST, PRODUCT_ORDER_COUNT_INVALID.format());
		}
		this.orderCount = orderCount;
	}

	public static OrderCount zero() {
		return new OrderCount(0L);
	}
}
