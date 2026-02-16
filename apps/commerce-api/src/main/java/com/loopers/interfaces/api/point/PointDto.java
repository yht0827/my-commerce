package com.loopers.interfaces.api.point;

import java.math.BigDecimal;

import com.loopers.application.point.ChargePointCommand;
import com.loopers.application.point.PointResult;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PointDto() {

	public record V1() {
		public record ChargePointRequest(
			@NotNull(message = "balance는 필수입니다.")
			@DecimalMin(value = "1000", message = "balance는 1000 이상이어야 합니다.")
			@DecimalMax(value = "1000000", message = "balance는 1000000 이하여야 합니다.")
			BigDecimal balance
		) {
			public ChargePointCommand toCommand(final String userId) {
				return new ChargePointCommand(userId, balance);
			}
		}

		public record BalanceResponse(Long id, String userId, BigDecimal balance) {
			public static BalanceResponse from(final PointResult info) {
				return new BalanceResponse(info.id(), info.userId(), info.balance());
			}
		}
	}
}
