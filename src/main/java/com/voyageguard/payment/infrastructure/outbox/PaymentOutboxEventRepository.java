package com.voyageguard.payment.infrastructure.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, Long> {

    List<PaymentOutboxEvent> findByPublishedFalse();
}
