package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.SubscriptionStatus;
import com.siren.sirenpaymentapi.dto.subscriptions.SubscriptionResponse;
import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.exception.NotFoundSubscriptionException;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @Mock
    private SubscriptionsService subscriptionsService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Test
    void getCurrentSubscriptionReturnsSubscription() {
        LocalDateTime periodEnd = LocalDateTime.of(2026, 9, 28, 0, 0);
        LocalDate nextBilling = LocalDate.of(2026, 9, 28);
        SubscriptionResponse expected = new SubscriptionResponse(
                Plan.MONTHLY, 29000L, SubscriptionStatus.ACTIVE, periodEnd, nextBilling);
        when(subscriptionsService.findLatestByUserId(1L)).thenReturn(expected);

        ResponseEntity<SubscriptionResponse> response = subscriptionController.getCurrentSubscription(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getCurrentSubscriptionThrowsWhenNoneFound() {
        when(subscriptionsService.findLatestByUserId(1L))
                .thenThrow(new NotFoundSubscriptionException("user: 1의 구독을 찾을 수 없습니다."));

        assertThrows(NotFoundSubscriptionException.class, () -> subscriptionController.getCurrentSubscription(1L));
    }

    @Test
    void cancelSubscriptionReturnsNoContent() {
        ResponseEntity<Void> response = subscriptionController.cancelSubscription(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(billingKeyRegistrationService).cancelSubscription(1L);
    }

    @Test
    void cancelSubscriptionPropagatesException() {
        doThrow(new NotFoundBillingKeysException("없음")).when(billingKeyRegistrationService).cancelSubscription(1L);

        assertThrows(NotFoundBillingKeysException.class, () -> subscriptionController.cancelSubscription(1L));
    }
}
