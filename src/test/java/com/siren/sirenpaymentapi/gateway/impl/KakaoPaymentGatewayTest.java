package com.siren.sirenpaymentapi.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.sirenpaymentapi.adaptor.KakaoAdaptor;
import com.siren.sirenpaymentapi.domain.Provider;
import com.siren.sirenpaymentapi.dto.gateway.ChargeResult;
import com.siren.sirenpaymentapi.dto.gateway.ConfirmedBillingKey;
import com.siren.sirenpaymentapi.dto.gateway.RegistrationStart;
import com.siren.sirenpaymentapi.dto.kakao.response.ApproveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.InactiveResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.ReadyResponse;
import com.siren.sirenpaymentapi.dto.kakao.response.SubscriptionResponse;
import com.siren.sirenpaymentapi.exception.BillingKeyRegistrationException;
import com.siren.sirenpaymentapi.exception.BillingKeyRemoveException;
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
class KakaoPaymentGatewayTest {

    @Mock
    private KakaoAdaptor kakaoAdaptor;

    private KakaoPaymentGateway kakaoPaymentGateway;

    @BeforeEach
    void setUp() {
        kakaoPaymentGateway = new KakaoPaymentGateway(kakaoAdaptor, new ObjectMapper());
    }

    @Test
    void getProviderReturnsKakaoPay() {
        assertEquals(Provider.KAKAO_PAY, kakaoPaymentGateway.getProvider());
    }

    @Test
    void startRegistrationReturnsRedirectUrl() {
        when(kakaoAdaptor.ready(eq(1L), anyString(), anyString()))
                .thenReturn(new ReadyResponse("tid-1", "http://redirect-url", "2026-01-01"));

        RegistrationStart result = kakaoPaymentGateway.startRegistration(1L, "http://return-url");

        assertEquals("http://redirect-url", result.redirectUrl());
        assertEquals("tid-1", result.providerReference());
    }

    @Test
    void startRegistrationThrowsWhenResponseNull() {
        when(kakaoAdaptor.ready(eq(1L), anyString(), anyString())).thenReturn(null);

        assertThrows(BillingKeyRegistrationException.class,
                () -> kakaoPaymentGateway.startRegistration(1L, "http://return-url"));
    }

    @Test
    void confirmRegistrationReturnsCredential() {
        Map<String, String> callbackParams = Map.of(
                "tid", "tid-1", "orderId", "order-1", "userId", "1", "pg_token", "pg-token-1");
        when(kakaoAdaptor.approve("tid-1", "order-1", 1L, "pg-token-1"))
                .thenReturn(new ApproveResponse("sid-1", "MONEY"));

        ConfirmedBillingKey result = kakaoPaymentGateway.confirmRegistration(callbackParams);

        assertEquals("MONEY", result.maskedInfo());
        assertTrue(result.providerCredential().contains("sid-1"));
    }

    @Test
    void confirmRegistrationThrowsWhenSidMissing() {
        Map<String, String> callbackParams = Map.of(
                "tid", "tid-1", "orderId", "order-1", "userId", "1", "pg_token", "pg-token-1");
        when(kakaoAdaptor.approve("tid-1", "order-1", 1L, "pg-token-1"))
                .thenReturn(new ApproveResponse(null, null));

        assertThrows(BillingKeyRegistrationException.class,
                () -> kakaoPaymentGateway.confirmRegistration(callbackParams));
    }

    @Test
    void chargeReturnsSuccess() {
        String credential = "{\"sid\":\"sid-1\",\"userId\":1}";
        SubscriptionResponse response = new SubscriptionResponse(
                "aid-1", "tid-1", "cid-1", "sid-1", "order-1", "1", "MONEY", null, "2026-01-01", "2026-01-01");
        when(kakaoAdaptor.charge("sid-1", 29000L, "order-1", 1L)).thenReturn(response);

        ChargeResult result = kakaoPaymentGateway.charge(credential, 29000L, "order-1");

        assertTrue(result.success());
        assertEquals("tid-1", result.providerTransactionId());
    }

    @Test
    void chargeReturnsBillingKeyRevokedWhenSidDeactivated() {
        String credential = "{\"sid\":\"sid-1\",\"userId\":1}";
        String errorBody = "{\"error_code\":-751,\"error_message\":\"비활성화된 SID\"}";
        RestClientResponseException exception = new RestClientResponseException(
                "실패", 400, "Bad Request", null, errorBody.getBytes(StandardCharsets.UTF_8), null);
        when(kakaoAdaptor.charge("sid-1", 29000L, "order-1", 1L)).thenThrow(exception);

        ChargeResult result = kakaoPaymentGateway.charge(credential, 29000L, "order-1");

        assertTrue(result.billingKeyRevoked());
    }

    @Test
    void chargeReturnsFailureWhenOtherErrorCode() {
        String credential = "{\"sid\":\"sid-1\",\"userId\":1}";
        String errorBody = "{\"error_code\":-752,\"error_message\":\"월 사용 횟수 초과\"}";
        RestClientResponseException exception = new RestClientResponseException(
                "실패", 400, "Bad Request", null, errorBody.getBytes(StandardCharsets.UTF_8), null);
        when(kakaoAdaptor.charge("sid-1", 29000L, "order-1", 1L)).thenThrow(exception);

        ChargeResult result = kakaoPaymentGateway.charge(credential, 29000L, "order-1");

        assertFalse(result.success());
        assertFalse(result.billingKeyRevoked());
    }

    @Test
    void revokeSucceeds() {
        String credential = "{\"sid\":\"sid-1\",\"userId\":1}";
        when(kakaoAdaptor.inactive("sid-1"))
                .thenReturn(new InactiveResponse("cid-1", "sid-1", "INACTIVE", "2026-01-01", "2026-01-01", "2026-01-01"));

        assertDoesNotThrow(() -> kakaoPaymentGateway.revoke(credential));
    }

    @Test
    void revokeThrowsWhenStatusNotInactive() {
        String credential = "{\"sid\":\"sid-1\",\"userId\":1}";
        when(kakaoAdaptor.inactive("sid-1"))
                .thenReturn(new InactiveResponse("cid-1", "sid-1", "ACTIVE", "2026-01-01", null, "2026-01-01"));

        assertThrows(BillingKeyRemoveException.class, () -> kakaoPaymentGateway.revoke(credential));
    }
}
