package com.voyageguard.payment.api.dto;

/** Toss 위젯의 failUrl 리다이렉트 쿼리파라미터(code, message, orderId)를 그대로 받는다. */
public record PaymentFailRequest(String orderId, String code, String message) {
}
