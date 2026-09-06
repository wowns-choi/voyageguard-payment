package com.voyageguard.payment.api.dto;

import com.voyageguard.payment.domain.payment.PaymentType;

public record PaymentCreateRequest(Long reservationId, PaymentType paymentType, Integer amount) {
}
