package com.siren.sirenpaymentapi.dto.payments;

/**
 * PaymentsService.prepareCharge가 READY row를 커밋한 직후 리턴하는 값.
 * SubscriptionChargeCoordinator가 이 record의 orderId로 PG 승인을 요청하고, paymentId로 결과를 기록한다.
 */
public record PreparedCharge(Long paymentId, String orderId) {
}
