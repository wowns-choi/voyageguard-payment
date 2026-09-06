package com.voyageguard.payment.application.reservation;

/**
 * Sales의 Reservation 조회 포트(port).
 * MSA 분리 대비 - Payment는 Sales의 ReservationRepository를 직접 참조하지 않고 이 인터페이스로만
 * 의존한다.
 */
public interface ReservationClient {

    ReservationView get(Long reservationId);
}
