package com.loopers.domain.common;

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
public class Price implements Serializable {

	@Column(name = "price")
	private Long price;

	public Price(Long price) {
		if (price == null || price < 0) {
			throw new CoreException(BAD_REQUEST, PRICE_INVALID.format());
		}
		this.price = price;
	}
}
