package com.loopers.interfaces.api.common;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("ApiControllerAdvice 테스트")
class ApiControllerAdviceTest {

	private final ApiControllerAdvice advice = new ApiControllerAdvice();

	@Test
	@DisplayName("CoreException은 에러 타입과 커스텀 메시지로 응답한다")
	void handle_coreExceptionReturnsExpectedFailure() {
		CoreException exception = new CoreException(ErrorType.BAD_REQUEST, "invalid-input");

		ResponseEntity<ApiResponse<?>> response = advice.handle(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "invalid-input");
	}

	@Test
	@DisplayName("CoreException의 커스텀 메시지가 없으면 기본 메시지를 사용한다")
	void handle_coreExceptionWithoutCustomMessageReturnsDefaultMessage() {
		CoreException exception = new CoreException(ErrorType.BAD_REQUEST);

		ResponseEntity<ApiResponse<?>> response = advice.handle(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, null);
	}

	@Test
	@DisplayName("타입 불일치 예외는 파라미터 정보를 포함한 BAD_REQUEST를 반환한다")
	void handleBadRequest_typeMismatchReturnsBadRequest() {
		MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
			"abc",
			Long.class,
			"page",
			methodParameter(),
			null
		);

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "요청 파라미터 'page'");
	}

	@Test
	@DisplayName("타입/값 정보가 없는 타입 불일치 예외도 BAD_REQUEST를 반환한다")
	void handleBadRequest_typeMismatchWithoutTypeAndValueReturnsBadRequest() {
		MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
			null,
			null,
			"page",
			methodParameter(),
			null
		);

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "타입: unknown");
	}

	@Test
	@DisplayName("필수 요청 파라미터 누락 예외는 BAD_REQUEST를 반환한다")
	void handleBadRequest_missingRequestParamReturnsBadRequest() {
		MissingServletRequestParameterException exception =
			new MissingServletRequestParameterException("brandId", "Long");

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필수 요청 파라미터 'brandId'");
	}

	@Test
	@DisplayName("검증 실패 예외는 첫 번째 필드 에러 메시지를 반환한다")
	void handleBadRequest_validationExceptionReturnsFirstFieldMessage() {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "name", "must not be blank"));
		bindingResult.addError(new FieldError("request", "email", "must be valid"));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필드 'name': must not be blank");
	}

	@Test
	@DisplayName("InvalidFormatException 루트 원인은 enum 가용값 안내 메시지를 반환한다")
	void handleBadRequest_invalidFormatRootCauseReturnsDetailedMessage() {
		InvalidFormatException invalidFormat = InvalidFormatException.from(
			(JsonParser) null,
			"invalid format",
			"UNKNOWN",
			SampleStatus.class
		);
		invalidFormat.prependPath(new Object(), "status");

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", invalidFormat, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "사용 가능한 값");
	}

	@Test
	@DisplayName("InvalidFormatException 대상이 enum이 아니면 enum 안내 문구 없이 메시지를 반환한다")
	void handleBadRequest_invalidFormatNonEnumReturnsDetailedMessageWithoutEnumHint() {
		InvalidFormatException invalidFormat = InvalidFormatException.from(
			(JsonParser) null,
			"invalid format",
			"abc",
			Integer.class
		);
		invalidFormat.prependPath(new Object(), "amount");

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", invalidFormat, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필드 'amount'의 값 'abc'");
	}

	@Test
	@DisplayName("MismatchedInputException 루트 원인은 누락 필드 메시지를 반환한다")
	void handleBadRequest_mismatchedInputRootCauseReturnsMissingFieldMessage() {
		MismatchedInputException mismatchedInput = MismatchedInputException.from(
			(JsonParser) null,
			String.class,
			"missing"
		);
		mismatchedInput.prependPath(new Object(), "orderId");

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", mismatchedInput, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필수 필드 'orderId'");
	}

	@Test
	@DisplayName("MismatchedInputException 경로가 인덱스인 경우 '?' 필드 표시를 사용한다")
	void handleBadRequest_mismatchedInputWithIndexPathUsesQuestionMarkField() {
		MismatchedInputException mismatchedInput = MismatchedInputException.from(
			(JsonParser) null,
			String.class,
			"missing"
		);
		mismatchedInput.prependPath(new Object(), 0);

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", mismatchedInput, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필수 필드 '?'이(가) 누락");
	}

	@Test
	@DisplayName("JsonMappingException 루트 원인은 매핑 오류 메시지를 반환한다")
	void handleBadRequest_jsonMappingRootCauseReturnsMappingErrorMessage() {
		JsonMappingException jsonMappingException = JsonMappingException.from((JsonParser) null, "mapping failed");
		jsonMappingException.prependPath(new Object(), "items");

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", jsonMappingException, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "JSON 매핑 오류");
	}

	@Test
	@DisplayName("JsonMappingException 경로가 인덱스인 경우 '?' 필드 표시를 사용한다")
	void handleBadRequest_jsonMappingWithIndexPathUsesQuestionMarkField() {
		JsonMappingException jsonMappingException = JsonMappingException.from((JsonParser) null, "mapping failed");
		jsonMappingException.prependPath(new Object(), 1);

		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", jsonMappingException, mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필드 '?'에서 JSON 매핑 오류");
	}

	@Test
	@DisplayName("알 수 없는 루트 원인은 기본 JSON 오류 메시지를 반환한다")
	void handleBadRequest_unknownRootCauseReturnsDefaultJsonMessage() {
		HttpMessageNotReadableException exception =
			new HttpMessageNotReadableException("invalid body", new RuntimeException("boom"), mock(HttpInputMessage.class));

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "JSON 메세지 규격");
	}

	@Test
	@DisplayName("ServerWebInputException reason에 파라미터명이 있으면 해당 이름으로 BAD_REQUEST를 반환한다")
	void handleBadRequest_serverWebInputWithMissingParamReturnsBadRequest() {
		ServerWebInputException exception = new ServerWebInputException("Required value 'userId' is missing");

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필수 요청 값 'userId'");
	}

	@Test
	@DisplayName("ServerWebInputException reason에서 파라미터명을 추출하지 못하면 기본 BAD_REQUEST 메시지를 반환한다")
	void handleBadRequest_serverWebInputWithoutMissingParamReturnsDefaultMessage() {
		ServerWebInputException exception = new ServerWebInputException("Malformed payload");

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, null);
	}

	@Test
	@DisplayName("ServerWebInputException reason이 null이어도 기본 BAD_REQUEST 메시지를 반환한다")
	void handleBadRequest_serverWebInputWithNullReasonReturnsDefaultMessage() {
		ServerWebInputException exception = new ServerWebInputException(null);

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, null);
	}

	@Test
	@DisplayName("X-USER-ID 헤더 누락은 UNAUTHORIZED를 반환한다")
	void handleBadRequest_missingUserHeaderReturnsUnauthorized() {
		MissingRequestHeaderException exception = missingHeaderException("X-USER-ID");

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.UNAUTHORIZED, ErrorType.UNAUTHORIZED, "필수 요청 헤더 'X-USER-ID'");
	}

	@Test
	@DisplayName("일반 헤더 누락은 BAD_REQUEST를 반환한다")
	void handleBadRequest_missingGeneralHeaderReturnsBadRequest() {
		MissingRequestHeaderException exception = missingHeaderException("X-TRACE-ID");

		ResponseEntity<ApiResponse<?>> response = advice.handleBadRequest(exception);

		assertFailure(response, HttpStatus.BAD_REQUEST, ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-TRACE-ID'");
	}

	@Test
	@DisplayName("리소스를 찾지 못하면 NOT_FOUND를 반환한다")
	void handleNotFound_returnsNotFound() {
		NoResourceFoundException exception = noResourceFoundException();

		ResponseEntity<ApiResponse<?>> response = advice.handleNotFound(exception);

		assertFailure(response, HttpStatus.NOT_FOUND, ErrorType.NOT_FOUND, null);
	}

	@Test
	@DisplayName("처리되지 않은 예외는 INTERNAL_ERROR를 반환한다")
	void handle_unknownExceptionReturnsInternalError() {
		ResponseEntity<ApiResponse<?>> response = advice.handle(new RuntimeException("unexpected"));

		assertFailure(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorType.INTERNAL_ERROR, null);
	}

	private void assertFailure(
		final ResponseEntity<ApiResponse<?>> response,
		final HttpStatus expectedStatus,
		final ErrorType expectedErrorType,
		final String expectedMessageContains
	) {
		assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
		assertThat(response.getBody()).isNotNull();

		ApiResponse.Metadata meta = response.getBody().meta();
		assertThat(meta.result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
		String expectedErrorCode = (String) ReflectionTestUtils.getField(expectedErrorType, "code");
		assertThat(meta.errorCode()).isEqualTo(expectedErrorCode);

		if (expectedMessageContains == null) {
			String expectedErrorMessage = (String) ReflectionTestUtils.getField(expectedErrorType, "message");
			assertThat(meta.message()).isEqualTo(expectedErrorMessage);
			return;
		}

		assertThat(meta.message()).contains(expectedMessageContains);
	}

	private MethodParameter methodParameter() {
		try {
			Method method = ApiControllerAdviceTest.class.getDeclaredMethod("dummyMethod", String.class);
			return new MethodParameter(method, 0);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	private MissingRequestHeaderException missingHeaderException(final String headerName) {
		try {
			Constructor<MissingRequestHeaderException> constructor =
				MissingRequestHeaderException.class.getConstructor(String.class, MethodParameter.class);
			return constructor.newInstance(headerName, methodParameter());
		} catch (NoSuchMethodException ex) {
			try {
				Constructor<MissingRequestHeaderException> constructor =
					MissingRequestHeaderException.class.getConstructor(String.class, MethodParameter.class, boolean.class);
				return constructor.newInstance(headerName, methodParameter(), false);
			} catch (Exception nested) {
				throw new IllegalStateException(nested);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private NoResourceFoundException noResourceFoundException() {
		try {
			Constructor<NoResourceFoundException> constructor =
				NoResourceFoundException.class.getConstructor(HttpMethod.class, String.class);
			return constructor.newInstance(HttpMethod.GET, "/not-found");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unused")
	private void dummyMethod(final String value) {
	}

	private enum SampleStatus {
		READY,
		DONE
	}
}
