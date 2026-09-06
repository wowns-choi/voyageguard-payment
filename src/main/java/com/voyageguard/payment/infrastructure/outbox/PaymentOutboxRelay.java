package com.voyageguard.payment.infrastructure.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentOutboxEvent 테이블을 주기적으로 폴링해서, 아직 발행 안 된 행을 실제로 Kafka에 발행하는
 * 릴레이. 동작은 SalesOutboxRelay와 동일(BC별 독립 테이블만 다름).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxRelay {

    private final PaymentOutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void relay() {
        List<PaymentOutboxEvent> pendingEvents = outboxEventRepository.findByPublishedFalse();
        for (PaymentOutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get();
                event.markPublished();
            } catch (Exception e) {
                log.warn("PaymentOutboxEvent 발행 실패, 다음 폴링에서 재시도. id={}, topic={}", event.getId(), event.getTopic(), e);
            }
        }
    }
}
