package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.repository.SubscriptionsRepository;
import com.siren.sirenpaymentapi.service.RoleChangeEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionsServiceTest {

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

    @Mock
    private RoleChangeEventPublisher roleChangeEventPublisher;

    @InjectMocks
    private SubscriptionsService subscriptionsService;

    @BeforeEach
    void setMaxRetryCount() {
        ReflectionTestUtils.setField(subscriptionsService, "maxRetryCount", 3);
    }

    private Subscriptions newActiveSubscription() {
        return Subscriptions.builder()
                .id(1L)
                .userId(1L)
                .plan(Plan.MONTHLY)
                .amount(29000L)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(LocalDateTime.now())
                .nextBillingDate(LocalDate.now())
                .retryCount(0)
                .build();
    }

    @Test
    void findActiveByUserIdReturnsSubscription() {
        Subscriptions subscription = newActiveSubscription();
        when(subscriptionsRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        Optional<Subscriptions> result = subscriptionsService.findActiveByUserId(1L);

        assertTrue(result.isPresent());
    }

    @Test
    void hasSubscriptionHistoryReturnsTrue() {
        when(subscriptionsRepository.existsByUserId(1L)).thenReturn(true);

        assertTrue(subscriptionsService.hasSubscriptionHistory(1L));
    }

    @Test
    void hasSubscriptionHistoryReturnsFalse() {
        when(subscriptionsRepository.existsByUserId(1L)).thenReturn(false);

        assertFalse(subscriptionsService.hasSubscriptionHistory(1L));
    }

    @Test
    void registerSubscriptionSavesActiveSubscription() {
        BillingKeys billingKeys = BillingKeys.builder().id(1L).build();
        PlanPrices planPrice = PlanPrices.builder().id(1L).build();
        when(subscriptionsRepository.save(any(Subscriptions.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscriptions result = subscriptionsService.registerSubscription(1L, billingKeys, planPrice, Plan.MONTHLY, 29000L);

        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(29000L, result.getAmount());
    }

    @Test
    void replaceBillingKeyChangesBillingKey() {
        Subscriptions subscription = newActiveSubscription();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));
        BillingKeys newKey = BillingKeys.builder().id(2L).build();

        subscriptionsService.replaceBillingKey(1L, newKey);

        assertEquals(2L, subscription.getBillingKey().getId());
    }

    @Test
    void advanceBillingCycleMovesNextBillingDate() {
        Subscriptions subscription = newActiveSubscription();
        LocalDateTime before = subscription.getCurrentPeriodEnd();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.advanceBillingCycle(1L);

        assertTrue(subscription.getCurrentPeriodEnd().isAfter(before));
    }

    @Test
    void recoverActiveSetsStatusActive() {
        Subscriptions subscription = newActiveSubscription();
        subscription.markPastDue();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.recoverActive(1L);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(0, subscription.getRetryCount());
    }

    @Test
    void markPastDueKeepsPastDueWhenRetryCountLow() {
        Subscriptions subscription = newActiveSubscription();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.markPastDue(1L);

        assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
        verify(roleChangeEventPublisher, never()).requestRoleChange(any(), any(), any());
    }

    @Test
    void markPastDueExpiresWhenRetryCountExceeded() {
        Subscriptions subscription = Subscriptions.builder()
                .id(1L)
                .userId(1L)
                .plan(Plan.MONTHLY)
                .amount(29000L)
                .status(SubscriptionStatus.PAST_DUE)
                .currentPeriodEnd(LocalDateTime.now())
                .nextBillingDate(LocalDate.now())
                .retryCount(3)
                .build();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.markPastDue(1L);

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        verify(roleChangeEventPublisher).requestRoleChange(1L, RoleChangeRequested.NORMAL, null);
    }

    @Test
    void markExpiredPublishesRoleChangeEvent() {
        Subscriptions subscription = newActiveSubscription();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.markExpired(1L);

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        verify(roleChangeEventPublisher).requestRoleChange(1L, RoleChangeRequested.NORMAL, null);
    }

    @Test
    void markExpiredThrowsWhenNotFound() {
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundSubscriptionException.class, () -> subscriptionsService.markExpired(1L));
    }

    @Test
    void markCanceledSetsStatusCanceled() {
        Subscriptions subscription = newActiveSubscription();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.markCanceled(1L);

        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
    }

    @Test
    void downgradeAfterCancelationKeepsStatusCanceled() {
        Subscriptions subscription = newActiveSubscription();
        subscription.markCanceled();
        when(subscriptionsRepository.findLockedById(1L)).thenReturn(Optional.of(subscription));

        subscriptionsService.downgradeAfterCancelation(1L);

        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        assertNotNull(subscription.getRoleDowngradedAt());
        verify(roleChangeEventPublisher).requestRoleChange(1L, RoleChangeRequested.NORMAL, null);
    }

    @Test
    void findDueForBillingMapsToBillingTarget() {
        Subscriptions subscription = newActiveSubscription();
        subscription.replaceBillingKey(BillingKeys.builder().id(1L).build());
        when(subscriptionsRepository.findDueForBilling(any(LocalDate.class))).thenReturn(List.of(subscription));

        assertEquals(1, subscriptionsService.findDueForBilling(LocalDate.now()).size());
    }

    @Test
    void findCanceledPastPeriodEndReturnsIds() {
        when(subscriptionsRepository.findCanceledPastPeriodEnd(any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));

        List<Long> result = subscriptionsService.findCanceledPastPeriodEnd(LocalDateTime.now());

        assertEquals(2, result.size());
    }
}
