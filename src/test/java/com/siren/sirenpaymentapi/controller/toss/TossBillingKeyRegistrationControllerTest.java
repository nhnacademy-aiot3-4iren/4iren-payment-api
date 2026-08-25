package com.siren.sirenpaymentapi.controller.toss;

import com.siren.sirenpaymentapi.domain.BillingKeyStatus;
import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.BillingKeys;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.toss.PendingRegistration;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.BillingKeysService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.cache.TossPendingRegistrationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TossBillingKeyRegistrationControllerTest {

    @Mock
    private RecurringPaymentGatewayRegistry gatewayRegistry;

    @Mock
    private TossPendingRegistrationCache pendingRegistrationCache;

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @Mock
    private PlanPricesService planPricesService;

    @Mock
    private BillingKeysService billingKeysService;

    @Mock
    private RecurringPaymentGateway gateway;

    private TossBillingKeyRegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new TossBillingKeyRegistrationController(
                gatewayRegistry, pendingRegistrationCache, billingKeyRegistrationService, planPricesService, billingKeysService);
        ReflectionTestUtils.setField(controller, "callbackUrl", "http://callback-url");
    }

    @Test
    void startRegistrationReturnsRedirectUrl() {
        when(planPricesService.getCurrentPlanPrice(Plan.MONTHLY))
                .thenReturn(PlanPrices.builder().id(1L).amount(29000L).build());
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.startRegistration(1L, "http://callback-url"))
                .thenReturn(new RegistrationStart("http://checkout-uri", "billing-key-1", null));

        StartRegistrationResponse response = controller.startRegistration(
                new StartRegistrationRequest(Plan.MONTHLY), 1L, "token-1");

        assertEquals("http://checkout-uri", response.redirectUrl());
        verify(pendingRegistrationCache).save(eq("billing-key-1"), any(PendingRegistration.class));
    }

    @Test
    void handleCallbackConfirmsRegistration() {
        Map<String, String> callbackParams = Map.of("action", "ACTIVATED", "billingKey", "billing-key-1");
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.confirmRegistration(callbackParams))
                .thenReturn(new ConfirmedBillingKey("credential", "CARD"));
        when(pendingRegistrationCache.consume("billing-key-1"))
                .thenReturn(Optional.of(new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "token-1")));

        ResponseEntity<Void> response = controller.handleCallback(callbackParams);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingKeyRegistrationService).confirmRegistration(new ConfirmRegistrationCommand(
                1L, Provider.TOSS_PAY, "credential", "CARD", Plan.MONTHLY, 29000L, 1L, "token-1"));
    }

    @Test
    void handleCallbackHandlesRemovedAction() {
        Map<String, String> callbackParams = Map.of("action", "REMOVED", "userId", "1");

        ResponseEntity<Void> response = controller.handleCallback(callbackParams);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingKeyRegistrationService).revokeByProviderNotice(1L);
        verifyNoInteractions(gatewayRegistry);
    }

    @Test
    void handleCallbackReturnsOkWhenPendingMissingButAlreadyActive() {
        Map<String, String> callbackParams = Map.of("action", "ACTIVATED", "billingKey", "billing-key-1", "userId", "1");
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.confirmRegistration(callbackParams))
                .thenReturn(new ConfirmedBillingKey("credential", "CARD"));
        when(pendingRegistrationCache.consume("billing-key-1")).thenReturn(Optional.empty());
        when(billingKeysService.findActiveByUserId(1L))
                .thenReturn(Optional.of(BillingKeys.builder().id(1L).status(BillingKeyStatus.ACTIVE).build()));

        ResponseEntity<Void> response = controller.handleCallback(callbackParams);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(billingKeyRegistrationService);
    }

    @Test
    void handleCallbackReturnsNotFoundWhenPendingMissingAndNoActiveKey() {
        Map<String, String> callbackParams = Map.of("action", "ACTIVATED", "billingKey", "billing-key-1", "userId", "1");
        when(gatewayRegistry.getGateway(Provider.TOSS_PAY)).thenReturn(gateway);
        when(gateway.confirmRegistration(callbackParams))
                .thenReturn(new ConfirmedBillingKey("credential", "CARD"));
        when(pendingRegistrationCache.consume("billing-key-1")).thenReturn(Optional.empty());
        when(billingKeysService.findActiveByUserId(1L)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.handleCallback(callbackParams);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
