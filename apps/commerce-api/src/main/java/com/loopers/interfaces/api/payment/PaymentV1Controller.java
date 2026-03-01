package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentApplicationService;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1ApiSpec {

	private final PaymentApplicationService paymentApplicationService;
	private final ObjectMapper objectMapper;

	@PostMapping("/callback")
	@Override
	public ApiResponse<PaymentDto.V1.CallbackResponse> handleCallback(
		@RequestHeader(value = "X-CALLBACK-ID", required = false) final String callbackId,
		@RequestHeader(value = "X-CALLBACK-TIMESTAMP", required = false) final String callbackTimestamp,
		@RequestHeader(value = "X-CALLBACK-SIGNATURE", required = false) final String callbackSignature,
		@RequestBody final String rawBody) {
		PaymentDto.V1.CallbackRequest request = parseRequest(rawBody);
		PaymentCommand.ProcessCallback command = request.toCommand(
			callbackId,
			callbackTimestamp,
			callbackSignature,
			rawBody
		);

		boolean accepted = paymentApplicationService.acceptCallback(command);
		PaymentDto.V1.CallbackResponse callbackResponse = PaymentDto.V1.CallbackResponse.accepted(request, accepted);

		return ApiResponse.success(callbackResponse);
	}

	private PaymentDto.V1.CallbackRequest parseRequest(final String rawBody) {
		try {
			return objectMapper.readValue(rawBody, PaymentDto.V1.CallbackRequest.class);
		} catch (JsonProcessingException e) {
			throw new CoreException(ErrorType.BAD_REQUEST, "콜백 본문 형식이 올바르지 않습니다.");
		}
	}
}
