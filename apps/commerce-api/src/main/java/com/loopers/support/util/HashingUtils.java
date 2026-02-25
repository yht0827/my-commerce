package com.loopers.support.util;

import static com.loopers.support.error.ErrorType.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.loopers.support.error.CoreException;

public final class HashingUtils {

	private HashingUtils() {
	}

	public static String sha256Hex(final String value, final String failureMessage) {
		String normalized = value == null ? "" : value;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new CoreException(INTERNAL_ERROR, failureMessage);
		}
	}
}
