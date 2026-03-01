package com.loopers.application.payment;

import static com.loopers.support.error.ErrorType.*;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.loopers.support.error.CoreException;

@Component
public class PaymentCallbackSignatureVerifier {

	private static final String HMAC_SHA_256 = "HmacSHA256";

	@Value("${payments.callback.signature.enabled:false}")
	private boolean enabled;

	@Value("${payments.callback.signature.secret:}")
	private String secret;

	@Value("${payments.callback.signature.tolerance-seconds:300}")
	private long toleranceSeconds;

	public void verify(final PaymentCommand.ProcessCallback command) {
		if (!enabled) {
			return;
		}

		if (secret == null || secret.isBlank()) {
			throw new CoreException(INTERNAL_ERROR, "콜백 서명 시크릿이 설정되지 않았습니다.");
		}
		if (command.callbackSignature() == null || command.callbackSignature().isBlank()) {
			throw new CoreException(UNAUTHORIZED, "콜백 서명이 누락되었습니다.");
		}
		if (command.callbackTimestamp() == null || command.callbackTimestamp().isBlank()) {
			throw new CoreException(UNAUTHORIZED, "콜백 타임스탬프가 누락되었습니다.");
		}

		long timestamp = parseTimestamp(command.callbackTimestamp());
		long now = Instant.now().getEpochSecond();
		if (Math.abs(now - timestamp) > toleranceSeconds) {
			throw new CoreException(UNAUTHORIZED, "콜백 타임스탬프가 유효 범위를 벗어났습니다.");
		}

		String expected = sign(command.callbackTimestamp(), command.rawBody());
		String provided = normalizeSignature(command.callbackSignature());
		if (!constantTimeEquals(expected, provided)) {
			throw new CoreException(UNAUTHORIZED, "콜백 서명이 유효하지 않습니다.");
		}
	}

	private long parseTimestamp(final String callbackTimestamp) {
		try {
			return Long.parseLong(callbackTimestamp.trim());
		} catch (NumberFormatException e) {
			throw new CoreException(BAD_REQUEST, "콜백 타임스탬프 형식이 올바르지 않습니다.");
		}
	}

	private String sign(final String timestamp, final String rawBody) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA_256);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
			String payload = timestamp + "." + (rawBody == null ? "" : rawBody);
			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return toHex(hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new CoreException(INTERNAL_ERROR, "콜백 서명 검증 중 오류가 발생했습니다.");
		}
	}

	private String normalizeSignature(final String signature) {
		String trimmed = signature.trim();
		if (trimmed.startsWith("sha256=")) {
			return trimmed.substring("sha256=".length());
		}
		return trimmed;
	}

	private boolean constantTimeEquals(final String expected, final String provided) {
		byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
		byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expectedBytes, providedBytes);
	}

	private String toHex(final byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
