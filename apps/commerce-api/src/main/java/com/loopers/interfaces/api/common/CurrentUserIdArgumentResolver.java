package com.loopers.interfaces.api.common;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String USER_ID_HEADER = "X-USER-ID";

	@Override
	public boolean supportsParameter(final MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserId.class)
			&& String.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer,
		final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
		String userId = webRequest.getHeader(USER_ID_HEADER);
		if (userId == null || userId.isBlank()) {
			throw new CoreException(ErrorType.UNAUTHORIZED, "필수 요청 헤더 'X-USER-ID'가 누락되었습니다.");
		}
		return userId.trim();
	}
}
