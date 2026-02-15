package com.loopers.interfaces.api.point;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.point.ChargePointCommand;
import com.loopers.application.point.GetPointQuery;
import com.loopers.application.point.PointResult;
import com.loopers.application.point.PointUseCase;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

	private final PointUseCase pointUseCase;

	@PostMapping("/charge")
	@Override
	public ApiResponse<PointDto.V1.BalanceResponse> chargePoint(@RequestBody final PointDto.V1.ChargePointRequest request) {
		ChargePointCommand command = request.toCommand();
		PointResult pointResult = pointUseCase.chargePoint(command);
		PointDto.V1.BalanceResponse response = PointDto.V1.BalanceResponse.from(pointResult);
		return ApiResponse.success(response);
	}

	@GetMapping
	@Override
	public ApiResponse<PointDto.V1.BalanceResponse> getPoint(@RequestHeader("X-USER-ID") final String userId) {
		GetPointQuery query = GetPointQuery.of(userId);
		PointResult pointResult = pointUseCase.getPoint(query);
		PointDto.V1.BalanceResponse response = PointDto.V1.BalanceResponse.from(pointResult);
		return ApiResponse.success(response);
	}
}
