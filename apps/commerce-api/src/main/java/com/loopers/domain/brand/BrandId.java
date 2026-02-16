package com.loopers.domain.brand;

import java.io.Serializable;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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
			throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 비어있을 수 없습니다.");
		}
		this.brandId = brandId;
	}

	public static BrandId of(final Long brandId) {
		return new BrandId(brandId);
	}
}
