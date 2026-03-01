package com.loopers.domain.outbox;

public enum OutboxStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED
}
