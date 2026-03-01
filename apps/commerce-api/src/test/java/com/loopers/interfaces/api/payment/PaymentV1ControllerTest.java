package com.loopers.interfaces.api.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.payment.PaymentApplicationService;
import com.loopers.application.payment.PaymentCommand;
import com.loopers.interfaces.api.common.ApiControllerAdvice;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentV1Controller 테스트")
class PaymentV1ControllerTest {

	@Mock
	private PaymentApplicationService paymentApplicationService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		PaymentV1Controller controller = new PaymentV1Controller(paymentApplicationService, new ObjectMapper());
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new ApiControllerAdvice())
			.build();
	}

	@Test
	@DisplayName("POST /api/v1/payments/callback - 신규 콜백을 접수하고 헤더/본문 값을 커맨드로 전달한다")
	void handleCallbackAcceptsNewCallback() throws Exception {
		String rawBody = "{\"transactionKey\":\"20260101:TR:abc123\",\"orderId\":\"ORD-1001\",\"cardType\":\"SAMSUNG\",\"cardNo\":\"1234-5678-1234-5678\",\"amount\":10000,\"status\":\"SUCCESS\",\"reason\":\"정상 승인\"}";

		when(paymentApplicationService.acceptCallback(any())).thenReturn(true);

		mockMvc.perform(post("/api/v1/payments/callback")
				.header("X-CALLBACK-ID", "callback-1")
				.header("X-CALLBACK-TIMESTAMP", "1739950043")
				.header("X-CALLBACK-SIGNATURE", "sha256=abc123")
				.contentType(APPLICATION_JSON)
				.content(rawBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.transactionKey").value("20260101:TR:abc123"))
			.andExpect(jsonPath("$.data.orderId").value("ORD-1001"))
			.andExpect(jsonPath("$.data.status").value("SUCCESS"))
			.andExpect(jsonPath("$.data.message").value("결제 콜백 접수 완료"));

		ArgumentCaptor<PaymentCommand.ProcessCallback> captor = ArgumentCaptor.forClass(PaymentCommand.ProcessCallback.class);
		verify(paymentApplicationService).acceptCallback(captor.capture());

		PaymentCommand.ProcessCallback command = captor.getValue();
		assertThat(command.transactionKey()).isEqualTo("20260101:TR:abc123");
		assertThat(command.orderId()).isEqualTo("ORD-1001");
		assertThat(command.callbackId()).isEqualTo("callback-1");
		assertThat(command.callbackTimestamp()).isEqualTo("1739950043");
		assertThat(command.callbackSignature()).isEqualTo("sha256=abc123");
		assertThat(command.rawBody()).isEqualTo(rawBody);
	}

	@Test
	@DisplayName("POST /api/v1/payments/callback - 중복 콜백이면 중복 메시지로 응답한다")
	void handleCallbackReturnsDuplicateMessageWhenAlreadyClaimed() throws Exception {
		String rawBody = "{\"transactionKey\":\"20260101:TR:abc123\",\"orderId\":\"ORD-1001\",\"cardType\":\"SAMSUNG\",\"cardNo\":\"1234-5678-1234-5678\",\"amount\":10000,\"status\":\"SUCCESS\",\"reason\":\"정상 승인\"}";

		when(paymentApplicationService.acceptCallback(any())).thenReturn(false);

		mockMvc.perform(post("/api/v1/payments/callback")
				.header("X-CALLBACK-ID", "callback-duplicate")
				.contentType(APPLICATION_JSON)
				.content(rawBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.message").value("중복 콜백 요청입니다."));
	}

	@Test
	@DisplayName("POST /api/v1/payments/callback - 잘못된 JSON 본문이면 400 응답을 반환한다")
	void handleCallbackReturnsBadRequestForInvalidJson() throws Exception {
		mockMvc.perform(post("/api/v1/payments/callback")
				.contentType(APPLICATION_JSON)
				.content("{invalid-json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.meta.result").value("FAIL"));

		verifyNoInteractions(paymentApplicationService);
	}
}
