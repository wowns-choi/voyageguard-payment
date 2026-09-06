package com.voyageguard.payment.domain.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1. 결제 요청부터 PG 승인/실패, 환불까지의 생애주기를 캡슐화한 Aggregate Root.
 *    (REQUESTED -> APPROVED/FAILED -> REFUND_REQUESTED -> REFUNDED/PARTIALLY_REFUNDED/REFUND_REJECTED)
 *
 * 2. 하나의 Reservation에 Payment가 여러 개 달릴 수 있다
 *    예약금/잔금 분할결제, 실패 후 재시도가 각각 별도의 Payment(및 별도의 PG orderId)로 취급되기 때문.
 *
 * 3. 환불은 1회만 허용한다 (REFUNDED/PARTIALLY_REFUNDED가 최종 상태)
 *    여러 번 부분환불을 반복하는 흐름은 스코프에서 의도적으로 제외.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reservation을 객체가 아닌 ID로 참조한다 (Reference Other Aggregates by Identity Only)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    private String orderId;

    private String paymentKey; // PG 승인 전엔 null

    private Integer amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime approvedAt;

    private String failureReason;

    private String refundReason;

    private String rejectReason;

    private Integer refundedAmount;

    private LocalDateTime canceledAt;

    private Payment(Long reservationId, PaymentType paymentType, String orderId, Integer amount) {
        this.reservationId = reservationId;
        this.paymentType = paymentType;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
    }

    public static Payment create(Long reservationId, PaymentType paymentType, String orderId, Integer amount) {
        return new Payment(reservationId, paymentType, orderId, amount);
    }

    public void approve(String paymentKey, LocalDateTime approvedAt) {
        if (status != PaymentStatus.REQUESTED) {
            throw new IllegalStateException("결제요청 상태에서만 승인할 수 있습니다. 현재 상태: " + status);
        }
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
        this.status = PaymentStatus.APPROVED;
    }

    public void fail(String failureReason) {
        if (status != PaymentStatus.REQUESTED) {
            throw new IllegalStateException("결제요청 상태에서만 실패 처리할 수 있습니다. 현재 상태: " + status);
        }
        this.failureReason = failureReason;
        this.status = PaymentStatus.FAILED;
    }

    public void requestRefund(String refundReason) {
        if (status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("승인 상태에서만 환불을 요청할 수 있습니다. 현재 상태: " + status);
        }
        this.refundReason = refundReason;
        this.status = PaymentStatus.REFUND_REQUESTED;
    }

    public void approveRefund(Integer refundAmount, LocalDateTime canceledAt) {
        if (status != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불요청 상태에서만 환불을 승인할 수 있습니다. 현재 상태: " + status);
        }
        if (refundAmount > amount) {
            throw new IllegalStateException("환불 금액이 결제 금액을 초과할 수 없습니다. 결제금액: " + amount + ", 환불금액: " + refundAmount);
        }
        this.refundedAmount = refundAmount;
        this.canceledAt = canceledAt;
        this.status = refundAmount.equals(amount) ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public void rejectRefund(String rejectReason) {
        if (status != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불요청 상태에서만 거절할 수 있습니다. 현재 상태: " + status);
        }
        this.rejectReason = rejectReason;
        this.status = PaymentStatus.REFUND_REJECTED;
    }

    public void disputeRefundRejection() {
        if (status != PaymentStatus.REFUND_REJECTED) {
            throw new IllegalStateException("환불거절 상태에서만 이의제기할 수 있습니다. 현재 상태: " + status);
        }
        this.status = PaymentStatus.REFUND_REQUESTED;
    }
}
