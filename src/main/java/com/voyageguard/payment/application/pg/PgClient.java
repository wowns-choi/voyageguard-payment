package com.voyageguard.payment.application.pg;

/** PG사(현재 토스페이먼츠)에 결제 승인/취소를 요청하는 포트(port) */
public interface PgClient {

    PgConfirmResult confirm(String paymentKey, String orderId, Integer amount);

    PgCancelResult cancel(String paymentKey, String cancelReason, Integer cancelAmount);
}
