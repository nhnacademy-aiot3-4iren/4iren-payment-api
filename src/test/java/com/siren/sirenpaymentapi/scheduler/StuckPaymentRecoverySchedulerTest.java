package com.siren.sirenpaymentapi.scheduler;

import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.SubscriptionChargeService;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StuckPaymentRecoverySchedulerTest {

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SubscriptionChargeService subscriptionChargeService;

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @InjectMocks
    private StuckPaymentRecoveryScheduler stuckPaymentRecoveryScheduler;

    @Test
    void recoverStuckPaymentsRecordsFailureWhenNoNewerAttempt() {
        StuckPayment stuckPayment = new StuckPayment(1L, 2L, 3L, LocalDateTime.now());
        when(paymentsService.findStuckInReady(any(LocalDateTime.class))).thenReturn(List.of(stuckPayment));
        when(paymentsService.hasNewerAttempt(eq(2L), any(LocalDateTime.class))).thenReturn(false);
        when(subscriptionChargeService.recordFailure(eq(1L), eq(2L), anyString(), any())).thenReturn(false);

        stuckPaymentRecoveryScheduler.recoverStuckPayments();

        verify(subscriptionChargeService).recordFailure(eq(1L), eq(2L), anyString(), any());
        verifyNoInteractions(billingKeyRegistrationService);
    }

    @Test
    void recoverStuckPaymentsRevokesBillingKeyWhenSubscriptionExpires() {
        StuckPayment stuckPayment = new StuckPayment(1L, 2L, 3L, LocalDateTime.now());
        when(paymentsService.findStuckInReady(any(LocalDateTime.class))).thenReturn(List.of(stuckPayment));
        when(paymentsService.hasNewerAttempt(eq(2L), any(LocalDateTime.class))).thenReturn(false);
        when(subscriptionChargeService.recordFailure(eq(1L), eq(2L), anyString(), any())).thenReturn(true);

        stuckPaymentRecoveryScheduler.recoverStuckPayments();

        verify(billingKeyRegistrationService).revokeBillingKeyAfterExpiry(3L);
    }

    @Test
    void recoverStuckPaymentsOnlyMarksFailedWhenNewerAttemptExists() {
        StuckPayment stuckPayment = new StuckPayment(1L, 2L, 3L, LocalDateTime.now());
        when(paymentsService.findStuckInReady(any(LocalDateTime.class))).thenReturn(List.of(stuckPayment));
        when(paymentsService.hasNewerAttempt(eq(2L), any(LocalDateTime.class))).thenReturn(true);

        stuckPaymentRecoveryScheduler.recoverStuckPayments();

        verify(paymentsService).markFailed(eq(1L), anyString(), any());
        verify(subscriptionChargeService, never()).recordFailure(any(), any(), any(), any());
    }

    @Test
    void recoverStuckPaymentsDoesNothingWhenEmpty() {
        when(paymentsService.findStuckInReady(any(LocalDateTime.class))).thenReturn(List.of());

        stuckPaymentRecoveryScheduler.recoverStuckPayments();

        verifyNoInteractions(subscriptionChargeService);
    }
}
