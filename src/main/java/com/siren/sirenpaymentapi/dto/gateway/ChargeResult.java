package com.siren.sirenpaymentapi.dto.gateway;

/**
 * 정기 청구 승인 API 호출 결과(RecurringPaymentGateway.charge). SubscriptionChargeCoordinator가
 * 이 결과를 보고 SubscriptionChargeService.recordSuccess/recordFailure 중 어느 걸 호출할지 결정한다.
 */
public record ChargeResult(boolean success, String providerTransactionId, String payToken,
                            boolean billingKeyRevoked, String failureReason, String rawResponse) {

    public static ChargeResult success(String providerTransactionId, String payToken, String rawResponse) {
        return new ChargeResult(true, providerTransactionId, payToken, false, null, rawResponse);
    }

    public static ChargeResult failure(String failureReason, String rawResponse) {
        return new ChargeResult(false, null, null, false, failureReason, rawResponse);
    }

    /**
     * 승인이 COMMON_BILLING_KEY_NOT_FOUND로 실패했고, 상태조회로 재확인까지 해서 빌링키가
     * 진짜 사라진 걸 확인한 경우. 재시도(PAST_DUE)로 보내면 안 되고 즉시 구독을 정리해야 한다.
     */
    public static ChargeResult billingKeyRevoked(String failureReason, String rawResponse) {
        return new ChargeResult(false, null, null, true, failureReason, rawResponse);
    }
}
