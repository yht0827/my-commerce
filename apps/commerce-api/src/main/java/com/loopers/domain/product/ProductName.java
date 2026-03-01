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
public class ProductName implements Serializable {
	
	@Column(name = "name")
	private String name;
	
	public ProductName(String name) {
		if (name == null || name.isBlank()) {
			throw new CoreException(BAD_REQUEST, PRODUCT_NAME_REQUIRED.format());
		}
		this.name = name;
	}
}
