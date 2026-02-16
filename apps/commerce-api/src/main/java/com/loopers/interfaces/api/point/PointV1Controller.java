package com.loopers.interfaces.api.point;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.point.ChargePointCommand;
import com.loopers.application.point.GetPointQuery;
import com.loopers.application.point.PointApplicationService;
import com.loopers.application.point.PointResult;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.interfaces.api.common.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

	private final PointApplicationService pointApplicationService;

	@PostMapping("/charge")
	@Override
	public ApiResponse<PointDto.V1.BalanceResponse> chargePoint(
		@CurrentUserId final String userId,
		@Valid @RequestBody final PointDto.V1.ChargePointRequest request
	) {
		ChargePointCommand command = request.toCommand(userId);
		PointResult pointResult = pointApplicationService.chargePoint(command);
		PointDto.V1.BalanceResponse response = PointDto.V1.BalanceResponse.from(pointResult);
		return ApiResponse.success(response);
	}

	@GetMapping
	@Override
	public ApiResponse<PointDto.V1.BalanceResponse> getPoint(@CurrentUserId final String userId) {
		GetPointQuery query = GetPointQuery.of(userId);
		PointResult pointResult = pointApplicationService.getPoint(query);
		PointDto.V1.BalanceResponse response = PointDto.V1.BalanceResponse.from(pointResult);
		return ApiResponse.success(response);
	}
}
