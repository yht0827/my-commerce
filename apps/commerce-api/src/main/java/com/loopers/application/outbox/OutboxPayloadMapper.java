package com.loopers.application.outbox;

import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPayloadMapper {

	private final ObjectMapper objectMapper;

	public String write(final Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new CoreException(INTERNAL_ERROR, "아웃박스 payload 직렬화에 실패했습니다.");
		}
	}

	public <T> T read(final String payload, final Class<T> targetType) {
		try {
			return objectMapper.readValue(payload, targetType);
		} catch (JsonProcessingException e) {
			throw new CoreException(INTERNAL_ERROR, "아웃박스 payload 역직렬화에 실패했습니다.");
		}
	}
}
