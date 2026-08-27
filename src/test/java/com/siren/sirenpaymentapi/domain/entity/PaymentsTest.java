package com.siren.sirenpaymentapi.domain.entity;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentsTest {

    private Payments newReadyPayment() {
        return Payments.builder()
                .orderId("order-1")
                .amount(29000L)
                .status(PaymentStatus.READY)
                .attemptedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void markSucceededSetsDone() {
        Payments payment = newReadyPayment();
        payment.markSucceeded("tx-1", "pay-token-1", "{}");
        assertEquals(PaymentStatus.DONE, payment.getStatus());
        assertEquals("tx-1", payment.getProviderTransactionId());
        assertEquals("pay-token-1", payment.getPayToken());
        assertNotNull(payment.getApprovedAt());
    }

    @Test
    void markFailedSetsFailed() {
        Payments payment = newReadyPayment();
        payment.markFailed("카드 한도 초과", "{}");
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("카드 한도 초과", payment.getFailureReason());
    }
}
