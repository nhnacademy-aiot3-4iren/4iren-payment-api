package com.siren.sirenpaymentapi.controller.kakao;

import com.siren.sirenpaymentapi.domain.Plan;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.domain.entity.PlanPrices;
import com.siren.sirenpaymentapi.dto.billing_keys.ConfirmRegistrationCommand;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationRequest;
import com.siren.sirenpaymentapi.dto.billing_keys.StartRegistrationResponse;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.kakao.PendingRegistration;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGateway;
import com.siren.sirenpaymentapi.gateway.RecurringPaymentGatewayRegistry;
import com.siren.sirenpaymentapi.service.BillingKeyRegistrationService;
import com.siren.sirenpaymentapi.service.basic_service.PlanPricesService;
import com.siren.sirenpaymentapi.service.cache.KakaoPendingRegistrationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoBillingKeyRegistrationControllerTest {

    @Mock
    private RecurringPaymentGatewayRegistry gatewayRegistry;

    @Mock
    private KakaoPendingRegistrationCache pendingRegistrationCache;

    @Mock
    private BillingKeyRegistrationService billingKeyRegistrationService;

    @Mock
    private PlanPricesService planPricesService;

    @Mock
    private RecurringPaymentGateway gateway;

    private KakaoBillingKeyRegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new KakaoBillingKeyRegistrationController(
                gatewayRegistry, pendingRegistrationCache, billingKeyRegistrationService, planPricesService);
        ReflectionTestUtils.setField(controller, "callbackBaseUrl", "http://callback-url");
        ReflectionTestUtils.setField(controller, "successUrl", "http://success-url");
        ReflectionTestUtils.setField(controller, "failureUrl", "http://failure-url");
    }

    @Test
    void startRegistrationReturnsRedirectUrl() {
        when(planPricesService.getCurrentPlanPrice(Plan.MONTHLY))
                .thenReturn(PlanPrices.builder().id(1L).amount(29000L).build());
        when(gatewayRegistry.getGateway(Provider.KAKAO_PAY)).thenReturn(gateway);
        when(gateway.startRegistration(1L, "http://callback-url"))
                .thenReturn(new RegistrationStart("http://redirect-url", "order-1", "tid-1"));

        StartRegistrationResponse response = controller.startRegistration(
                new StartRegistrationRequest(Plan.MONTHLY), 1L, "token-1");

        assertEquals("http://redirect-url", response.redirectUrl());
        verify(pendingRegistrationCache).save(eq("order-1"), any(PendingRegistration.class));
    }

    @Test
    void handleCallbackRedirectsToSuccessUrl() {
        PendingRegistration pending = new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "tid-1", "token-1");
        when(pendingRegistrationCache.consume("order-1")).thenReturn(Optional.of(pending));
        when(gatewayRegistry.getGateway(Provider.KAKAO_PAY)).thenReturn(gateway);
        when(gateway.confirmRegistration(any())).thenReturn(new ConfirmedBillingKey("credential", "MONEY"));

        ResponseEntity<Void> response = controller.handleCallback("pg-token-1", "order-1");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("http://success-url", response.getHeaders().getLocation().toString());
        verify(billingKeyRegistrationService).confirmRegistration(new ConfirmRegistrationCommand(
                1L, Provider.KAKAO_PAY, "credential", "MONEY", Plan.MONTHLY, 29000L, 1L, "token-1"));
    }

    @Test
    void handleCallbackRedirectsToFailureUrlWhenPendingMissing() {
        when(pendingRegistrationCache.consume("order-1")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.handleCallback("pg-token-1", "order-1");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("http://failure-url", response.getHeaders().getLocation().toString());
        verifyNoInteractions(billingKeyRegistrationService);
    }

    @Test
    void handleCallbackRedirectsToFailureUrlWhenGatewayThrows() {
        PendingRegistration pending = new PendingRegistration(1L, Plan.MONTHLY, 29000L, 1L, "tid-1", "token-1");
        when(pendingRegistrationCache.consume("order-1")).thenReturn(Optional.of(pending));
        when(gatewayRegistry.getGateway(Provider.KAKAO_PAY)).thenReturn(gateway);
        when(gateway.confirmRegistration(any())).thenThrow(new RuntimeException("승인 실패"));

        ResponseEntity<Void> response = controller.handleCallback("pg-token-1", "order-1");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("http://failure-url", response.getHeaders().getLocation().toString());
    }
}
