package com.voyageguard.payment.api.dto;

/**
 * 결제 요청 성공 응답 - 프론트가 Toss 위젯을 초기화(customerKey)하고 결제를 요청(orderId, amount)
 * 하는 데 필요한 값만 담는다. customerKey는 정기결제(빌링) 없이 매 결제 요청마다 새로 발급하고
 * 재사용하지 않으므로 Payment 엔티티엔 저장하지 않는다.
 */
public record PaymentRequestResponse(Long paymentId, String orderId, String customerKey, Integer amount) {
}
