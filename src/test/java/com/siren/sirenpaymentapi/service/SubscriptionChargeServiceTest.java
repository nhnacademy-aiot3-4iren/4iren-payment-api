package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.mail.MailEventPublisher;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChargeServiceTest {

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SubscriptionsService subscriptionsService;

    @Mock
    private MailEventPublisher mailEventPublisher;

    @InjectMocks
    private SubscriptionChargeService subscriptionChargeService;

    private Subscriptions stubSubscription() {
        BillingKeys billingKeys = mock(BillingKeys.class);
        when(billingKeys.getMaskedInfo()).thenReturn("토스페이");

        Subscriptions subscriptions = mock(Subscriptions.class);
        when(subscriptions.getUserId()).thenReturn(3L);
        when(subscriptions.getPlan()).thenReturn(Plan.MONTHLY);
        when(subscriptions.getAmount()).thenReturn(29000L);
        when(subscriptions.getBillingKey()).thenReturn(billingKeys);
        when(subscriptions.getNextBillingDate()).thenReturn(LocalDate.now().plusMonths(1));
        return subscriptions;
    }

    @Test
    void recordSuccessAdvancesBillingCycleWhenNotRecovering() {
        Subscriptions subscriptions = stubSubscription();
        when(subscriptionsService.advanceBillingCycle(2L)).thenReturn(subscriptions);

        subscriptionChargeService.recordSuccess(1L, 2L, false, "tx-1", "pay-token-1", "{}");

        verify(paymentsService).markSucceeded(1L, "tx-1", "pay-token-1", "{}");
        verify(subscriptionsService).advanceBillingCycle(2L);
        verify(subscriptionsService, never()).recoverActive(any());
        verify(mailEventPublisher).notify(eq(3L), any());
    }

    @Test
    void recordSuccessRecoversActiveWhenRecovering() {
        Subscriptions subscriptions = stubSubscription();
        when(subscriptionsService.recoverActive(2L)).thenReturn(subscriptions);

        subscriptionChargeService.recordSuccess(1L, 2L, true, "tx-1", "pay-token-1", "{}");

        verify(subscriptionsService).recoverActive(2L);
        verify(subscriptionsService, never()).advanceBillingCycle(any());
        verify(mailEventPublisher).notify(eq(3L), any());
    }

    @Test
    void recordFailureMarksPastDue() {
        when(subscriptionsService.markPastDue(2L, "실패 사유")).thenReturn(false);

        boolean expired = subscriptionChargeService.recordFailure(1L, 2L, "실패 사유", "{}");

        verify(paymentsService).markFailed(1L, "실패 사유", "{}");
        verify(subscriptionsService).markPastDue(2L, "실패 사유");
        assertFalse(expired);
    }

    @Test
    void recordFailureReturnsTrueWhenSubscriptionExpires() {
        when(subscriptionsService.markPastDue(2L, "실패 사유")).thenReturn(true);

        boolean expired = subscriptionChargeService.recordFailure(1L, 2L, "실패 사유", "{}");

        assertTrue(expired);
    }

    @Test
    void recordFailureFromRevokedBillingKeyDoesNotTouchSubscription() {
        subscriptionChargeService.recordFailureFromRevokedBillingKey(1L, "빌링키 소멸", "{}");

        verify(paymentsService).markFailed(1L, "빌링키 소멸", "{}");
        verifyNoInteractions(subscriptionsService);
    }
}
