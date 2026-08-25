package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.domain.entity.Subscriptions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionsRepositoryCustom {

    // 자동청구 스케줄러 배치 쿼리: status IN (ACTIVE, PAST_DUE) AND nextBillingDate <= billingDate, billingKey는 fetch join
    List<Subscriptions> findDueForBilling(LocalDate billingDate);

    // CanceledSubscriptionExpiryScheduler 배치 쿼리: status=CANCELED AND currentPeriodEnd < cutoff
    List<Long> findCanceledPastPeriodEnd(LocalDateTime cutoff);
}
