package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionsTest {

    private Subscriptions newMonthlySubscription() {
        return Subscriptions.builder()
                .userId(1L)
                .plan(Plan.MONTHLY)
                .amount(29000L)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0))
                .nextBillingDate(LocalDate.of(2026, Month.JANUARY, 1))
                .retryCount(0)
                .build();
    }

    private Subscriptions newYearlySubscription() {
        return Subscriptions.builder()
                .userId(1L)
                .plan(Plan.YEARLY)
                .amount(290000L)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0))
                .nextBillingDate(LocalDate.of(2026, Month.JANUARY, 1))
                .retryCount(0)
                .build();
    }

    @Test
    void advanceBillingCycleMonthly() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.advanceBillingCycle();
        assertEquals(LocalDateTime.of(2026, Month.FEBRUARY, 1, 0, 0), subscription.getCurrentPeriodEnd());
        assertEquals(LocalDate.of(2026, Month.FEBRUARY, 1), subscription.getNextBillingDate());
    }

    @Test
    void advanceBillingCycleYearly() {
        Subscriptions subscription = newYearlySubscription();
        subscription.advanceBillingCycle();
        assertEquals(LocalDateTime.of(2027, Month.JANUARY, 1, 0, 0), subscription.getCurrentPeriodEnd());
        assertEquals(LocalDate.of(2027, Month.JANUARY, 1), subscription.getNextBillingDate());
    }

    @Test
    void recoverActiveResetsRetryCount() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markPastDue();
        subscription.markPastDue();
        subscription.recoverActive();
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(0, subscription.getRetryCount());
    }

    @Test
    void markPastDueIncreasesRetryCount() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markPastDue();
        assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
        assertEquals(1, subscription.getRetryCount());
    }

    @Test
    void markPastDueTwiceIncreasesRetryCountTwice() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markPastDue();
        subscription.markPastDue();
        assertEquals(2, subscription.getRetryCount());
    }

    @Test
    void markExpiredSetsStatusAndExpiredAt() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markExpired();
        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        assertNotNull(subscription.getExpiredAt());
    }

    @Test
    void markCanceledSetsStatusAndCanceledAt() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markCanceled();
        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        assertNotNull(subscription.getCanceledAt());
    }

    @Test
    void markRoleDowngradedSetsTimestamp() {
        Subscriptions subscription = newMonthlySubscription();
        subscription.markCanceled();
        subscription.markRoleDowngraded();
        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        assertNotNull(subscription.getRoleDowngradedAt());
    }

    @Test
    void replaceBillingKeyChangesBillingKey() {
        Subscriptions subscription = newMonthlySubscription();
        BillingKeys newKey = BillingKeys.builder().id(99L).build();
        subscription.replaceBillingKey(newKey);
        assertEquals(99L, subscription.getBillingKey().getId());
    }
}
