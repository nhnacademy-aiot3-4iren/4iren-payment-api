package com.siren.sirenpaymentapi.dto.subscriptions;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResponse(
        Plan plan,
        Long amount,
        SubscriptionStatus status,
        LocalDateTime currentPeriodEnd,
        LocalDate nextBillingDate
) {
}
