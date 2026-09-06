package com.voyageguard.payment.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Toss가 confirm/cancel 실패 시 내려주는 에러 응답 형식(code, message). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossErrorResponse(String code, String message) {
}
