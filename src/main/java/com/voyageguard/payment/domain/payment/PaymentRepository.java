package com.voyageguard.payment.domain.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    boolean existsByReservationIdAndPaymentTypeAndStatus(Long reservationId, PaymentType paymentType, PaymentStatus status);
}
