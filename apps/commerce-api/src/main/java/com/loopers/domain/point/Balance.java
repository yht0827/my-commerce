package com.loopers.domain.point;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;
import java.math.BigDecimal;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Balance implements Serializable {

	@Column(name = "balance")
	private BigDecimal balance;
	
	public Balance(BigDecimal balance) {
		if (balance == null) {
			throw new CoreException(BAD_REQUEST, POINT_BALANCE_REQUIRED.getMessage());
		}
		if (balance.compareTo(BigDecimal.ZERO) < 0) {
			throw new CoreException(BAD_REQUEST, POINT_BALANCE_INVALID.format(0));
		}
		this.balance = balance;
	}

	public Balance charge(final Balance amount) {
		return new Balance(this.balance.add(amount.getBalance()));
	}

	public Balance use(final Balance amount) {
		if (this.balance.compareTo(amount.getBalance()) < 0) {
			throw new CoreException(BAD_REQUEST, POINT_INSUFFICIENT.format());
		}
		return new Balance(this.balance.subtract(amount.getBalance()));
	}

	public static Balance of(final BigDecimal amount) {
		return new Balance(amount);
	}
}
