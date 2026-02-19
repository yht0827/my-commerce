package com.loopers.support.error;

import lombok.Getter;

@Getter
public enum ErrorMessage {

	USER_ID_REQUIRED("사용자 ID는 필수입니다."),
	USER_NAME_REQUIRED("사용자 이름은 필수입니다."),
	USER_EMAIL_REQUIRED("사용자 이메일은 필수입니다."),
	USER_ID_ALREADY_EXISTS("이미 사용중인 ID입니다."),
	EMAIL_ALREADY_EXISTS("이미 사용중인 이메일입니다"),
	USER_NOT_FOUND("해당 사용자 ID '%s'의 회원을 찾을 수 없습니다."),
	BIRTHDAY_INVALID_FORMAT("생년월일은 yyyy-MM-dd 형식이어야 합니다."),
	BIRTHDAY_CANNOT_BE_FUTURE("생년월일은 미래일 수 없습니다."),
	USER_ID_INVALID_FORMAT("사용자 ID는 영문/숫자 조합이며 %d자 이상 %d자 이하여야 합니다."),
	USER_NAME_INVALID_FORMAT("사용자 이름은 한글, 영문만 가능하며 %d자 이내여야 합니다."),
	EMAIL_INVALID_FORMAT("유효하지 않은 이메일 형식입니다."),
	GENDER_INVALID("유효하지 않은 성별입니다."),
	BRAND_ID_INVALID("브랜드 ID는 비어있을 수 없습니다."),
	BRAND_NAME_REQUIRED("브랜드 이름은 비어있을 수 없습니다."),
	BRAND_NOT_FOUND("해당 [id = %s]의 브랜드를 찾을 수 없습니다."),
	PRICE_INVALID("가격은 0 이상이어야 합니다."),
	QUANTITY_INVALID("수량은 0 이상이어야 합니다."),
	INSUFFICIENT_STOCK("재고가 충분하지 않습니다."),
	PRODUCT_ID_INVALID("제품 ID는 비어있을 수 없습니다."),
	PRODUCT_NAME_REQUIRED("상품 이름은 비어있을 수 없습니다."),
	PRODUCT_LIKE_COUNT_INVALID("좋아요 수는 0 이상이어야 합니다."),
	PRODUCT_NOT_ORDERABLE("판매 중인 상품만 주문할 수 있습니다."),
	COUPON_NAME_REQUIRED("쿠폰 이름은 비어있을 수 없습니다."),
	COUPON_DISCOUNT_VALUE_REQUIRED("할인 값이 설정되지 않았습니다."),
	COUPON_DISCOUNT_VALUE_INVALID("할인 금액은 0 이상이어야 합니다."),
	COUPON_PERCENTAGE_DISCOUNT_VALUE_INVALID("정률 쿠폰 할인율은 0 이상 100 이하여야 합니다."),
	COUPON_MAX_DISCOUNT_AMOUNT_INVALID("최대 할인 금액은 0 이상이어야 합니다."),
	COUPON_TYPE_REQUIRED("쿠폰 타입이 설정되지 않았습니다."),
	COUPON_DISCOUNT_TARGET_AMOUNT_INVALID("할인 계산 대상 금액은 0 이상이어야 합니다."),
	COUPON_ISSUED_AT_REQUIRED("쿠폰 발행일은 비어있을 수 없습니다."),
	COUPON_ISSUED_AT_INVALID("쿠폰 발행일은 현재 시간보다 과거여야 합니다."),
	COUPON_EXPIRED_AT_REQUIRED("쿠폰 만료일은 비어있을 수 없습니다."),
	COUPON_EXPIRED_AT_INVALID("쿠폰 만료일은 현재 시간보다 미래여야 합니다."),
	COUPON_USED_AT_REQUIRED("쿠폰 사용일은 비어있을 수 없습니다."),
	COUPON_ALREADY_USED("이미 사용된 쿠폰입니다."),
	COUPON_NOT_FOUND("해당 [id = %s]의 쿠폰이 존재하지 않습니다."),

	PRODUCT_NOT_FOUND("해당 상품 ID '%s'의 상품을 찾을 수 없습니다."),
	STOCK_NOT_FOUND("해당 상품 ID '%s'의 재고가 존재하지 않습니다."),
	POINT_NOT_FOUND("해당 사용자 ID '%s'의 포인트가 존재하지 않습니다."),
	POINT_BALANCE_REQUIRED("포인트 금액은 필수입니다."),
	POINT_BALANCE_INVALID("포인트 금액은 %d보다 커야 합니다.");

	private final String message;

	ErrorMessage(String message) {
		this.message = message;
	}

	public String format(Object... args) {
		return String.format(message, args);
	}
}
