package com.siren.sirenpaymentapi.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.adaptor.TossAdaptor;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.toss.response.BillingKeyStatusResponse;
import com.siren.sirenpaymentapi.dto.toss.response.ChargeResponse;
import com.siren.sirenpaymentapi.dto.toss.response.CreateBillingKeyResponse;
import com.siren.sirenpaymentapi.dto.toss.response.RemoveBillingKeyResponse;
import com.siren.sirenpaymentapi.exception.BillingKeyCallbackException;
import com.siren.sirenpaymentapi.exception.BillingKeyRegistrationException;
import com.siren.sirenpaymentapi.exception.BillingKeyRemoveException;
import com.siren.sirenpaymentapi.exception.InactiveBillingKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TossPaymentGatewayTest {

    @Mock
    private TossAdaptor tossAdaptor;

    private TossPaymentGateway tossPaymentGateway;

    @BeforeEach
    void setUp() {
        tossPaymentGateway = new TossPaymentGateway(tossAdaptor, new ObjectMapper());
    }

    @Test
    void getProviderReturnsTossPay() {
        assertEquals(Provider.TOSS_PAY, tossPaymentGateway.getProvider());
    }

    @Test
    void startRegistrationReturnsRedirectUrl() {
        when(tossAdaptor.createBillingKey(1L, "http://return-url"))
                .thenReturn(new CreateBillingKeyResponse(0, "billing-key-1", "http://checkout-uri"));

        RegistrationStart result = tossPaymentGateway.startRegistration(1L, "http://return-url");

        assertEquals("http://checkout-uri", result.redirectUrl());
        assertEquals("billing-key-1", result.correlationKey());
    }

    @Test
    void startRegistrationThrowsWhenCodeNotZero() {
        when(tossAdaptor.createBillingKey(1L, "http://return-url"))
                .thenReturn(new CreateBillingKeyResponse(-1, null, null));

        assertThrows(BillingKeyRegistrationException.class,
                () -> tossPaymentGateway.startRegistration(1L, "http://return-url"));
    }

    @Test
    void confirmRegistrationReturnsCredentialWhenActive() {
        Map<String, String> callbackParams = Map.of("action", "ACTIVATED", "userId", "1");
        when(tossAdaptor.getBillingKeyStatus("1"))
                .thenReturn(new BillingKeyStatusResponse(0, "1", "billing-key-1", "ACTIVE", "CARD"));

        ConfirmedBillingKey result = tossPaymentGateway.confirmRegistration(callbackParams);

        assertEquals("CARD", result.maskedInfo());
        assertTrue(result.providerCredential().contains("billing-key-1"));
    }

    @Test
    void confirmRegistrationThrowsWhenActionNotActivated() {
        Map<String, String> callbackParams = Map.of("action", "REMOVED", "userId", "1");

        assertThrows(BillingKeyCallbackException.class, () -> tossPaymentGateway.confirmRegistration(callbackParams));
    }

    @Test
    void confirmRegistrationThrowsWhenStatusNotActive() {
        Map<String, String> callbackParams = Map.of("action", "ACTIVATED", "userId", "1");
        when(tossAdaptor.getBillingKeyStatus("1"))
                .thenReturn(new BillingKeyStatusResponse(0, "1", "billing-key-1", "CREATE", "CARD"));

        assertThrows(InactiveBillingKeyException.class, () -> tossPaymentGateway.confirmRegistration(callbackParams));
    }

    @Test
    void chargeReturnsSuccess() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        when(tossAdaptor.executeBilling("billing-key-1", 29000L, "order-1"))
                .thenReturn(new ChargeResponse(0, null, null, "tx-1", "pay-token-1", "2026-01-01", "CARD"));

        ChargeResult result = tossPaymentGateway.charge(credential, 29000L, "order-1");

        assertTrue(result.success());
        assertEquals("tx-1", result.providerTransactionId());
    }

    @Test
    void chargeReturnsFailure() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        when(tossAdaptor.executeBilling("billing-key-1", 29000L, "order-1"))
                .thenReturn(new ChargeResponse(-1, "EXCEED_MAX_DAILY_PAYMENT_COUNT", "한도 초과", null, null, null, null));

        ChargeResult result = tossPaymentGateway.charge(credential, 29000L, "order-1");

        assertFalse(result.success());
        assertFalse(result.billingKeyRevoked());
    }

    @Test
    void chargeReturnsBillingKeyRevokedWhenStatusRemoved() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        when(tossAdaptor.executeBilling("billing-key-1", 29000L, "order-1"))
                .thenReturn(new ChargeResponse(-1, "COMMON_BILLING_KEY_NOT_FOUND", "빌링키 없음", null, null, null, null));
        when(tossAdaptor.getBillingKeyStatus("1"))
                .thenReturn(new BillingKeyStatusResponse(0, "1", "billing-key-1", "REMOVE", "CARD"));

        ChargeResult result = tossPaymentGateway.charge(credential, 29000L, "order-1");

        assertTrue(result.billingKeyRevoked());
    }

    @Test
    void chargeReturnsFailureOnRestClientException() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        RestClientResponseException exception = new RestClientResponseException(
                "서버 오류", 500, "Internal Server Error", null, "에러 바디".getBytes(StandardCharsets.UTF_8), null);
        when(tossAdaptor.executeBilling("billing-key-1", 29000L, "order-1")).thenThrow(exception);

        ChargeResult result = tossPaymentGateway.charge(credential, 29000L, "order-1");

        assertFalse(result.success());
    }

    @Test
    void revokeSucceeds() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        when(tossAdaptor.removeBillingKey("billing-key-1")).thenReturn(new RemoveBillingKeyResponse(0, null));

        assertDoesNotThrow(() -> tossPaymentGateway.revoke(credential));
    }

    @Test
    void revokeThrowsWhenCodeNotZero() {
        String credential = "{\"billingKey\":\"billing-key-1\",\"userId\":1}";
        when(tossAdaptor.removeBillingKey("billing-key-1")).thenReturn(new RemoveBillingKeyResponse(-1, "실패"));

        assertThrows(BillingKeyRemoveException.class, () -> tossPaymentGateway.revoke(credential));
    }
}
