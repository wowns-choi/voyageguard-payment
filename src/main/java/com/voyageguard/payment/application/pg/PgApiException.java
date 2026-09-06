package com.voyageguard.payment.application.pg;

import com.voyageguard.common.exception.BusinessException;

/**
 * PG사(현재 토스페이먼츠) confirm/cancel 호출이 실패했을 때 어댑터(TossPgClient 등)가 던지는 예외.
 * BusinessException을 상속해서 GlobalExceptionHandler의 기존 핸들러가 그대로 잡고,
 * PG사가 내려준 에러코드를 응답의 code 필드로 그대로 전달한다
 */
public class PgApiException extends BusinessException {

    private final String errorCode;

    public PgApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }
}
