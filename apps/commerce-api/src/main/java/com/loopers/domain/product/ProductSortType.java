package com.loopers.domain.product;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSortType {
	LATEST("latest", Sort.by("createdAt").descending()),
	PRICE_ASC("price_asc", Sort.by("price").ascending()),
	LIKES_DESC("likes_desc", Sort.by("likesCount").descending()),
	ORDERS_DESC("orders_desc", Sort.by("orderCount").descending()),
	VIEWS_DESC("views_desc", Sort.by("viewCount").descending());

	private final String requestValue;
	private final Sort sort;

	private static final Map<String, ProductSortType> REQUEST_VALUE_MAP =
		Arrays.stream(values())
			.collect(Collectors.toMap(
				type -> type.requestValue.toLowerCase(),
				Function.identity()
			));

	public static ProductSortType fromRequestValue(String requestValue) {
		if (requestValue == null || requestValue.trim().isEmpty()) {
			return LATEST;
		}

		return REQUEST_VALUE_MAP.getOrDefault(requestValue.toLowerCase(), LATEST);
	}
}
