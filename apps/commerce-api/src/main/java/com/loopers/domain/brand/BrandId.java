package com.loopers.domain.brand;

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
public class BrandId implements Serializable {
	
	@Column(name = "brand_id")
	private Long brandId;
	
	public BrandId(Long brandId) {
		if (brandId == null || brandId <= 0) {
			throw new CoreException(BAD_REQUEST, BRAND_ID_INVALID.format());
		}
		this.brandId = brandId;
	}

	public static BrandId of(final Long brandId) {
		return new BrandId(brandId);
	}
}
