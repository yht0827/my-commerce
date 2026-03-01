package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payments V1 API", description = "Payments API 입니다.")
public interface PaymentV1ApiSpec {

	@Operation(
		summary = "결제 콜백 처리",
		description = "PG 콜백을 검증하고 비동기로 처리합니다."
	)
	ApiResponse<PaymentDto.V1.CallbackResponse> handleCallback(
		final String callbackId,
		final String callbackTimestamp,
		final String callbackSignature,
		final String rawBody
	);
}
