package com.voyageguard.payment.application.pg;

import java.time.LocalDateTime;

public record PgCancelResult(LocalDateTime canceledAt) {
}
