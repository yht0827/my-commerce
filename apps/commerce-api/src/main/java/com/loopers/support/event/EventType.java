package com.loopers.support.event;

import lombok.Getter;

@Getter
public enum EventType {
	ORDER_CREATED("ORDER_CREATED"),
	PAYMENT_COMPLETED("PAYMENT_COMPLETED"),
	PAYMENT_FAILED("PAYMENT_FAILED"),
	PRODUCT_LIKED("PRODUCT_LIKED"),
	PRODUCT_UNLIKED("PRODUCT_UNLIKED"),
	DATA_PLATFORM("DATA_PLATFORM");

	private final String className;

	EventType(String className) {
		this.className = className;
	}
}
