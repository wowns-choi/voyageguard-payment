package com.voyageguard.payment.domain.payment;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    private Payment createPayment() {
        return Payment.create(1L, PaymentType.DEPOSIT, "RESV1-DEPOSIT-a3f9c21b", 100000);
    }

    @Test
    void create_시_REQUESTED_상태로_생성된다() {
        Payment payment = createPayment();

        assertEquals(1L, payment.getReservationId());
        assertEquals(PaymentType.DEPOSIT, payment.getPaymentType());
        assertEquals(100000, payment.getAmount());
        assertEquals(PaymentStatus.REQUESTED, payment.getStatus());
    }

    @Test
    void REQUESTED_상태에서_approve_하면_APPROVED로_전이된다() {
        Payment payment = createPayment();
        LocalDateTime approvedAt = LocalDateTime.now();

        payment.approve("paymentKey123", approvedAt);

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("paymentKey123", payment.getPaymentKey());
        assertEquals(approvedAt, payment.getApprovedAt());
    }

    @Test
    void REQUESTED가_아닌_상태에서_approve_하면_예외가_발생한다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> payment.approve("paymentKey456", LocalDateTime.now()));
    }

    @Test
    void REQUESTED_상태에서_fail_하면_FAILED로_전이된다() {
        Payment payment = createPayment();

        payment.fail("한도초과");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("한도초과", payment.getFailureReason());
    }

    @Test
    void REQUESTED가_아닌_상태에서_fail_하면_예외가_발생한다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> payment.fail("한도초과"));
    }

    @Test
    void APPROVED_상태에서_requestRefund_하면_REFUND_REQUESTED로_전이된다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());

        payment.requestRefund("단순변심");

        assertEquals(PaymentStatus.REFUND_REQUESTED, payment.getStatus());
        assertEquals("단순변심", payment.getRefundReason());
    }

    @Test
    void APPROVED가_아닌_상태에서_requestRefund_하면_예외가_발생한다() {
        Payment payment = createPayment();

        assertThrows(IllegalStateException.class, () -> payment.requestRefund("단순변심"));
    }

    @Test
    void REFUND_REQUESTED_상태에서_전액_approveRefund_하면_REFUNDED로_전이된다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());
        payment.requestRefund("단순변심");
        LocalDateTime canceledAt = LocalDateTime.now();

        payment.approveRefund(100000, canceledAt);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(100000, payment.getRefundedAmount());
        assertEquals(canceledAt, payment.getCanceledAt());
    }

    @Test
    void REFUND_REQUESTED_상태에서_부분금액_approveRefund_하면_PARTIALLY_REFUNDED로_전이된다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());
        payment.requestRefund("단순변심");

        payment.approveRefund(50000, LocalDateTime.now());

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        assertEquals(50000, payment.getRefundedAmount());
    }

    @Test
    void REFUND_REQUESTED_상태에서_결제금액을_초과한_금액으로_approveRefund_하면_예외가_발생한다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());
        payment.requestRefund("단순변심");

        assertThrows(IllegalStateException.class, () -> payment.approveRefund(200000, LocalDateTime.now()));
    }

    @Test
    void REFUND_REQUESTED가_아닌_상태에서_approveRefund_하면_예외가_발생한다() {
        Payment payment = createPayment();

        assertThrows(IllegalStateException.class, () -> payment.approveRefund(100000, LocalDateTime.now()));
    }

    @Test
    void REFUND_REQUESTED_상태에서_rejectRefund_하면_REFUND_REJECTED로_전이된다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());
        payment.requestRefund("단순변심");

        payment.rejectRefund("환불 정책상 거절");

        assertEquals(PaymentStatus.REFUND_REJECTED, payment.getStatus());
        assertEquals("환불 정책상 거절", payment.getRejectReason());
    }

    @Test
    void REFUND_REQUESTED가_아닌_상태에서_rejectRefund_하면_예외가_발생한다() {
        Payment payment = createPayment();

        assertThrows(IllegalStateException.class, () -> payment.rejectRefund("환불 정책상 거절"));
    }

    @Test
    void REFUND_REJECTED_상태에서_disputeRefundRejection_하면_REFUND_REQUESTED로_전이된다() {
        Payment payment = createPayment();
        payment.approve("paymentKey123", LocalDateTime.now());
        payment.requestRefund("단순변심");
        payment.rejectRefund("환불 정책상 거절");

        payment.disputeRefundRejection();

        assertEquals(PaymentStatus.REFUND_REQUESTED, payment.getStatus());
    }

    @Test
    void REFUND_REJECTED가_아닌_상태에서_disputeRefundRejection_하면_예외가_발생한다() {
        Payment payment = createPayment();

        assertThrows(IllegalStateException.class, payment::disputeRefundRejection);
    }
}
