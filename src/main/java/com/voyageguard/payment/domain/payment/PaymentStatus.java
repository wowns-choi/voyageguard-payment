package com.voyageguard.payment.domain.payment;

/** 결제 진행 상태 - Payment의 전이 메서드 호출에 따라 계속 바뀜 */
public enum PaymentStatus {
    REQUESTED,          // 결제요청됨, PG 승인 대기
    APPROVED,           // PG 승인 완료
    FAILED,             // PG 승인 실패
    REFUND_REQUESTED,   // 환불요청됨, 검토중 (이의제기로 재진입도 이 상태)
    REFUNDED,           // 환불완료(전액)
    PARTIALLY_REFUNDED, // 환불완료(부분)
    REFUND_REJECTED     // 환불거절됨
}
