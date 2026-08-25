package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionChargeService {
    private final PaymentsService paymentsService;
    private final SubscriptionsService subscriptionsService;

    /**
     * PG 청구가 성공할때
     * Payments를 DONE으로 Subscriptions을 다음 주기로 진행시키는 걸 한 트랜잭션으로 묶는다.
     * wasRecovering=true면 PAST_DUE에서 복구(recoverActive), false면 정상 갱신(advanceBillingCycle).
     */
    @Transactional
    public void recordSuccess(Long paymentId, Long subscriptionId, boolean wasRecovering,
                              String providerTransactionId, String payToken, String rawResponse) {
        paymentsService.markSucceeded(paymentId, providerTransactionId, payToken, rawResponse);

        if (wasRecovering) {
            subscriptionsService.recoverActive(subscriptionId);
        } else {
            subscriptionsService.advanceBillingCycle(subscriptionId);
        }
    }

    /**
     * PG 청구가 실패했거나 예외가 났을 때 호출
     * Payments를 FAILED로, Subscriptions을 PAST_DUE로 전이시키는 걸 한 트랜잭션으로 묶는다.
     */
    @Transactional
    public void recordFailure(Long paymentId, Long subscriptionId, String failureReason, String rawResponse) {
        paymentsService.markFailed(paymentId, failureReason, rawResponse);
        subscriptionsService.markPastDue(subscriptionId);
    }

    /**
     * 승인이 COMMON_BILLING_KEY_NOT_FOUND로 실패했고 상태조회로 빌링키가 진짜 사라진 걸 확인했을 때 호출
     * 구독은 PAST_DUE(재시도 대상)로 보내지 않는다 - 빌링키 자체가
     * 없어져서 재시도해봐야 또 실패한다.
     */
    @Transactional
    public void recordFailureFromRevokedBillingKey(Long paymentId, String failureReason, String rawResponse) {
        paymentsService.markFailed(paymentId, failureReason, rawResponse);
    }
}
