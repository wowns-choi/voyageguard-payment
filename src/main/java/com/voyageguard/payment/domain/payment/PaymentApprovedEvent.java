package com.voyageguard.payment.domain.payment;

public record PaymentApprovedEvent(Long paymentId, Long reservationId) {
}
