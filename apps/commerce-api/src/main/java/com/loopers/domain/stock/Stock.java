package com.loopers.domain.stock;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

	@Embedded
	private ProductId productId;

	@Embedded
	private Quantity quantity;

	@Builder(builderMethodName = "create")
	public Stock(final ProductId productId, final Quantity quantity) {
		this.productId = productId;
		this.quantity = quantity == null ? new Quantity(0L) : quantity;
	}

	public void deduct(final Quantity amount) {
		this.quantity = this.quantity.subtractWithValidation(amount);
	}

	public void increase(final Quantity amount) {
		this.quantity = this.quantity.add(amount);
	}

	public boolean hasEnough(final Quantity amount) {
		return this.quantity.isSufficient(amount);
	}

	public void changeQuantity(final Quantity quantity) {
		this.quantity = quantity;
	}

	public boolean isOutOfStock() {
		return this.quantity.isOutOfStock();
	}
}
