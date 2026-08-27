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
     * 반환값: 이번 호출로 구독이 방금 EXPIRED로 전이됐는지(Dunning 재시도 소진) - true면 호출부가
     * 트랜잭션 밖에서 PG 빌링키 revoke를 이어서 해야 한다(BillingKeyRegistrationService.revokeBillingKeyAfterExpiry).
     */
    @Transactional
    public boolean recordFailure(Long paymentId, Long subscriptionId, String failureReason, String rawResponse) {
        paymentsService.markFailed(paymentId, failureReason, rawResponse);
        return subscriptionsService.markPastDue(subscriptionId);
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

    /**
     * 가입 직후 첫 청구가 성공했을 때 호출. registerSubscription이 이미 첫 주기(currentPeriodEnd/
     * nextBillingDate)를 잡아뒀으므로 advanceBillingCycle은 부르지 않는다(부르면 한 주기 더 밀려버림) -
     * Payments만 DONE으로 남긴다.
     */
    @Transactional
    public void recordInitialChargeSuccess(Long paymentId, String providerTransactionId, String payToken,
                                            String rawResponse) {
        paymentsService.markSucceeded(paymentId, providerTransactionId, payToken, rawResponse);
    }

    /**
     * 가입 직후 첫 청구가 실패했을 때 호출. PAST_DUE 재시도로 보내지 않고 구독을 바로 종결시킨다
     * (첫 결제 실패는 사용자가 바로 다시 시도하면 되지, 며칠짜리 유예를 줄 이유가 없음).
     */
    @Transactional
    public void recordInitialChargeFailure(Long paymentId, Long subscriptionId, String failureReason,
                                            String rawResponse) {
        paymentsService.markFailed(paymentId, failureReason, rawResponse);
        subscriptionsService.failInitialCharge(subscriptionId);
    }
}
