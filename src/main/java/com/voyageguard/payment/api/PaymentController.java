package com.voyageguard.payment.api;

import com.voyageguard.payment.api.dto.PaymentConfirmRequest;
import com.voyageguard.payment.api.dto.PaymentCreateRequest;
import com.voyageguard.payment.api.dto.PaymentFailRequest;
import com.voyageguard.payment.api.dto.PaymentRefundApproveRequest;
import com.voyageguard.payment.api.dto.PaymentRefundRejectRequest;
import com.voyageguard.payment.api.dto.PaymentRefundRequest;
import com.voyageguard.payment.api.dto.PaymentRequestResponse;
import com.voyageguard.payment.application.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "결제 요청/승인/실패, 환불 요청/승인/거절/이의제기 API")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청", description = "예약요청 상태의 예약에 대해 결제를 요청하고, Toss 위젯 초기화에 필요한 값을 반환한다.")
    @ApiResponse(responseCode = "401", description = "로그인 필요")
    @ApiResponse(responseCode = "403", description = "본인의 예약이 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 예약")
    @ApiResponse(responseCode = "409", description = "예약요청 상태가 아니어서 결제 요청 불가")
    @PostMapping
    public PaymentRequestResponse request(@RequestBody PaymentCreateRequest request) {
        return paymentService.request(request.reservationId(), request.paymentType(), request.amount());
    }

    @Operation(summary = "결제 승인(콜백)", description = "Toss 위젯 successUrl 리다이렉트 값으로 PG 승인을 요청하고, 성공 시 예약을 확정한다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "금액 불일치, 이미 처리된 결제, 또는 PG 승인 실패")
    @PostMapping("/confirm")
    public void confirm(@RequestBody PaymentConfirmRequest request) {
        paymentService.confirmSuccess(request.orderId(), request.paymentKey(), request.amount());
    }

    @Operation(summary = "결제 실패(콜백)", description = "Toss 위젯 failUrl 리다이렉트 값으로 결제를 실패 처리한다. confirm API는 호출하지 않는다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "결제요청 상태가 아니어서 실패 처리 불가")
    @PostMapping("/fail")
    public void fail(@RequestBody PaymentFailRequest request) {
        paymentService.confirmFailure(request.orderId(), request.code() + ": " + request.message());
    }

    @Operation(summary = "환불 요청", description = "승인 상태의 결제에 대해 환불을 요청한다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "승인 상태가 아니어서 환불 요청 불가")
    @PostMapping("/{id}/refund-request")
    public void requestRefund(@PathVariable Long id, @RequestBody PaymentRefundRequest request) {
        paymentService.requestRefund(id, request.reason());
    }

    @Operation(summary = "환불 승인", description = "환불요청 상태의 결제를 승인하고 실제 PG 환불을 처리한다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "환불요청 상태가 아니거나, 환불 금액이 결제 금액을 초과하거나, PG 환불 실패")
    @PostMapping("/{id}/refund-approve")
    public void approveRefund(@PathVariable Long id, @RequestBody PaymentRefundApproveRequest request) {
        paymentService.approveRefund(id, request.refundAmount());
    }

    @Operation(summary = "환불 거절", description = "환불요청 상태의 결제를 거절한다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "환불요청 상태가 아니어서 거절 불가")
    @PostMapping("/{id}/refund-reject")
    public void rejectRefund(@PathVariable Long id, @RequestBody PaymentRefundRejectRequest request) {
        paymentService.rejectRefund(id, request.reason());
    }

    @Operation(summary = "환불 거절 이의제기", description = "환불거절 상태의 결제를 재검토(환불요청) 상태로 되돌린다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    @ApiResponse(responseCode = "409", description = "환불거절 상태가 아니어서 이의제기 불가")
    @PostMapping("/{id}/refund-dispute")
    public void disputeRefundRejection(@PathVariable Long id) {
        paymentService.disputeRefundRejection(id);
    }
}
