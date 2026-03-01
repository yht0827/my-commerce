package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.domain.payment.TransactionStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("PaymentCallbackSignatureVerifier 테스트")
class PaymentCallbackSignatureVerifierTest {

	private static final String SECRET = "test-signature-secret";

	private final PaymentCallbackSignatureVerifier verifier = new PaymentCallbackSignatureVerifier();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(verifier, "enabled", true);
		ReflectionTestUtils.setField(verifier, "secret", SECRET);
		ReflectionTestUtils.setField(verifier, "toleranceSeconds", 300L);
	}

	@Test
	@DisplayName("서명 검증이 비활성화되면 필수 값이 없어도 통과한다")
	void verify_skipsWhenDisabled() {
		ReflectionTestUtils.setField(verifier, "enabled", false);

		PaymentCommand.ProcessCallback command = new PaymentCommand.ProcessCallback(
			null,
			TransactionStatus.SUCCESS,
			null,
			null,
			null,
			null,
			null
		);

		assertThatCode(() -> verifier.verify(command)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("시크릿이 비어있으면 INTERNAL_ERROR 예외가 발생한다")
	void verify_throwsWhenSecretIsBlank() {
		ReflectionTestUtils.setField(verifier, "secret", " ");

		PaymentCommand.ProcessCallback command = callbackCommand(
			String.valueOf(Instant.now().getEpochSecond()),
			"sha256=anything",
			"{\"orderId\":\"ORD-1\"}"
		);

		assertCoreException(() -> verifier.verify(command), ErrorType.INTERNAL_ERROR);
	}

	@Test
	@DisplayName("콜백 서명이 비어있으면 UNAUTHORIZED 예외가 발생한다")
	void verify_throwsWhenSignatureMissing() {
		PaymentCommand.ProcessCallback command = callbackCommand(
			String.valueOf(Instant.now().getEpochSecond()),
			" ",
			"{\"orderId\":\"ORD-1\"}"
		);

		assertCoreException(() -> verifier.verify(command), ErrorType.UNAUTHORIZED);
	}

	@Test
	@DisplayName("콜백 타임스탬프가 비어있으면 UNAUTHORIZED 예외가 발생한다")
	void verify_throwsWhenTimestampMissing() {
		PaymentCommand.ProcessCallback command = callbackCommand(
			" ",
			"sha256=anything",
			"{\"orderId\":\"ORD-1\"}"
		);

		assertCoreException(() -> verifier.verify(command), ErrorType.UNAUTHORIZED);
	}

	@Test
	@DisplayName("콜백 타임스탬프 형식이 숫자가 아니면 BAD_REQUEST 예외가 발생한다")
	void verify_throwsWhenTimestampFormatInvalid() {
		PaymentCommand.ProcessCallback command = callbackCommand(
			"not-a-number",
			"sha256=anything",
			"{\"orderId\":\"ORD-1\"}"
		);

		assertCoreException(() -> verifier.verify(command), ErrorType.BAD_REQUEST);
	}

	@Test
	@DisplayName("콜백 타임스탬프가 허용 오차를 벗어나면 UNAUTHORIZED 예외가 발생한다")
	void verify_throwsWhenTimestampOutOfTolerance() {
		String oldTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
		String rawBody = "{\"orderId\":\"ORD-1\"}";
		String signature = "sha256=" + sign(SECRET, oldTimestamp, rawBody);

		PaymentCommand.ProcessCallback command = callbackCommand(oldTimestamp, signature, rawBody);

		assertCoreException(() -> verifier.verify(command), ErrorType.UNAUTHORIZED);
	}

	@Test
	@DisplayName("서명이 일치하지 않으면 UNAUTHORIZED 예외가 발생한다")
	void verify_throwsWhenSignatureInvalid() {
		String timestamp = String.valueOf(Instant.now().getEpochSecond());
		PaymentCommand.ProcessCallback command = callbackCommand(
			timestamp,
			"sha256=invalid-signature",
			"{\"orderId\":\"ORD-1\"}"
		);

		assertCoreException(() -> verifier.verify(command), ErrorType.UNAUTHORIZED);
	}

	@Test
	@DisplayName("유효한 sha256 prefix 서명이면 검증을 통과한다")
	void verify_passesWhenSignatureValidWithPrefix() {
		String timestamp = String.valueOf(Instant.now().getEpochSecond());
		String rawBody = "{\"orderId\":\"ORD-1\",\"status\":\"SUCCESS\"}";
		String signature = "sha256=" + sign(SECRET, timestamp, rawBody);

		PaymentCommand.ProcessCallback command = callbackCommand(timestamp, signature, rawBody);

		assertThatCode(() -> verifier.verify(command)).doesNotThrowAnyException();
	}

	private PaymentCommand.ProcessCallback callbackCommand(
		final String callbackTimestamp,
		final String callbackSignature,
		final String rawBody
	) {
		return new PaymentCommand.ProcessCallback(
			"TR-1",
			TransactionStatus.SUCCESS,
			"ORD-1",
			"cb-1",
			callbackTimestamp,
			callbackSignature,
			rawBody
		);
	}

	private String sign(final String secret, final String timestamp, final String rawBody) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			String payload = timestamp + "." + (rawBody == null ? "" : rawBody);
			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void assertCoreException(final ThrowableAssert.ThrowingCallable callable, final ErrorType errorType) {
		assertThatThrownBy(callable)
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				ErrorType actual = (ErrorType) ReflectionTestUtils.getField(throwable, "errorType");
				assertThat(actual).isEqualTo(errorType);
			});
	}
}
