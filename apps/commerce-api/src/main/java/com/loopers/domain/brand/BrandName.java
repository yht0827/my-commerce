package com.loopers.domain.brand;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandName implements Serializable {
	
	@Column(name = "name")
	private String brandName;
	
	public BrandName(String brandName) {
		if (brandName == null || brandName.isBlank()) {
			throw new CoreException(BAD_REQUEST, BRAND_NAME_REQUIRED.format());
		}
		this.brandName = brandName;
	}
}
