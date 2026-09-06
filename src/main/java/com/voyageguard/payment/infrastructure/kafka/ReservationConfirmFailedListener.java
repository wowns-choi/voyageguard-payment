package com.voyageguard.payment.infrastructure.kafka;

import com.voyageguard.payment.application.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Sales가 발행하는 ReservationConfirmFailed(예약확정실패) 이벤트를 구독해서 자동 환불한다.
 */
@Component
@RequiredArgsConstructor
public class ReservationConfirmFailedListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation.confirm-failed", groupId = "payment-compensation")
    public void handle(String payload) {
        ReservationConfirmFailedPayload event = objectMapper.readValue(payload, ReservationConfirmFailedPayload.class);
        paymentService.compensateConfirmFailure(event.paymentId(), event.reason());
    }

    private record ReservationConfirmFailedPayload(Long paymentId, Long reservationId, String reason) {
    }
}
