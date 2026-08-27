package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChargeServiceTest {

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SubscriptionsService subscriptionsService;

    @InjectMocks
    private SubscriptionChargeService subscriptionChargeService;

    @Test
    void recordSuccessAdvancesBillingCycleWhenNotRecovering() {
        subscriptionChargeService.recordSuccess(1L, 2L, false, "tx-1", "pay-token-1", "{}");

        verify(paymentsService).markSucceeded(1L, "tx-1", "pay-token-1", "{}");
        verify(subscriptionsService).advanceBillingCycle(2L);
        verify(subscriptionsService, never()).recoverActive(any());
    }

    @Test
    void recordSuccessRecoversActiveWhenRecovering() {
        subscriptionChargeService.recordSuccess(1L, 2L, true, "tx-1", "pay-token-1", "{}");

        verify(subscriptionsService).recoverActive(2L);
        verify(subscriptionsService, never()).advanceBillingCycle(any());
    }

    @Test
    void recordFailureMarksPastDue() {
        when(subscriptionsService.markPastDue(2L)).thenReturn(false);

        boolean expired = subscriptionChargeService.recordFailure(1L, 2L, "실패 사유", "{}");

        verify(paymentsService).markFailed(1L, "실패 사유", "{}");
        verify(subscriptionsService).markPastDue(2L);
        assertFalse(expired);
    }

    @Test
    void recordFailureReturnsTrueWhenSubscriptionExpires() {
        when(subscriptionsService.markPastDue(2L)).thenReturn(true);

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
