package com.siren.sirenpaymentapi.service.basic_service;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import com.siren.sirenpaymentapi.domain.entity.Payments;
import com.siren.sirenpaymentapi.domain.entity.Subscriptions;
import com.siren.sirenpaymentapi.dto.payments.PreparedCharge;
import com.siren.sirenpaymentapi.dto.payments.StuckPayment;
import com.siren.sirenpaymentapi.exception.NotFoundPaymentsException;
import com.siren.sirenpaymentapi.repository.PaymentsRepository;
import com.siren.sirenpaymentapi.repository.SubscriptionsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentsServiceTest {

    @Mock
    private PaymentsRepository paymentsRepository;

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

    @InjectMocks
    private PaymentsService paymentsService;

    @Test
    void prepareChargeSavesReadyPayment() {
        Subscriptions subscription = Subscriptions.builder().id(1L).build();
        when(subscriptionsRepository.getReferenceById(1L)).thenReturn(subscription);
        when(paymentsRepository.save(any(Payments.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreparedCharge result = paymentsService.prepareCharge(1L, 29000L);

        assertNotNull(result.orderId());
        verify(paymentsRepository).save(argThat(payment -> payment.getStatus() == PaymentStatus.READY));
    }

    @Test
    void markSucceededSetsDone() {
        Payments payment = Payments.builder().id(1L).status(PaymentStatus.READY).build();
        when(paymentsRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentsService.markSucceeded(1L, "tx-1", "pay-token-1", "{}");

        assertEquals(PaymentStatus.DONE, payment.getStatus());
    }

    @Test
    void markSucceededThrowsWhenNotFound() {
        when(paymentsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundPaymentsException.class,
                () -> paymentsService.markSucceeded(1L, "tx-1", "pay-token-1", "{}"));
    }

    @Test
    void markFailedSetsFailed() {
        Payments payment = Payments.builder().id(1L).status(PaymentStatus.READY).build();
        when(paymentsRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentsService.markFailed(1L, "실패 사유", "{}");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void findStuckInReadyReturnsList() {
        LocalDateTime cutoff = LocalDateTime.now();
        StuckPayment stuckPayment = new StuckPayment(1L, 2L, 3L, LocalDateTime.now());
        when(paymentsRepository.findStuckInReady(cutoff)).thenReturn(List.of(stuckPayment));

        List<StuckPayment> result = paymentsService.findStuckInReady(cutoff);

        assertEquals(1, result.size());
    }

    @Test
    void hasNewerAttemptReturnsTrue() {
        LocalDateTime after = LocalDateTime.now();
        when(paymentsRepository.hasNewerAttempt(1L, after)).thenReturn(true);

        assertTrue(paymentsService.hasNewerAttempt(1L, after));
    }

    @Test
    void hasNewerAttemptReturnsFalse() {
        LocalDateTime after = LocalDateTime.now();
        when(paymentsRepository.hasNewerAttempt(1L, after)).thenReturn(false);

        assertFalse(paymentsService.hasNewerAttempt(1L, after));
    }
}
