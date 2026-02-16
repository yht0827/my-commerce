package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Points V1 API", description = "Points API 입니다.")
public interface PointV1ApiSpec {

	@Operation(
		summary = "포인트 충전",
		description = "사용자의 포인트를 충전합니다."
	)
	ApiResponse<PointDto.V1.BalanceResponse> chargePoint(final String userId, final PointDto.V1.ChargePointRequest pointRequest);

	@Operation(
		summary = "포인트 조회",
		description = "사용자의 포인트를 조회합니다."
	)
	ApiResponse<PointDto.V1.BalanceResponse> getPoint(final String userId);
}
