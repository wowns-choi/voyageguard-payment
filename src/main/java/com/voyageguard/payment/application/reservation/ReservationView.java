package com.voyageguard.payment.application.reservation;

/** Payment가 Sales의 Reservation에 대해 알아야 하는 정보만 담은 뷰. */
public record ReservationView(Long id, Long memberId, Status status, Integer depositAmount, Integer balanceAmount) {

    public enum Status { REQUESTED, CONFIRMED, CANCELLED, EXPIRED }
}
