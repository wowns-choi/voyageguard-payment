package com.voyageguard.payment.application.pg;

import java.time.LocalDateTime;

public record PgConfirmResult(String paymentKey, LocalDateTime approvedAt) {
}
