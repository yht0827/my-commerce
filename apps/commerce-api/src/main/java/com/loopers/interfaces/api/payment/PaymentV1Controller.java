package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller {

	private final PaymentFacade paymentFacade;

	@PostMapping("/callback")
	public ApiResponse<PaymentDto.V1.CallbackResponse> handleCallback(
		@RequestBody final PaymentDto.V1.CallbackRequest request) {
		PaymentCommand.ProcessCallback command = request.toCommand();
		PaymentResult callbackResult = paymentFacade.processCallback(command);
		PaymentDto.V1.CallbackResponse callbackResponse = PaymentDto.V1.CallbackResponse.from(callbackResult);

		return ApiResponse.success(callbackResponse);
	}
}
