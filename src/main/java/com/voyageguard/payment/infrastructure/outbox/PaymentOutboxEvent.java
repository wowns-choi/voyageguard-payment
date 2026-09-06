package com.voyageguard.payment.infrastructure.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment 전용 Outbox - SalesOutboxEvent와 같은 이유로 BC별로 쪼갠 것(Sales 쪽 주석 참고).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    private String topic;

    private String messageKey;

    @Lob
    private String payload;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private PaymentOutboxEvent(String eventType, String topic, String messageKey, String payload) {
        this.eventType = eventType;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.published = false;
        this.createdAt = LocalDateTime.now();
    }

    public static PaymentOutboxEvent create(String eventType, String topic, String messageKey, String payload) {
        return new PaymentOutboxEvent(eventType, topic, messageKey, payload);
    }

    public void markPublished() {
        if (published) {
            throw new IllegalStateException("이미 발행된 이벤트입니다. id=" + id);
        }
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
