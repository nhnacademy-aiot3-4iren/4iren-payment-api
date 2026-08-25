package com.siren.sirenpaymentapi.dto.subscriptions;

import com.siren.sirenpaymentapi.domain.Provider;

/**
 * 자동청구 스케줄러가 조회한 청구 대상 - SubscriptionChargeCoordinator.chargeSubscription에 넘김.
 */
public record BillingTarget(Long subscriptionId, Long userId, Provider provider, String providerCredential,
                             Long amount, boolean wasRecovering) {
}
