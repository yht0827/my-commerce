package com.loopers.interfaces.api.point;

import java.math.BigDecimal;

import com.loopers.application.point.ChargePointCommand;
import com.loopers.application.point.PointResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PointDto() {

	public record V1() {
		public record ChargePointRequest(
			@Schema(description = "충전 금액", example = "50000")
			@NotNull(message = "balance는 필수입니다.")
			@DecimalMin(value = "1000", message = "balance는 1000 이상이어야 합니다.")
			@DecimalMax(value = "1000000", message = "balance는 1000000 이하여야 합니다.")
			BigDecimal balance
		) {
			public ChargePointCommand toCommand(final String userId) {
				return new ChargePointCommand(userId, balance);
			}
		}

		public record PointBalanceResponse(
			@Schema(description = "포인트 PK", example = "1")
			Long id,
			@Schema(description = "회원 아이디", example = "loopers01")
			String userId,
			@Schema(description = "현재 포인트 잔액", example = "150000")
			BigDecimal balance
		) {
			public static PointBalanceResponse from(final PointResult info) {
				return new PointBalanceResponse(info.id(), info.userId(), info.balance());
			}
		}
	}
}
