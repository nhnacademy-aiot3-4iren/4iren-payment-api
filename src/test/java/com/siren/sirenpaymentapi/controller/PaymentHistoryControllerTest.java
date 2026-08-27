package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.domain.PaymentStatus;
import com.siren.sirenpaymentapi.dto.payments.PaymentHistoryResponse;
import com.siren.sirenpaymentapi.service.basic_service.PaymentsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryControllerTest {

    @Mock
    private PaymentsService paymentsService;

    @InjectMocks
    private PaymentHistoryController paymentHistoryController;

    @Test
    void getMyPaymentsReturnsList() {
        PaymentHistoryResponse payment = new PaymentHistoryResponse(
                1L, 29000L, PaymentStatus.DONE, null, LocalDateTime.now(), LocalDateTime.now());
        when(paymentsService.findByUserId(1L)).thenReturn(List.of(payment));

        List<PaymentHistoryResponse> result = paymentHistoryController.getMyPayments(1L);

        assertEquals(1, result.size());
        assertEquals(29000L, result.get(0).amount());
    }

    @Test
    void getMyPaymentsReturnsEmptyList() {
        when(paymentsService.findByUserId(1L)).thenReturn(List.of());

        List<PaymentHistoryResponse> result = paymentHistoryController.getMyPayments(1L);

        assertTrue(result.isEmpty());
    }
}
