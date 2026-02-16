package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "brands")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

	@Embedded
	private BrandName brandName;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	public Brand(final BrandName brandName) {
		this(brandName, null, null);
	}

	@Builder
	public Brand(final BrandName brandName, final String description, final String logoUrl) {
		this.brandName = brandName;
		this.description = description;
		this.logoUrl = logoUrl;
	}
}
