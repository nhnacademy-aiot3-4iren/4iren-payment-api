package com.siren.sirenpaymentapi.controller;

import com.siren.sirenpaymentapi.exception.NotFoundBillingKeysException;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @InjectMocks
    private SubscriptionController subscriptionController;

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
