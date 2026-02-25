package com.loopers.domain.payment;

public class PgClientException extends RuntimeException {

	public PgClientException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
