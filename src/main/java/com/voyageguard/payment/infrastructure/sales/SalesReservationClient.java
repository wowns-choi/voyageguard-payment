package com.voyageguard.payment.infrastructure.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.voyageguard.payment.application.reservation.ReservationClient;
import com.voyageguard.payment.application.reservation.ReservationView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** ReservationClient의 REST 어댑터 - Sales가 노출하는 GET /api/v1/reservations/{id}를 호출한다. */
@Component
public class SalesReservationClient implements ReservationClient {

    private final RestClient restClient;

    public SalesReservationClient(@Value("${sales.service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ReservationView get(Long reservationId) {
        try {
            ReservationResponse response = restClient.get()
                    .uri("/api/v1/reservations/{id}", reservationId)
                    .retrieve()
                    .body(ReservationResponse.class);
            return new ReservationView(
                    response.id(),
                    response.memberId(),
                    ReservationView.Status.valueOf(response.status()),
                    response.depositAmount(),
                    response.balanceAmount()
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("존재하지 않는 예약입니다. id=" + reservationId);
        }
    }

    // Sales의 ReservationResponse 중 Payment가 필요한 필드만 매핑
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReservationResponse(Long id, Long memberId, String status, Integer depositAmount, Integer balanceAmount) {
    }
}
