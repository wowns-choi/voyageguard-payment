package com.voyageguard.payment.infrastructure.pg;

import com.voyageguard.payment.application.pg.PgApiException;
import com.voyageguard.payment.application.pg.PgCancelResult;
import com.voyageguard.payment.application.pg.PgClient;
import com.voyageguard.payment.application.pg.PgConfirmResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * PgClient의 토스페이먼츠 어댑터.
 * Toss의 실제 API 스펙(엔드포인트, Basic 인증 헤더 구성, 에러 응답 형식)은 이 클래스 안에만 존재한다
 * PaymentService/Payment 도메인은 이 클래스의 존재 자체를 모른다.
 * 나중에 PG사를 바꾸면 이 클래스와 같은 위치에 구현체를 하나 더 추가하고 빈만 교체하면 된다.
 */
@Component
public class TossPgClient implements PgClient {

    private static final String BASE_URL = "https://api.tosspayments.com";

    private final RestClient restClient;

    public TossPgClient(@Value("${toss.secret-key}") String secretKey) {
        // Basic 인증: 시크릿키 뒤에 콜론(:)을 붙이고 Base64 인코딩 (콜론 빠뜨리는 게 가장 흔한 실수)
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public PgConfirmResult confirm(String paymentKey, String orderId, Integer amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);

        TossPaymentResponse response = execute(() -> restClient.post()
                .uri("/v1/payments/confirm")
                .body(body)
                .retrieve()
                .body(TossPaymentResponse.class));

        return new PgConfirmResult(response.paymentKey(), response.approvedAt().toLocalDateTime());
    }

    @Override
    public PgCancelResult cancel(String paymentKey, String cancelReason, Integer cancelAmount) {
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            body.put("cancelAmount", cancelAmount);
        }

        TossPaymentResponse response = execute(() -> restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .body(body)
                .retrieve()
                .body(TossPaymentResponse.class));

        List<TossPaymentResponse.TossCancelDetail> cancels = response.cancels();
        TossPaymentResponse.TossCancelDetail latestCancel = cancels.get(cancels.size() - 1);
        return new PgCancelResult(latestCancel.canceledAt().toLocalDateTime());
    }

    // confirm()/cancel() 모두 예외처리가 동일하기 때문에, 중복을 피하기 위함
    private TossPaymentResponse execute(Supplier<TossPaymentResponse> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            String code = error != null ? error.code() : "UNKNOWN_ERROR";
            String message = error != null ? error.message() : e.getMessage();
            throw new PgApiException(code, message);
        }
    }
}
