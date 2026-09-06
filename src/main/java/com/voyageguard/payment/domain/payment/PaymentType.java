package com.voyageguard.payment.domain.payment;

/** 결제의 "명목"(무엇에 대한 결제인지) - 생성 시 정해지고 이후 안 바뀜. */
public enum PaymentType {
    FULL,    // 전액
    DEPOSIT, // 예약금
    BALANCE  // 잔금
}
