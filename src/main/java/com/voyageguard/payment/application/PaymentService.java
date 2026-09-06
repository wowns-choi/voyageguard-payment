package com.voyageguard.payment.application;

import com.voyageguard.common.exception.AuthenticationFailedException;
import com.voyageguard.common.exception.AuthorizationFailedException;
import com.voyageguard.common.security.CurrentMember;
import com.voyageguard.payment.api.dto.PaymentRequestResponse;
import com.voyageguard.payment.application.pg.PgApiException;
import com.voyageguard.payment.application.pg.PgCancelResult;
import com.voyageguard.payment.application.pg.PgClient;
import com.voyageguard.payment.application.pg.PgConfirmResult;
import com.voyageguard.payment.application.reservation.ReservationClient;
import com.voyageguard.payment.application.reservation.ReservationView;
import com.voyageguard.payment.domain.payment.Payment;
import com.voyageguard.payment.domain.payment.PaymentApprovedEvent;
import com.voyageguard.payment.domain.payment.PaymentRepository;
import com.voyageguard.payment.domain.payment.PaymentStatus;
import com.voyageguard.payment.domain.payment.PaymentType;
import com.voyageguard.payment.infrastructure.outbox.PaymentOutboxEvent;
import com.voyageguard.payment.infrastructure.outbox.PaymentOutboxEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationClient reservationClient;
    private final PgClient pgClient;
    private final PaymentOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CurrentMember currentMember;

    // 예약 상태 검증을 동기 REST 호출함.
    public PaymentRequestResponse request(Long reservationId, PaymentType paymentType, Integer amount) {
        Long memberId = currentMember.memberId()
                .orElseThrow(() -> new AuthenticationFailedException("로그인이 필요합니다."));

        ReservationView reservation = reservationClient.get(reservationId);
        // 본인 예약에 대해서만 결제 요청 가능 - 예약 id만 알면 남의 예약도 대신 결제할 수 있던 문제를 막기 위함
        if (!reservation.memberId().equals(memberId)) {
            throw new AuthorizationFailedException("본인의 예약에 대해서만 결제를 요청할 수 있습니다.");
        }
        if (reservation.status() != ReservationView.Status.REQUESTED && reservation.status() != ReservationView.Status.CONFIRMED) {
            throw new IllegalStateException("예약요청 또는 확정 상태에서만 결제를 요청할 수 있습니다. 현재 상태: " + reservation.status());
        }
        // CONFIRMED 예약도 결제 요청을 허용하는 이유:
        // 예약금 결제 성공 시 confirmSuccess()가 예약을 곧바로 CONFIRMED로 확정하는데,
        // 그 뒤에 이어지는 잔금(BALANCE) 결제 요청까지 막히면 안 되기 때문.
        // 대신 같은 paymentType으로 이미 승인된 결제가 있으면 막아서,
        // 예약금/잔금 중복 요청·전액결제 재요청 같은 중복 결제를 방지한다.
        if (paymentRepository.existsByReservationIdAndPaymentTypeAndStatus(reservationId, paymentType, PaymentStatus.APPROVED)) {
            throw new IllegalStateException("이미 승인된 " + paymentType + " 결제가 존재합니다. reservationId=" + reservationId);
        }

        // amount는 클라이언트가 넘기는 값이라 그대로 믿지 않고,
        // Reservation에 스냅샷된 예약금/잔금과 정확히 일치하는지 검증한다
        Integer expectedAmount = switch (paymentType) {
            case DEPOSIT -> reservation.depositAmount();
            case BALANCE -> reservation.balanceAmount();
            case FULL -> reservation.depositAmount() + reservation.balanceAmount();
        };
        if (!amount.equals(expectedAmount)) {
            throw new IllegalStateException(
                    "요청 금액이 맞지 않습니다. " + paymentType + " 기대 금액: " + expectedAmount + ", 요청 금액: " + amount);
        }

        String orderId = generateOrderId(reservationId, paymentType);
        Payment payment = Payment.create(reservationId, paymentType, orderId, amount);
        paymentRepository.save(payment);

        // Toss가 발급하는 값이 아니라 우리가 만들어서 넘겨주는 값 - 정기결제 없이는 재사용할 이유가 없어 매번 새로 발급, 저장 안 함
        String customerKey = UUID.randomUUID().toString();
        return new PaymentRequestResponse(payment.getId(), orderId, customerKey, amount);
    }

    /**
     * 결제 승인.
     * 금액 위변조 검증을 PG 승인 요청보다 먼저 해서, 조작된 금액으로 실제 승인 API가 나가는 일이 없게 한다.
     * 예약 확정은 여기서 직접 안 하고 PaymentApproved 이벤트로 Sales에 알린다(Choreography Saga) -
     * PG 승인은 이미 벌어진 되돌릴 수 없는 사실이라, Sales 쪽 확정 처리가 실패해도 이 트랜잭션은
     * 영향받으면 안 되기 때문.
     */
    public void confirmSuccess(String orderId, String paymentKey, Integer amount) {
        Payment payment = getPaymentByOrderId(orderId);
        if (!payment.getAmount().equals(amount)) {
            throw new IllegalStateException(
                    "요청 금액이 결제 금액과 일치하지 않습니다. 저장된 금액: " + payment.getAmount() + ", 전달된 금액: " + amount);
        }

        // 결제 승인 요청
        // 참고) paymentKey : orderId와 달리 Toss가 발급한 값. 이후 조회/취소 시 Toss가 요구하는 식별자
        try {
            PgConfirmResult result = pgClient.confirm(paymentKey, orderId, amount); /// 결제 승인 요청
            payment.approve(result.paymentKey(), result.approvedAt());
        } catch (PgApiException e) {
            payment.fail(e.getMessage());
            throw e;
        }

        publishPaymentApproved(payment);
    }

    /** 결제 실패 */
    public void confirmFailure(String orderId, String failureReason) {
        Payment payment = getPaymentByOrderId(orderId);
        payment.fail(failureReason);
    }

    // 보상 트랜잭션(자동 환불) - 카프카 중복 전달 대비, APPROVED 아니면 조용히 스킵
    public void compensateConfirmFailure(Long paymentId, String reason) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.info("이미 처리된 결제라 보상(환불)을 건너뜀. paymentId={}, 현재 상태={}", paymentId, payment.getStatus());
            return;
        }

        payment.requestRefund(reason);
        PgCancelResult result = pgClient.cancel(payment.getPaymentKey(), reason, payment.getAmount());
        payment.approveRefund(payment.getAmount(), result.canceledAt());
    }

    // Kafka로 바로 안 보내고, 같은 트랜잭션 안에서 outbox 테이블에 "보낼 것"만 원자적으로 기록
    private void publishPaymentApproved(Payment payment) {
        PaymentApprovedEvent event = new PaymentApprovedEvent(payment.getId(), payment.getReservationId());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("PaymentApproved 직렬화 실패", e);
        }
        outboxEventRepository.save(
                PaymentOutboxEvent.create(
                        "PaymentApproved",
                        "payment.approved",
                        payment.getReservationId().toString(), // key : 예약 id - 같은 예약에 대한 이벤트는 순서 보장
                        payload
                )
        );
    }

    /** 환불 요청 */
    public void requestRefund(Long paymentId, String refundReason) {
        Payment payment = getPayment(paymentId);
        payment.requestRefund(refundReason);
    }

    /** 환불 승인 요청 */
    public void approveRefund(Long paymentId, Integer refundAmount) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불요청 상태에서만 환불을 승인할 수 있습니다. 현재 상태: " + payment.getStatus());
        }

        PgCancelResult result = pgClient.cancel(payment.getPaymentKey(), payment.getRefundReason(), refundAmount);
        payment.approveRefund(refundAmount, result.canceledAt());
    }

    /** 환불 거절 */
    public void rejectRefund(Long paymentId, String rejectReason) {
        Payment payment = getPayment(paymentId);
        payment.rejectRefund(rejectReason);
    }

    public void disputeRefundRejection(Long paymentId) {
        Payment payment = getPayment(paymentId);
        payment.disputeRefundRejection();
    }

    /**
     * reservationId를 그대로 orderId로 쓰지 않는 이유:
     * 1) 한 Reservation에 Payment가 여러 개(예약금/잔금, 재시도) 달릴 수 있는데, Toss orderId는
     *    결제 시도 1건마다 유니크해야 한다 - reservationId만 쓰면 두 번째 결제부터 충돌.
     * 2) Toss orderId는 최소 6자 이상이어야 하는데, reservationId 숫자만으로는 이 길이를 못 채우는
     *    경우가 많다(특히 초창기 낮은 id).
     */
    private String generateOrderId(Long reservationId, PaymentType paymentType) {
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        return "RESV" + reservationId + "-" + paymentType + "-" + shortUuid;
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다. id=" + id));
    }

    private Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다. orderId=" + orderId));
    }
}
