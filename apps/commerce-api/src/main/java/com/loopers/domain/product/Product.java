package com.loopers.domain.product;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

	@Embedded
	private BrandId brandId;

	@Embedded
	private ProductName name;

	@Embedded
	private Price price;

	@Embedded
	private Quantity quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ProductStatus status;

	@Version
	private Long version;

	public Product(final BrandId brandId, final ProductName name, final Price price, final Quantity quantity) {
		this(brandId, name, price, quantity, ProductStatus.ON_SALE);
	}

	@Builder
	public Product(final BrandId brandId, final ProductName name, final Price price, final Quantity quantity,
		final ProductStatus status) {
		this.brandId = brandId;
		this.name = name;
		this.price = price;
		this.quantity = quantity;
		this.status = status == null ? ProductStatus.ON_SALE : status;
	}

	public void deduct(final Quantity amount) {
		if (!status.isOrderable()) {
			throw new CoreException(BAD_REQUEST, PRODUCT_NOT_ORDERABLE.format());
		}
		this.quantity = this.quantity.subtractWithValidation(amount);
		if (this.quantity.isOutOfStock()) {
			this.status = ProductStatus.SOLD_OUT;
		}
	}
}
