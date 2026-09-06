package com.voyageguard.payment.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Toss의 결제 승인/취소 API가 공통으로 반환하는 Payment 객체 중, 실제로 쓰는 필드만 뽑아서 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
        String paymentKey,
        String status,
        OffsetDateTime approvedAt,
        List<TossCancelDetail> cancels
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossCancelDetail(OffsetDateTime canceledAt) {
    }
}
