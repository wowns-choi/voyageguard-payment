package com.voyageguard.payment.api.dto;

/** Toss 위젯의 successUrl 리다이렉트 쿼리파라미터(paymentKey, orderId, amount)를 그대로 받는다. */
public record PaymentConfirmRequest(String orderId, String paymentKey, Integer amount) {
}
